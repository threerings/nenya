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

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.lwjgl.opengl.GLContext;

import com.jme.bounding.BoundingVolume;
import com.jme.math.Matrix4f;
import com.jme.renderer.Renderer;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh;
import com.jme.scene.VBOInfo;
import com.jme.scene.batch.SharedBatch;
import com.jme.scene.batch.TriangleBatch;
import com.jme.scene.state.GLSLShaderObjectsState;
import com.jme.scene.state.RenderState;
import com.jme.system.DisplaySystem;
import com.jme.util.ShaderAttribute;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.OutputCapsule;
import com.jme.util.export.Savable;
import com.jme.util.geom.BufferUtils;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.ListUtil;

import com.threerings.jme.util.JmeUtil;
import com.threerings.jme.util.ShaderCache;
import com.threerings.jme.util.ShaderConfig;

/**
 * A triangle mesh that deforms according to a bone hierarchy.
 */
public class SkinMesh extends ModelMesh
{
    /** The maximum number of bone matrices that we can use for hardware skinning. */
    public static final int MAX_SHADER_BONE_COUNT = 31;

    /** The maximum number of bones influencing a single vertex for hardware skinning. */
    public static final int MAX_SHADER_BONES_PER_VERTEX = 4;

    /** Represents the vertex weights of a group of vertices influenced by the
     * same set of bones. */
    public static class WeightGroup
        implements Savable
    {
        /** The number of vertices in this weight group. */
        public int vertexCount;

        /** The bones influencing this group. */
        public Bone[] bones;

        /** The array of interleaved weights (of length <code>vertexCount *
         * boneIndices.length</code>): weights for first vertex, weights for
         * second, etc. */
        public float[] weights;

        /**
         * Rebinds this weight group for a prototype instance.
         *
         * @param bmap the mapping from prototype to instance bones
         */
        public WeightGroup rebind (Map<Bone, Bone> bmap)
        {
            WeightGroup wgroup = new WeightGroup();
            wgroup.vertexCount = vertexCount;
            wgroup.bones = new Bone[bones.length];
            for (int ii = 0; ii < bones.length; ii++) {
                wgroup.bones[ii] = bmap.get(bones[ii]);
            }
            wgroup.weights = weights;
            return wgroup;
        }

        // documentation inherited
        public Class<?> getClassTag ()
        {
            return getClass();
        }

        // documentation inherited
        public void read (JMEImporter im)
            throws IOException
        {
            InputCapsule capsule = im.getCapsule(this);
            vertexCount = capsule.readInt("vertexCount", 0);
            bones = ArrayUtil.copy(capsule.readSavableArray("bones", null),
                new Bone[0]);
            weights = capsule.readFloatArray("weights", null);
        }

        // documentation inherited
        public void write (JMEExporter ex)
            throws IOException
        {
            OutputCapsule capsule = ex.getCapsule(this);
            capsule.write(vertexCount, "vertexCount", 0);
            capsule.write(bones, "bones", null);
            capsule.write(weights, "weights", null);
        }

        private static final long serialVersionUID = 1;
    }

    /** Represents a bone that influences the mesh. */
    public static class Bone
        implements Savable
    {
        /** The node that defines the bone's position. */
        public ModelNode node;

        /** The inverse of the bone's model space reference transform. */
        public transient Matrix4f invRefTransform;

        /** The bone's current transform in model space. */
        public transient Matrix4f transform;

        public Bone (ModelNode node)
        {
            this();
            this.node = node;
        }

        public Bone ()
        {
            transform = new Matrix4f();
        }

        /**
         * Rebinds this bone for a prototype instance.
         *
         * @param pnodes a mapping from prototype nodes to instance nodes
         */
        public Bone rebind (Map<ModelSpatial, ModelSpatial> pnodes)
        {
            Bone bone = new Bone((ModelNode)pnodes.get(node));
            bone.invRefTransform = invRefTransform;
            bone.transform = new Matrix4f();
            return bone;
        }

        // documentation inherited
        public Class<?> getClassTag ()
        {
            return getClass();
        }

        // documentation inherited
        public void read (JMEImporter im)
            throws IOException
        {
            InputCapsule capsule = im.getCapsule(this);
            node = (ModelNode)capsule.readSavable("node", null);
        }

        // documentation inherited
        public void write (JMEExporter ex)
            throws IOException
        {
            OutputCapsule capsule = ex.getCapsule(this);
            capsule.write(node, "node", null);
        }

        private static final long serialVersionUID = 1;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public SkinMesh ()
    {
    }

    /**
     * Creates an empty mesh.
     */
    public SkinMesh (String name)
    {
        super(name);
    }

    /**
     * Sets the array of weight groups that determine how bones affect
     * each vertex.
     */
    public void setWeightGroups (WeightGroup[] weightGroups)
    {
        _weightGroups = weightGroups;

        // compile a list of all referenced bones
        HashSet<Bone> bones = Sets.newHashSet();
        for (WeightGroup group : weightGroups) {
            Collections.addAll(bones, group.bones);
        }
        _bones = bones.toArray(new Bone[bones.size()]);
    }

    @Override
    public void addOverlay (RenderState[] overlay)
    {
        // add a cloned state config (with same uniforms) for the overlay
        super.addOverlay(overlay);
        if (_sconfig == null) {
            return;
        }
        if (_osconfigs == null) {
            _osconfigs = Lists.newArrayListWithCapacity(1);
        }
        SkinShaderConfig osconfig = (SkinShaderConfig)_sconfig.clone();
        osconfig.getState().uniforms = _sconfig.getState().uniforms;
        _osconfigs.add(osconfig);
    }

    @Override
    public void removeOverlay (RenderState[] overlay)
    {
        // remove the corresponding state config
        int idx = (_overlays == null) ? -1 : _overlays.indexOf(overlay);
        super.removeOverlay(overlay);
        if (_osconfigs != null && idx >= 0) {
            _osconfigs.remove(idx);
            if (_osconfigs.isEmpty()) {
                _osconfigs = null;
            }
        }
    }

    @Override
    public void reconstruct (
        FloatBuffer vertices, FloatBuffer normals, FloatBuffer colors,
        FloatBuffer textures, IntBuffer indices)
    {
        super.reconstruct(vertices, normals, colors, textures, indices);

        // initialize the quantized frame table
        _frames = new HashIntMap<Object>();
    }

    @Override
    public Spatial putClone (Spatial store, Model.CloneCreator properties)
    {
        SkinMesh mstore = (SkinMesh)properties.originalToCopy.get(this);
        if (mstore != null) {
            return mstore;
        } else if (store == null) {
            mstore = new SkinMesh(getName());
        } else {
            mstore = (SkinMesh)store;
        }
        GLSLShaderObjectsState sstate = (GLSLShaderObjectsState)getRenderState(
            RenderState.RS_GLSL_SHADER_OBJECTS);
        if (sstate == null) {
            // vertices and normals must be cloned if not using a shader
            properties.removeProperty("vertices");
            properties.removeProperty("normals");
        }
        properties.removeProperty("displaylistid");
        super.putClone(mstore, properties);
        if (sstate == null) {
            properties.addProperty("vertices");
            properties.addProperty("normals");
        }
        properties.addProperty("displaylistid");
        mstore._frames = _frames;
        mstore._useDisplayLists = _useDisplayLists;
        mstore._invRefTransform = _invRefTransform;
        mstore._bones = new Bone[_bones.length];
        HashMap<Bone, Bone> bmap = Maps.newHashMap();
        for (int ii = 0; ii < _bones.length; ii++) {
            bmap.put(_bones[ii], mstore._bones[ii] =
                _bones[ii].rebind(properties.originalToCopy));
        }
        mstore._weightGroups = new WeightGroup[_weightGroups.length];
        for (int ii = 0; ii < _weightGroups.length; ii++) {
            mstore._weightGroups[ii] = _weightGroups[ii].rebind(bmap);
        }
        mstore._ovbuf = _ovbuf;
        mstore._onbuf = _onbuf;
        mstore._vbuf = (sstate == null) ? new float[_vbuf.length] : _vbuf;
        mstore._nbuf = (sstate == null) ? new float[_nbuf.length] : _nbuf;
        if (_sconfig != null) {
            mstore._sconfig = (SkinShaderConfig)_sconfig.clone();
            mstore.setRenderState(mstore._sconfig.getState());
        }
        return mstore;
    }

    @Override
    public void read (JMEImporter im)
        throws IOException
    {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        setWeightGroups(ArrayUtil.copy(capsule.readSavableArray(
            "weightGroups", null), new WeightGroup[0]));
    }

    @Override
    public void write (JMEExporter ex)
        throws IOException
    {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(_weightGroups, "weightGroups", null);
    }

    @Override
    public void expandModelBounds ()
    {
        BoundingVolume obound = getBatch(0).getModelBound().clone(null);
        updateModelBound();
        getBatch(0).getModelBound().mergeLocal(obound);
    }

    @Override
    public void setReferenceTransforms ()
    {
        _invRefTransform = new Matrix4f();
        if (parent instanceof ModelNode) {
            Matrix4f transform = new Matrix4f();
            JmeUtil.setTransform(getLocalTranslation(), getLocalRotation(),
                getLocalScale(), transform);
            ((ModelNode)parent).getModelTransform().mult(transform,
                _invRefTransform);
            _invRefTransform.invertLocal();
        }
        for (Bone bone : _bones) {
            bone.invRefTransform =
                _invRefTransform.mult(bone.node.getModelTransform()).invert();
        }
    }

    @Override
    public void lockStaticMeshes (
        Renderer renderer, boolean useVBOs, boolean useDisplayLists)
    {
        // we can use VBOs for color, texture, and indices if not using shaders
        if (useVBOs && renderer.supportsVBO()) {
            // use VBOs for shader attributes
            GLSLShaderObjectsState sstate = (GLSLShaderObjectsState)getRenderState(
                RenderState.RS_GLSL_SHADER_OBJECTS);
            if (sstate != null) {
                for (ShaderAttribute attrib : sstate.attribs.values()) {
                    attrib.useVBO = true;
                }
            }

            VBOInfo vboinfo = new VBOInfo(true);
            if (sstate == null) {
                vboinfo.setVBOVertexEnabled(false);
                vboinfo.setVBONormalEnabled(false);
            }
            vboinfo.setVBOIndexEnabled(!_translucent);
            setVBOInfo(vboinfo);
        }
        _useDisplayLists = useDisplayLists && !_translucent;
    }

    @Override
    public void configureShaders (ShaderCache scache)
    {
        if (_disableShaders || !GLContext.getCapabilities().GL_ARB_vertex_shader ||
                _bones.length > MAX_SHADER_BONE_COUNT) {
            return;
        }
        int bonesPerVertex = 0;
        for (WeightGroup group : _weightGroups) {
            bonesPerVertex = Math.max(group.bones.length, bonesPerVertex);
        }
        if (bonesPerVertex > MAX_SHADER_BONES_PER_VERTEX) {
            return;
        }
        _sconfig = new SkinShaderConfig(scache, _emissiveMap != null);
        if (_sconfig.update(getBatch(0).states)) {
            setShaderAttributes();
            setRenderState(_sconfig.getState());
        } else {
            _sconfig = null;
            _disableShaders = true;
        }
    }

    @Override
    public void storeMeshFrame (int frameId, boolean blend)
    {
        _storeFrameId = frameId;
        _storeBlend = blend;
    }

    @Override
    public void setMeshFrame (int frameId)
    {
        TriangleBatch batch = getBatch(0),
            tbatch = (TriangleBatch)_frames.get(frameId);
        if (batch instanceof SharedBatch) {
            ((SharedBatch)batch).setTarget(tbatch);
        } else {
            clearBatches();
            addBatch(new SharedBatch(tbatch));
            getBatch(0).updateRenderState();
        }
    }

    @Override
    public void blendMeshFrames (int frameId1, int frameId2, float alpha)
    {
        BlendFrame frame1 = (BlendFrame)_frames.get(frameId1),
            frame2 = (BlendFrame)_frames.get(frameId2);
        frame1.blend(frame2, alpha, _vbuf, _nbuf);
        FloatBuffer vbuf = getVertexBuffer(0), nbuf = getNormalBuffer(0);
        vbuf.rewind();
        vbuf.put(_vbuf);
        nbuf.rewind();
        nbuf.put(_nbuf);
    }

    @Override
    public void updateWorldData (float time)
    {
        super.updateWorldData(time);
        if (_weightGroups == null || _storeFrameId == -1 ||
                (_storeFrameId == 0 && getCullMode() == CULL_ALWAYS)) {
            return;
        }
        // update the bone transforms
        for (Bone bone : _bones) {
            _invRefTransform.mult(bone.node.getModelTransform(),
                bone.transform);
            bone.transform.multLocal(bone.invRefTransform);
        }

        // if we're using shaders, initialize the uniform variables with the bone transforms
        GLSLShaderObjectsState sstate = (GLSLShaderObjectsState)getRenderState(
            RenderState.RS_GLSL_SHADER_OBJECTS);
        if (sstate != null) {
            for (int ii = 0; ii < _bones.length; ii++) {
                sstate.setUniform("boneTransforms[" + ii + "]", _bones[ii].transform, true);
            }
            return;
        }

        // deform the mesh according to the positions of the bones (this code
        // is ugly as sin because it's optimized at a low level)
        Bone[] bones;
        int vertexCount, jj, kk, ww;
        float[] weights;
        Matrix4f m;
        float weight, ovx, ovy, ovz, onx, ony, onz, vx, vy, vz, nx, ny, nz;
        for (int ii = 0, bidx = 0; ii < _weightGroups.length; ii++) {
            vertexCount = _weightGroups[ii].vertexCount;
            bones = _weightGroups[ii].bones;
            weights = _weightGroups[ii].weights;
            for (jj = 0, ww = 0; jj < vertexCount; jj++) {
                ovx = _ovbuf[bidx];
                ovy = _ovbuf[bidx + 1];
                ovz = _ovbuf[bidx + 2];
                onx = _onbuf[bidx];
                ony = _onbuf[bidx + 1];
                onz = _onbuf[bidx + 2];
                vx = vy = vz = 0f;
                nx = ny = nz = 0f;
                for (kk = 0; kk < bones.length; kk++) {
                    m = bones[kk].transform;
                    weight = weights[ww++];

                    vx += (ovx*m.m00 + ovy*m.m01 + ovz*m.m02 + m.m03) * weight;
                    vy += (ovx*m.m10 + ovy*m.m11 + ovz*m.m12 + m.m13) * weight;
                    vz += (ovx*m.m20 + ovy*m.m21 + ovz*m.m22 + m.m23) * weight;

                    nx += (onx*m.m00 + ony*m.m01 + onz*m.m02) * weight;
                    ny += (onx*m.m10 + ony*m.m11 + onz*m.m12) * weight;
                    nz += (onx*m.m20 + ony*m.m21 + onz*m.m22) * weight;
                }
                _vbuf[bidx] = vx;
                _vbuf[bidx + 1] = vy;
                _vbuf[bidx + 2] = vz;
                _nbuf[bidx++] = nx;
                _nbuf[bidx++] = ny;
                _nbuf[bidx++] = nz;
            }
        }

        // if skinning in real time, copy the data from arrays to buffers;
        // otherwise, store the mesh as an animation frame
        if (_storeFrameId == 0) {
            FloatBuffer vbuf = getVertexBuffer(0), nbuf = getNormalBuffer(0);
            vbuf.rewind();
            vbuf.put(_vbuf);
            nbuf.rewind();
            nbuf.put(_nbuf);
        } else {
            storeFrame();
            _storeFrameId = -1;
        }
    }

    /**
     * Stores the current frame data for later use.
     */
    protected void storeFrame ()
    {
        if (_storeBlend) {
            _frames.put(_storeFrameId, new BlendFrame(_vbuf.clone(), _nbuf.clone()));
        } else {
            TriangleBatch batch = getBatch(0), tbatch = new TriangleBatch();
            tbatch.setParentGeom(DUMMY_MESH);
            tbatch.setColorBuffer(batch.getColorBuffer());
            int nunits = batch.getNumberOfUnits();
            for (int ii = 0; ii < nunits; ii++) {
                tbatch.setTextureBuffer(batch.getTextureBuffer(ii), ii);
            }
            tbatch.setIndexBuffer(batch.getIndexBuffer());
            tbatch.setVertexBuffer(BufferUtils.createFloatBuffer(_vbuf));
            tbatch.setNormalBuffer(BufferUtils.createFloatBuffer(_nbuf));
            VBOInfo ovboinfo = batch.getVBOInfo();
            if (ovboinfo != null) {
                VBOInfo vboinfo = new VBOInfo(true);
                vboinfo.setVBOIndexEnabled(!_translucent);
                vboinfo.setVBOColorID(ovboinfo.getVBOColorID());
                for (int ii = 0; ii < nunits; ii++) {
                    vboinfo.setVBOTextureID(ii, ovboinfo.getVBOTextureID(ii));
                }
                vboinfo.setVBOIndexID(ovboinfo.getVBOIndexID());
                tbatch.setVBOInfo(vboinfo);
            } else if (_useDisplayLists) {
                tbatch.lockMeshes(
                    DisplaySystem.getDisplaySystem().getRenderer());
            }
            _frames.put(_storeFrameId, tbatch);
        }
    }

    @Override
    protected ModelBatch createModelBatch ()
    {
        // update the shader configs immediately before drawing
        return new ModelBatch() {
            protected void preDraw () {
                if (_sconfig != null) {
                    _sconfig.update(states);
                }
            }
            protected void preDrawOverlay (int oidx) {
                super.preDrawOverlay(oidx);
                if (_osconfigs != null) {
                    _ostates[RenderState.RS_GLSL_SHADER_OBJECTS] =
                        states[RenderState.RS_GLSL_SHADER_OBJECTS];
                    SkinShaderConfig osconfig = _osconfigs.get(oidx);
                    states[RenderState.RS_GLSL_SHADER_OBJECTS] = osconfig.getState();
                    _osconfigs.get(oidx).update(states);
                }
            }
            protected void postDrawOverlay (int oidx) {
                super.postDrawOverlay(oidx);
                if (_osconfigs != null) {
                    states[RenderState.RS_GLSL_SHADER_OBJECTS] =
                        _ostates[RenderState.RS_GLSL_SHADER_OBJECTS];
                }
            }
        };
    }

    @Override
    protected void storeOriginalBuffers ()
    {
        super.storeOriginalBuffers();

        FloatBuffer vbuf = getVertexBuffer(0), nbuf = getNormalBuffer(0);
        vbuf.rewind();
        nbuf.rewind();
        FloatBuffer.wrap(_ovbuf = new float[vbuf.capacity()]).put(vbuf);
        FloatBuffer.wrap(_onbuf = new float[nbuf.capacity()]).put(nbuf);
        _vbuf = new float[_ovbuf.length];
        _nbuf = new float[_onbuf.length];
    }

    /**
     * Initializes the skin shader attributes (bone indices and weights) in the supplied state.
     */
    protected void setShaderAttributes ()
    {
        int size = getBatch(0).getVertexCount() * 4;
        ByteBuffer bibuf = BufferUtils.createByteBuffer(size);
        FloatBuffer bwbuf = BufferUtils.createFloatBuffer(size);

        for (WeightGroup group : _weightGroups) {
            byte[] indices = new byte[4];
            for (int ii = 0; ii < 4; ii++) {
                indices[ii] = (byte)((ii < group.bones.length) ?
                    ListUtil.indexOf(_bones, group.bones[ii]) : 0);
            }
            for (int ii = 0, widx = 0; ii < group.vertexCount; ii++) {
                bibuf.put(indices);
                for (int jj = 0; jj < 4; jj++) {
                    bwbuf.put((jj < group.bones.length) ? group.weights[widx++] : 0f);
                }
            }
        }
        bibuf.rewind();
        bwbuf.rewind();

        GLSLShaderObjectsState sstate = _sconfig.getState();
        sstate.setAttributePointer("boneIndices", 4, false, false, 0, bibuf);
        sstate.setAttributePointer("boneWeights", 4, false, 0, bwbuf);
    }

    /** Tracks the configuration of a skin shader. */
    protected static class SkinShaderConfig extends ShaderConfig
    {
        public SkinShaderConfig (ShaderCache scache, boolean emissiveMapped)
        {
            super(scache);
            _emissiveMapped = emissiveMapped;

            // set bindings from texture units to samplers
            if (emissiveMapped) {
                _state.setUniform("diffuseMap", 1);
                _state.setUniform("emissiveMap", 0);
            } else {
                _state.setUniform("diffuseMap", 0);
            }
        }

        @Override
        protected String getVertexShader ()
        {
            return "media/jme/skin.vert";
        }

        @Override
        protected String getFragmentShader ()
        {
            return "media/jme/skin.frag";
        }

        @Override
        protected void getDefinitions (ArrayList<String> defs)
        {
            super.getDefinitions(defs);
            if (_emissiveMapped) {
                defs.add("EMISSIVE_MAPPED");
            }
        }

        @Override
        protected void getDerivedDefinitions (ArrayList<String> ddefs)
        {
            super.getDerivedDefinitions(ddefs);
            ddefs.add("MAX_BONE_COUNT " + MAX_SHADER_BONE_COUNT);
        }

        protected boolean _emissiveMapped;
    }

    /** A stored frame used for linear blending. */
    protected static class BlendFrame
    {
        /** The skinned vertex and normal values. */
        public float[] vbuf, nbuf;

        public BlendFrame (float[] vbuf, float[] nbuf)
        {
            this.vbuf = vbuf;
            this.nbuf = nbuf;
        }

        public void blend (
            BlendFrame next, float alpha, float[] rvbuf, float[] rnbuf)
        {
            float[] nvbuf = next.vbuf, nnbuf = next.nbuf;
            float ialpha = 1f - alpha;
            for (int ii = 0, nn = vbuf.length; ii < nn; ii++) {
                rvbuf[ii] = vbuf[ii] * ialpha + nvbuf[ii] * alpha;
                rnbuf[ii] = nbuf[ii] * ialpha + nnbuf[ii] * alpha;
            }
        }
    }

    /** Pre-skinned {@link TriangleBatch}es or {@link BlendFrame}s shared
     * between all instances corresponding to frame ids from
     * {@link #storeAnimationFrame}. */
    protected HashIntMap<Object> _frames;

    /** Whether or to use display lists if VBOs are unavailable for quantized
     * meshes. */
    protected boolean _useDisplayLists;

    /** The inverse of the model space reference transform. */
    protected Matrix4f _invRefTransform;

    /** The groups of vertices influenced by different sets of bones. */
    protected WeightGroup[] _weightGroups;

    /** The bones referenced by the weight groups. */
    protected Bone[] _bones;

    /** The original (undeformed) vertex and normal buffers and the deformed
     * versions. */
    protected float[] _onbuf, _ovbuf, _nbuf;

    /** The primary skin shader configuration. */
    protected SkinShaderConfig _sconfig;

    /** Skin shader configurations for each overlay. */
    protected ArrayList<SkinShaderConfig> _osconfigs;

    /** The frame id to store on the next update.  If 0, don't store any frame
     * and skin the mesh as normal.  If -1, a frame has been stored and thus
     * skinning should only take place when further frames are requested. */
    protected int _storeFrameId;

    /** Whether or not the stored frame id will be used for blending. */
    protected boolean _storeBlend;

    /** Set if we determine that our shaders don't compile to prevent us from trying again. */
    protected static boolean _disableShaders;

    /** A dummy mesh that simply hold transformation values. */
    protected static final TriMesh DUMMY_MESH = new TriMesh();

    private static final long serialVersionUID = 1;
}
