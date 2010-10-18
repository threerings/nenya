//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
// http://code.google.com/p/nenya/
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.jme.model;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import java.util.ArrayList;
import java.util.Properties;

import com.google.common.collect.Lists;

import com.jme.bounding.BoundingBox;
import com.jme.bounding.BoundingSphere;
import com.jme.bounding.BoundingVolume;
import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh;
import com.jme.scene.VBOInfo;
import com.jme.scene.batch.TriangleBatch;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.CullState;
import com.jme.scene.state.FogState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.ZBufferState;
import com.jme.system.DisplaySystem;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.OutputCapsule;
import com.jme.util.geom.BufferUtils;

import com.samskivert.util.PropertiesUtil;
import com.samskivert.util.StringUtil;

import com.threerings.jme.util.ShaderCache;

/**
 * A {@link TriMesh} with a serialization mechanism tailored to stored models.
 */
public class ModelMesh extends TriMesh
    implements ModelSpatial
{
    /**
     * No-arg constructor for deserialization.
     */
    public ModelMesh ()
    {
        super("mesh");
    }

    /**
     * Creates a mesh with no vertex data.
     */
    public ModelMesh (String name)
    {
        super(name);
    }

    @Override
    public int hashCode ()
    {
        // hash on the name rather than the identity for consistent ordering
        return getName().hashCode();
    }

    /**
     * Reconfigures this model with a new set of (sub-)properties.  Textures
     * must be (re-)resolved after calling this method.
     */
    public void reconfigure (Properties props)
    {
        configure(_solid, _textureKey, _transparent, props);
        setRenderStates();
    }

    /**
     * Configures this mesh based on the given parameters and (sub-)properties.
     *
     * @param texture the texture specified in the model export, if any (can be
     * overridden by textures specified in the properties)
     * @param solid whether or not the mesh allows back face culling
     * @param transparent whether or not the mesh is (partially) transparent
     */
    public void configure (
        boolean solid, String texture, boolean transparent, Properties props)
    {
        _textureKey = texture;
        _textures = (texture == null) ? null : StringUtil.parseStringArray(
            props.getProperty(texture, texture));
        Properties tprops = PropertiesUtil.getFilteredProperties(
            props, texture);
        _sphereMapped = Boolean.parseBoolean(tprops.getProperty("sphere_map"));
        _filterMode = "nearest".equals(tprops.getProperty("filter")) ?
                Texture.FM_NEAREST : Texture.FM_LINEAR;
        _mipMapMode = getMipMapMode(tprops.getProperty("mipmap"));
        _compress = Boolean.parseBoolean(tprops.getProperty("compress", "true"));
        String emissive = tprops.getProperty("emissive");
        if (Boolean.parseBoolean(emissive)) {
            _emissive = true;
        } else {
            _emissiveMap = emissive;
        }
        _additive = Boolean.parseBoolean(tprops.getProperty("additive"));
        _solid = solid;
        _transparent = transparent;
        String threshold = tprops.getProperty("alpha_threshold");
        _alphaThreshold = (threshold == null) ?
            DEFAULT_ALPHA_THRESHOLD : Float.parseFloat(threshold);
        _translucent = _transparent &&
            Boolean.parseBoolean(tprops.getProperty("translucent"));
    }

    /**
     * Adjusts the vertices and the transform of the mesh so that the mesh's
     * position lies at the center of its bounding volume.
     */
    public void centerVertices ()
    {
        Vector3f offset = getBatch(0).getModelBound().getCenter().negate();
        if (!offset.equals(Vector3f.ZERO)) {
            getLocalTranslation().subtractLocal(offset);
            getBatch(0).getModelBound().getCenter().set(Vector3f.ZERO);
            getBatch(0).translatePoints(offset);
        }
        storeOriginalBuffers();
    }

    /**
     * Adds an overlay layer to this mesh.  After the mesh is rendered with
     * its configured states, these states will be applied and the mesh will
     * be rendered again.
     */
    public void addOverlay (RenderState[] overlay)
    {
        if (_overlays == null) {
            _overlays = Lists.newArrayListWithCapacity(1);
        }
        _overlays.add(overlay);
    }

    /**
     * Removes a layer from this mesh.
     */
    public void removeOverlay (RenderState[] overlay)
    {
        if (_overlays != null) {
            _overlays.remove(overlay);
            if (_overlays.isEmpty()) {
                _overlays = null;
            }
        }
    }

    @Override
    public void reconstruct (
        FloatBuffer vertices, FloatBuffer normals, FloatBuffer colors,
        FloatBuffer textures, IntBuffer indices)
    {
        super.reconstruct(vertices, normals, colors, textures, indices);
        for (int ii = 1, nn = getTextureCount(); ii < nn; ii++) {
            setTextureBuffer(0, getTextureBuffer(0, 0), ii);
        }

        // store any buffers that will be manipulated on a per-instance basis
        storeOriginalBuffers();

        // initialize the model if we're displaying
        setRenderStates();
    }

    // documentation inherited from interface ModelSpatial
    public Spatial putClone (Spatial store, Model.CloneCreator properties)
    {
        ModelMesh mstore = (ModelMesh)properties.originalToCopy.get(this);
        if (mstore != null) {
            return mstore;
        } else if (store == null) {
            mstore = new ModelMesh(getName());
        } else {
            mstore = (ModelMesh)store;
        }
        properties.originalToCopy.put(this, mstore);
        mstore.normalsMode = normalsMode;
        mstore.cullMode = cullMode;
        for (int ii = 0; ii < RenderState.RS_MAX_STATE; ii++) {
            RenderState rstate = getRenderState(ii);
            if (rstate != null) {
                mstore.setRenderState(rstate);
            }
        }
        mstore.renderQueueMode = renderQueueMode;
        mstore.lockedMode = lockedMode;
        mstore.lightCombineMode = lightCombineMode;
        mstore.textureCombineMode = textureCombineMode;
        mstore.name = name;
        mstore.isCollidable = isCollidable;
        mstore.localRotation.set(localRotation);
        mstore.localTranslation.set(localTranslation);
        mstore.localScale.set(localScale);
        for (Object controller : getControllers()) {
            if (controller instanceof ModelController) {
                mstore.addController(
                    ((ModelController)controller).putClone(null, properties));
            }
        }
        TriangleBatch batch = getBatch(0), mbatch = mstore.getBatch(0);
        mbatch.setVertexBuffer(properties.isSet("vertices") ?
            batch.getVertexBuffer() :
                BufferUtils.clone(batch.getVertexBuffer()));
        mbatch.setColorBuffer(properties.isSet("colors") ?
            batch.getColorBuffer() :
                BufferUtils.clone(batch.getColorBuffer()));
        mbatch.setNormalBuffer(properties.isSet("normals") ?
            batch.getNormalBuffer() :
                BufferUtils.clone(batch.getNormalBuffer()));
        FloatBuffer texcoords;
        for (int ii = 0; (texcoords = batch.getTextureBuffer(ii)) != null;
            ii++) {
            mbatch.setTextureBuffer(properties.isSet("texcoords") ?
                texcoords : BufferUtils.clone(texcoords), ii);
        }
        mbatch.setIndexBuffer((properties.isSet("indices") && !_translucent) ?
            batch.getIndexBuffer() :
                BufferUtils.clone(batch.getIndexBuffer()));
        if (properties.isSet("vboinfo")) {
            mbatch.setVBOInfo(batch.getVBOInfo());
        }
        if (properties.isSet("obbtree")) {
            mbatch.setCollisionTree(batch.getCollisionTree());
        }
        if (properties.isSet("displaylistid")) {
            mbatch.setDisplayListID(batch.getDisplayListID());
        }
        if (batch.getModelBound() != null) {
            mbatch.setModelBound(properties.isSet("bound") ?
                batch.getModelBound() : batch.getModelBound().clone(null));
        }
        mstore._textureKey = _textureKey;
        if (_textures != null && _textures.length > 1) {
            int tidx = properties.random % _textures.length;
            mstore._textures = new String[] { _textures[tidx] };
            mstore._tstates = new TextureState[] { _tstates[tidx] };
            mstore.setRenderState(_tstates[tidx]);
        } else {
            mstore._textures = _textures;
            mstore._tstates = _tstates;
        }
        mstore._sphereMapped = _sphereMapped;
        mstore._filterMode = _filterMode;
        mstore._mipMapMode = _mipMapMode;
        mstore._compress = _compress;
        mstore._emissiveMap = _emissiveMap;
        mstore._emissive = _emissive;
        mstore._additive = _additive;
        mstore._solid = _solid;
        mstore._transparent = _transparent;
        mstore._alphaThreshold = _alphaThreshold;
        mstore._translucent = _translucent;
        mstore._oibuf = _oibuf;
        mstore._vbuf = _vbuf;
        return mstore;
    }

    @Override
    public void updateWorldVectors ()
    {
        if (!_transformLocked) {
            super.updateWorldVectors();
        }
    }

    @Override
    public void read (JMEImporter im)
        throws IOException
    {
        InputCapsule capsule = im.getCapsule(this);
        setName(capsule.readString("name", null));
        setLocalTranslation((Vector3f)capsule.readSavable(
            "localTranslation", null));
        setLocalRotation((Quaternion)capsule.readSavable(
            "localRotation", null));
        setLocalScale((Vector3f)capsule.readSavable(
            "localScale", null));
        TriangleBatch batch = getBatch(0);
        batch.setModelBound((BoundingVolume)capsule.readSavable(
            "modelBound", null));
        _textureKey = capsule.readString("textureKey", null);
        _textures = capsule.readStringArray("textures", null);
        _sphereMapped = capsule.readBoolean("sphereMapped", false);
        _filterMode = capsule.readInt("filterMode", Texture.FM_LINEAR);
        _mipMapMode = capsule.readInt("mipMapMode", Texture.MM_LINEAR_LINEAR);
        _compress = capsule.readBoolean("compress", true);
        _emissiveMap = capsule.readString("emissiveMap", null);
        _emissive = capsule.readBoolean("emissive", false);
        _additive = capsule.readBoolean("additive", false);
        _solid = capsule.readBoolean("solid", true);
        _transparent = capsule.readBoolean("transparent", false);
        _alphaThreshold = capsule.readFloat("alphaThreshold",
            DEFAULT_ALPHA_THRESHOLD);
        _translucent = capsule.readBoolean("translucent", false);

        reconstruct(capsule.readFloatBuffer("vertexBuffer", null),
            capsule.readFloatBuffer("normalBuffer", null), null,
            capsule.readFloatBuffer("textureBuffer", null),
            capsule.readIntBuffer("indexBuffer", null));
    }

    @Override
    public void write (JMEExporter ex)
        throws IOException
    {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(getName(), "name", null);
        capsule.write(getLocalTranslation(), "localTranslation", null);
        capsule.write(getLocalRotation(), "localRotation", null);
        capsule.write(getLocalScale(), "localScale", null);
        capsule.write(getBatch(0).getModelBound(), "modelBound", null);
        capsule.write(getVertexBuffer(0), "vertexBuffer", null);
        capsule.write(getNormalBuffer(0), "normalBuffer", null);
        capsule.write(getTextureBuffer(0, 0), "textureBuffer", null);
        capsule.write(getIndexBuffer(0), "indexBuffer", null);
        capsule.write(_textureKey, "textureKey", null);
        capsule.write(_textures, "textures", null);
        capsule.write(_sphereMapped, "sphereMapped", false);
        capsule.write(_filterMode, "filterMode", Texture.FM_LINEAR);
        capsule.write(_mipMapMode, "mipMapMode", Texture.MM_LINEAR_LINEAR);
        capsule.write(_compress, "compress", true);
        capsule.write(_emissiveMap, "emissiveMap", null);
        capsule.write(_emissive, "emissive", false);
        capsule.write(_additive, "additive", false);
        capsule.write(_solid, "solid", true);
        capsule.write(_transparent, "transparent", false);
        capsule.write(_alphaThreshold, "alphaThreshold",
            DEFAULT_ALPHA_THRESHOLD);
        capsule.write(_translucent, "translucent", false);
    }

    // documentation inherited from interface ModelSpatial
    public void expandModelBounds ()
    {
        // no-op
    }

    // documentation inherited from interface ModelSpatial
    public void setReferenceTransforms ()
    {
        // no-op
    }

    // documentation inherited from interface ModelSpatial
    public void lockStaticMeshes (
        Renderer renderer, boolean useVBOs, boolean useDisplayLists)
    {
        if (useVBOs && renderer.supportsVBO()) {
            VBOInfo vboinfo = new VBOInfo(true);
            vboinfo.setVBOIndexEnabled(!_translucent);
            setVBOInfo(vboinfo);

        } else if (useDisplayLists && !_translucent) {
            lockMeshes(renderer);
        }
    }

    // documentation inherited from interface ModelSpatial
    public void resolveTextures (TextureProvider tprov)
    {
        if (_textures == null) {
            return;
        }
        Texture emissiveTex = null;
        if (_emissiveMap != null && TextureState.getNumberOfFixedUnits() >= 2) {
            TextureState tstate = tprov.getTexture(_emissiveMap);
            emissiveTex = tstate.getTexture();
            emissiveTex.setApply(Texture.AM_BLEND);
            emissiveTex.getBlendColor().set(ColorRGBA.white);
        }
        _tstates = new TextureState[_textures.length];
        for (int ii = 0; ii < _textures.length; ii++) {
            _tstates[ii] = tprov.getTexture(_textures[ii]);
            Texture tex = _tstates[ii].getTexture();
            if (_sphereMapped) {
                tex.setEnvironmentalMapMode(Texture.EM_SPHERE);
            }
            tex.setFilter(_filterMode);
            tex.setMipmapState(_mipMapMode);
            if (_compress && _tstates[ii].isS3TCAvailable()) {
                Image image = tex.getImage();
                int type = image.getType();
                if (type == Image.RGB888) {
                    image.setType(Image.RGB888_DXT1);
                } else if (type == Image.RGBA8888) {
                    image.setType(Image.RGBA8888_DXT5);
                }
            }
            if (emissiveTex != null) {
                _tstates[ii] = DisplaySystem.getDisplaySystem().
                    getRenderer().createTextureState();
                _tstates[ii].setTexture(emissiveTex, 0);
                _tstates[ii].setTexture(tex, 1);
            }
        }
        if (_tstates[0] != null) {
            setRenderState(_tstates[0]);
        } else {
            clearRenderState(RenderState.RS_TEXTURE);
        }
    }

    // documentation inherited from interface ModelSpatial
    public void configureShaders (ShaderCache scache)
    {
        // no-op
    }

    // documentation inherited from interface ModelSpatial
    public void storeMeshFrame (int frameId, boolean blend)
    {
        // no-op
    }

    // documentation inherited from interface ModelSpatial
    public void setMeshFrame (int frameId)
    {
        // no-op
    }

    // documentation inherited from interface ModelSpatial
    public void blendMeshFrames (int frameId1, int frameId2, float alpha)
    {
        // no-op
    }

    @Override
    protected void setupBatchList ()
    {
        batchList = Lists.newArrayListWithCapacity(1);
        TriangleBatch batch = createModelBatch();
        batch.setParentGeom(this);
        batchList.add(batch);
    }

    /**
     * Creates a batch for this mesh.
     */
    protected ModelBatch createModelBatch ()
    {
        return new ModelBatch();
    }

    /**
     * Returns the number of textures this mesh uses (they must all share the
     * same texture coordinates).
     */
    protected int getTextureCount ()
    {
        return (_emissiveMap == null || TextureState.getNumberOfFixedUnits() < 2) ? 1 : 2;
    }

    /**
     * For buffers that must be manipulated in some fashion, this method stores
     * the originals.
     */
    protected void storeOriginalBuffers ()
    {
        if (!_translucent) {
            return;
        }
        IntBuffer ibuf = getIndexBuffer(0);
        ibuf.rewind();
        IntBuffer.wrap(_oibuf = new int[ibuf.capacity()]).put(ibuf);

        FloatBuffer vbuf = getVertexBuffer(0);
        vbuf.rewind();
        FloatBuffer.wrap(_vbuf = new float[vbuf.capacity()]).put(vbuf);
    }

    /**
     * Sets the model's render states (excluding the texture state, which is
     * set by {@link #resolveTextures}) according to its configuration.
     */
    protected void setRenderStates ()
    {
        if (DisplaySystem.getDisplaySystem() == null) {
            return;
        }
        if (_backCull == null) {
            initSharedStates();
        }
        if (_emissive) {
            setRenderState(_emissiveLight);
        }
        if (_solid) {
            setRenderState(_backCull);
        }
        if (_additive) {
            setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
            setRenderState(_addAlpha);
            setRenderState(_overlayZBuffer);
            setRenderState(_noFog);
        } else if (_transparent) {
            setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
            if (_translucent) {
                setRenderState(_blendAlpha);
                setRenderState(_overlayZBuffer);
            } else if (_alphaThreshold == DEFAULT_ALPHA_THRESHOLD) {
                setRenderState(_defaultTestAlpha);
            } else {
                setRenderState(createTestAlpha(_alphaThreshold));
            }
        }
    }

    /**
     * Locks the transform and bounds of this mesh on the assumption that its
     * position will not change.
     */
    protected void lockInstance ()
    {
        lockBounds();
        _transformLocked = true;
    }

    /**
     * Returns the mip-map mode corresponding to the given string
     * (defaulting to {@link Texture#MM_LINEAR_LINEAR}).
     */
    protected static int getMipMapMode (String mmode)
    {
        if ("none".equals(mmode)) {
            return Texture.MM_NONE;
        } else if ("nearest".equals(mmode)) {
            return Texture.MM_NEAREST;
        } else if ("linear".equals(mmode)) {
            return Texture.MM_LINEAR;
        } else if ("nearest_nearest".equals(mmode)) {
            return Texture.MM_NEAREST_NEAREST;
        } else if ("nearest_linear".equals(mmode)) {
            return Texture.MM_NEAREST_LINEAR;
        } else if ("linear_nearest".equals(mmode)) {
            return Texture.MM_LINEAR_NEAREST;
        } else {
            return Texture.MM_LINEAR_LINEAR;
        }
    }

    /**
     * Initializes the states shared between all models.  Requires an active
     * display.
     */
    protected static void initSharedStates ()
    {
        Renderer renderer = DisplaySystem.getDisplaySystem().getRenderer();
        _backCull = renderer.createCullState();
        _backCull.setCullMode(CullState.CS_BACK);
        _blendAlpha = renderer.createAlphaState();
        _blendAlpha.setBlendEnabled(true);
        _addAlpha = renderer.createAlphaState();
        _addAlpha.setBlendEnabled(true);
        _addAlpha.setDstFunction(AlphaState.DB_ONE);
        _defaultTestAlpha = createTestAlpha(DEFAULT_ALPHA_THRESHOLD);
        _overlayZBuffer = renderer.createZBufferState();
        _overlayZBuffer.setFunction(ZBufferState.CF_LEQUAL);
        _overlayZBuffer.setWritable(false);
        _emissiveLight = renderer.createLightState();
        _emissiveLight.setGlobalAmbient(ColorRGBA.white);
        _noFog = renderer.createFogState();
        _noFog.setEnabled(false);
    }

    /**
     * Creates an alpha state what will throw away fragments with alpha
     * values less than or equal to the given threshold.
     */
    protected static AlphaState createTestAlpha (float threshold)
    {
        AlphaState astate = DisplaySystem.getDisplaySystem().
            getRenderer().createAlphaState();
        astate.setBlendEnabled(true);
        astate.setTestEnabled(true);
        astate.setTestFunction(AlphaState.TF_GREATER);
        astate.setReference(threshold);
        return astate;
    }

    /**
     * Sorts the encoded triangle index/distance pairs in {@link #_tcodes}
     * using a two-pass (16 bit) radix sort (as described by
     * <a href="http://codercorner.com/RadixSortRevisited.htm">Pierre
     * Terdiman</a>.  {@link #_bcounts} is assumed to be initialized to the
     * counts for the first radix.
     */
    protected static void sortTriangleCodes (int tcount)
    {
        // initialize the offsets for the first radix (LSB) and clear
        // the counts
        initByteOffsets();

        // sort by the first radix and get the counts for the second
        // (swapping directions in the hope of using the cache more
        // effectively)
        if (_stcodes == null || _stcodes.length < tcount) {
            _stcodes = new int[tcount];
        }
        int tcode;
        for (int ii = tcount - 1; ii >= 0; ii--) {
            tcode = _tcodes[ii];
            _stcodes[_boffsets[tcode & 0xFF]++] = tcode;
            _bcounts[(tcode >> 8) & 0xFF]++;
        }

        // initialize offsets for the second radix, clear counts, and
        // sort by the second radix
        initByteOffsets();
        for (int ii = 0; ii < tcount; ii++) {
            tcode = _stcodes[ii];
            _tcodes[_boffsets[(tcode >> 8) & 0xFF]++] = tcode;
        }
    }

    /**
     * Sets the initial byte offsets used to place bytes within the sorted
     * array using the byte counts, clearing the counts in the process.
     */
    protected static void initByteOffsets ()
    {
        _boffsets[0] = 0;
        for (int ii = 1; ii < 256; ii++) {
            _boffsets[ii] = _boffsets[ii - 1] + _bcounts[ii - 1];
            _bcounts[ii - 1] = 0;
        }
        _bcounts[255] = 0;
    }

    /** Sorts triangles for transparent meshes and renders overlays as well as
     * the base layer. */
    protected class ModelBatch extends TriangleBatch
    {
        @Override
        public void draw (Renderer r)
        {
            boolean drawing = (isEnabled() && r.isProcessingQueue());
            if (drawing) {
                if (_translucent) {
                    sortTriangles(r);
                }
                preDraw();
            }
            super.draw(r);
            if (_overlays != null && drawing) {
                for (int ii = 0, nn = _overlays.size(); ii < nn; ii++) {
                    preDrawOverlay(ii);
                    r.draw(this);
                    postDrawOverlay(ii);
                }
            }
        }

        /**
         * Gives derived classes a chance to update states immediately before drawing.
         */
        protected void preDraw ()
        {
        }

        /**
         * Updates the batch's states with those of the identified overlay.
         */
        protected void preDrawOverlay (int oidx)
        {
            RenderState[] overlay = _overlays.get(oidx);
            for (RenderState rstate : overlay) {
                int idx = rstate.getType();
                _ostates[idx] = states[idx];
                states[idx] = rstate;
            }
        }

        /**
         * Restores the batch's original states after drawing an overlay.
         */
        protected void postDrawOverlay (int oidx)
        {
            RenderState[] overlay = _overlays.get(oidx);
            for (RenderState rstate : overlay) {
                int idx = rstate.getType();
                states[idx] = _ostates[idx];
            }
        }

        /**
         * Sorts the batch's triangles by their distance to the camera.
         */
        protected void sortTriangles (Renderer r)
        {
            // using the camera's direction in model space and the position
            // and size of the model bound, find a set of plane parameters
            // that determine the distance to a camera-aligned plane
            // that touches the near edge of the bounding volume, as well
            // as a scaling factor that brings the distance into a 16-bit
            // integer range
            getParentGeom().getWorldRotation().inverse().mult(
                r.getCamera().getDirection(), _cdir);
            BoundingVolume mbound = getModelBound();
            Vector3f mc = mbound.getCenter();
            float radius;
            if (mbound instanceof BoundingSphere) {
                radius = ((BoundingSphere)mbound).getRadius();
            } else { // mbound instanceof BoundingBox
                BoundingBox bbox = (BoundingBox)mbound;
                radius = FastMath.sqrt(3f) * Math.max(bbox.xExtent,
                    Math.max(bbox.yExtent, bbox.zExtent));
            }
            float a = _cdir.x, b = _cdir.y, c = _cdir.z,
                d = radius - a*mc.x - b*mc.y - c*mc.z,
                dscale = 65535f / (radius * 2);

            // premultiply the scale and averaging factor
            a *= dscale / 3f;
            b *= dscale / 3f;
            c *= dscale / 3f;
            d *= dscale;

            // encode the model's triangles into integers such that the
            // high 16 bits represent the original triangle index and the
            // low 16 bits represent the distance to the plane.  also
            // increment the byte counts used for radix sorting
            int tcount = getTriangleCount(), idist;
            if (_tcodes == null || _tcodes.length < tcount) {
                _tcodes = new int[tcount];
            }
            int i1, i2, i3;
            for (int ii = 0, idx = 0; ii < tcount; ii++) {
                i1 = _oibuf[idx++] * 3;
                i2 = _oibuf[idx++] * 3;
                i3 = _oibuf[idx++] * 3;
                idist = (int)(
                    a * (_vbuf[i1++] + _vbuf[i2++] + _vbuf[i3++]) +
                    b * (_vbuf[i1++] + _vbuf[i2++] + _vbuf[i3++]) +
                    c * (_vbuf[i1++] + _vbuf[i2++] + _vbuf[i3++]) + d);
                _tcodes[ii] = (ii << 16) | idist;
                _bcounts[idist & 0xFF]++;
            }

            // sort the encoded triangles by increasing distance
            sortTriangleCodes(tcount);

            // reorder the triangles as dictated by the sorted codes, furthest
            // triangles first
            int icount = tcount * 3, idx;
            if (_sibuf == null || _sibuf.length < icount) {
                _sibuf = new int[icount];
            }
            for (int ii = tcount - 1, sidx = 0; ii >= 0; ii--) {
                idx = ((_tcodes[ii] >> 16) & 0xFFFF) * 3;
                _sibuf[sidx++] = _oibuf[idx++];
                _sibuf[sidx++] = _oibuf[idx++];
                _sibuf[sidx++] = _oibuf[idx];
            }

            // copy the indices to the buffer
            IntBuffer ibuf = getIndexBuffer();
            ibuf.rewind();
            ibuf.put(_sibuf, 0, icount);
        }

        /**
         * Gives derived classes a chance to update the states immediately before drawing.
         *
         * @param oidx the index of the overlay being rendered, or -1 for the base layer.
         */
        protected void processStates (int oidx)
        {
        }

        /** Temporarily stores the original states. */
        protected RenderState[] _ostates =
            new RenderState[RenderState.RS_MAX_STATE];
    }

    /** The name of the texture specified in the model file, which acts as a
     * property key and a default value. */
    protected String _textureKey;

    /** The name of this model's textures, or <code>null</code> for none. */
    protected String[] _textures;

    /** Whether or not to use sphere mapping on this model's textures. */
    protected boolean _sphereMapped;

    /** The filter mode to use on magnification. */
    protected int _filterMode;

    /** The mipmap mode to use on minification. */
    protected int _mipMapMode;

    /** Whether or not to use compressed textures, if available. */
    protected boolean _compress;

    /** The emissive map, if specified. */
    protected String _emissiveMap;

    /** Whether or not this mesh is completely emissive. */
    protected boolean _emissive;

    /** Whether or not this mesh should be rendered with additive blending. */
    protected boolean _additive;

    /** Whether or not this mesh can enable back-face culling. */
    protected boolean _solid;

    /** Whether or not this mesh must be rendered as transparent. */
    protected boolean _transparent;

    /** The alpha threshold below which fragments are discarded. */
    protected float _alphaThreshold;

    /** Whether or not the triangles of this mesh should be depth-sorted before
     * rendering. */
    protected boolean _translucent;

    /** If non-null, additional layers to render over the base layer. */
    protected ArrayList<RenderState[]> _overlays;

    /** For prototype meshes, the resolved texture states. */
    protected TextureState[] _tstates;

    /** Whether or not the transform has been locked.  This operates in a
     * slightly different way than JME's locking, in that it allows applying
     * transformations to display lists. */
    protected boolean _transformLocked;

    /** For depth-sorted and skinned meshes, the array of vertices. */
    protected float[] _vbuf;

    /** For depth-sorted meshes, the original array of indices. */
    protected int[] _oibuf;

    /** The shared state for back face culling. */
    protected static CullState _backCull;

    /** The shared state for alpha blending. */
    protected static AlphaState _blendAlpha;

    /** The shared state for additive blending. */
    protected static AlphaState _addAlpha;

    /** The shared state for alpha testing with the default threshold. */
    protected static AlphaState _defaultTestAlpha;

    /** The shared state for checking, but not writing to, the z buffer. */
    protected static ZBufferState _overlayZBuffer;

    /** A light state that simulates emissivity. */
    protected static LightState _emissiveLight;

    /** A fog state that disables fog. */
    protected static FogState _noFog;

    /** Work vector to store the camera direction. */
    protected static Vector3f _cdir = new Vector3f();

    /** Work arrays used to sort triangles. */
    protected static int[] _tcodes, _stcodes;

    /** Holds counts of each byte and array offsets for radix sorting. */
    protected static int[] _bcounts = new int[256], _boffsets = new int[256];

    /** Work array used to hold indices of sorted triangles. */
    protected static int[] _sibuf;

    /** The default alpha threshold. */
    protected static final float DEFAULT_ALPHA_THRESHOLD = 0.5f;

    private static final long serialVersionUID = 1;
}
