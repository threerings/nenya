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

import java.util.HashSet;
import java.util.List;

import com.jme.math.Matrix4f;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.state.RenderState;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.OutputCapsule;

import com.threerings.jme.util.JmeUtil;
import com.threerings.jme.util.ShaderCache;

/**
 * A {@link Node} with a serialization mechanism tailored to stored models.
 */
public class ModelNode extends Node
    implements ModelSpatial
{
    /**
     * No-arg constructor for deserialization.
     */
    public ModelNode ()
    {
        super("node");
    }

    /**
     * Standard constructor.
     */
    public ModelNode (String name)
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
     * Recursively searches the scene graph rooted at this node for a
     * node with the provided name.
     */
    public Spatial getDescendant (String name)
    {
        for (int ii = 0, nn = getQuantity(); ii < nn; ii++) {
            Spatial child = getChild(ii);
            if (child.getName().equals(name)) {
                return child;
            } else if (child instanceof ModelNode) {
                child = ((ModelNode)child).getDescendant(name);
                if (child != null) {
                    return child;
                }
            }
        }
        return null;
    }

    /**
     * Returns a reference to the model space transform of the node.
     */
    public Matrix4f getModelTransform ()
    {
        return _modelTransform;
    }

    @Override
    public void updateWorldData (float time)
    {
        // we use locked bounds as an indication that we can skip the update
        // altogether
        if ((lockedMode & LOCKED_BOUNDS) == 0) {
            super.updateWorldData(time);
        }
    }

    @Override
    public void updateWorldBound ()
    {
        // don't bother updating if we know there are no visible descendants
        if (_hasVisibleDescendants) {
            super.updateWorldBound();
        }
    }

    @Override
    public void updateWorldVectors ()
    {
        super.updateWorldVectors();
        if (parent instanceof ModelNode) {
            JmeUtil.setTransform(getLocalTranslation(), getLocalRotation(),
                getLocalScale(), _localTransform);
            ((ModelNode)parent).getModelTransform().mult(_localTransform,
                _modelTransform);

        } else {
            _modelTransform.loadIdentity();
        }
    }

    // documentation inherited from interface ModelSpatial
    public Spatial putClone (Spatial store, Model.CloneCreator properties)
    {
        ModelNode mstore = (ModelNode)properties.originalToCopy.get(this);
        if (mstore != null) {
            return mstore;
        } else if (store == null) {
            mstore = new ModelNode(getName());
        } else {
            mstore = (ModelNode)store;
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
        for (int ii = 0, nn = getQuantity(); ii < nn; ii++) {
            Spatial child = getChild(ii);
            if (child instanceof ModelSpatial) {
                mstore.attachChild(
                    ((ModelSpatial)child).putClone(null, properties));
            }
        }
        mstore._hasVisibleDescendants = _hasVisibleDescendants;
        return mstore;
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
        List<?> children = capsule.readSavableArrayList("children", null);
        if (children != null) {
            for (Object child : children) {
                attachChild((Spatial)child);
            }
        }
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
        capsule.writeSavableArrayList(getChildren(), "children", null);
    }

    // documentation inherited from interface ModelSpatial
    public void expandModelBounds ()
    {
        for (int ii = 0, nn = getQuantity(); ii < nn; ii++) {
            Spatial child = getChild(ii);
            if (child instanceof ModelSpatial) {
                ((ModelSpatial)child).expandModelBounds();
            }
        }
    }

    // documentation inherited from interface ModelSpatial
    public void setReferenceTransforms ()
    {
        updateWorldVectors();
        for (int ii = 0, nn = getQuantity(); ii < nn; ii++) {
            Spatial child = getChild(ii);
            if (child instanceof ModelSpatial) {
                ((ModelSpatial)child).setReferenceTransforms();
            }
        }
    }

    // documentation inherited from interface ModelSpatial
    public void lockStaticMeshes (
        Renderer renderer, boolean useVBOs, boolean useDisplayLists)
    {
        for (int ii = 0, nn = getQuantity(); ii < nn; ii++) {
            Spatial child = getChild(ii);
            if (child instanceof ModelSpatial) {
                ((ModelSpatial)child).lockStaticMeshes(renderer, useVBOs,
                    useDisplayLists);
            }
        }
    }

    // documentation inherited from interface ModelSpatial
    public void resolveTextures (TextureProvider tprov)
    {
        for (int ii = 0, nn = getQuantity(); ii < nn; ii++) {
            Spatial child = getChild(ii);
            if (child instanceof ModelSpatial) {
                ((ModelSpatial)child).resolveTextures(tprov);
            }
        }
    }

    // documentation inherited from interface ModelSpatial
    public void configureShaders (ShaderCache scache)
    {
        for (int ii = 0, nn = getQuantity(); ii < nn; ii++) {
            Spatial child = getChild(ii);
            if (child instanceof ModelSpatial) {
                ((ModelSpatial)child).configureShaders(scache);
            }
        }
    }

    // documentation inherited from interface ModelSpatial
    public void storeMeshFrame (int frameId, boolean blend)
    {
        for (int ii = 0, nn = getQuantity(); ii < nn; ii++) {
            Spatial child = getChild(ii);
            if (child instanceof ModelSpatial) {
                ((ModelSpatial)child).storeMeshFrame(frameId, blend);
            }
        }
    }

    // documentation inherited from interface ModelSpatial
    public void setMeshFrame (int frameId)
    {
        for (int ii = 0, nn = getQuantity(); ii < nn; ii++) {
            Spatial child = getChild(ii);
            if (child instanceof ModelSpatial) {
                ((ModelSpatial)child).setMeshFrame(frameId);
            }
        }
    }

    // documentation inherited from interface ModelSpatial
    public void blendMeshFrames (int frameId1, int frameId2, float alpha)
    {
        for (int ii = 0, nn = getQuantity(); ii < nn; ii++) {
            Spatial child = getChild(ii);
            if (child instanceof ModelSpatial) {
                ((ModelSpatial)child).blendMeshFrames(
                    frameId1, frameId2, alpha);
            }
        }
    }

    /**
     * Sets whether this node should be culled even if it has mesh
     * descendants.
     */
    public void setForceCull (boolean force)
    {
        _forceCull = force;
    }

    /**
     * Checks whether this node should be culled even if it has mesh
     * descendants.
     */
    public boolean getForceCull ()
    {
        return _forceCull;
    }

    /**
     * Sets the cull state of any nodes that do not contain geometric
     * descendants to {@link CULL_ALWAYS} so that they don't waste
     * rendering time.
     *
     * @return true if this node should be drawn, false if it contains
     * no mesh descendants
     */
    protected boolean cullInvisibleNodes ()
    {
        for (int ii = 0, nn = getQuantity(); ii < nn; ii++) {
            Spatial child = getChild(ii);
            if (!(child instanceof ModelNode) ||
                ((ModelNode)child).cullInvisibleNodes()) {
                _hasVisibleDescendants = true;
            }
        }
        updateCullMode();
        return _hasVisibleDescendants;
    }

    /**
     * Locks the transforms and bounds of this instance with the assumption
     * that the position will never change.
     *
     * @param targets the targets of the model's controllers, which determine
     * the subset of nodes that can be locked
     * @return true if this node is a target or contains any targets, otherwise
     * false
     */
    protected boolean lockInstance (HashSet<Spatial> targets)
    {
        updateWorldVectors();
        lockedMode |= LOCKED_TRANSFORMS;

        boolean containsTargets = false;
        for (int ii = 0, nn = getQuantity(); ii < nn; ii++) {
            Spatial child = getChild(ii);
            if (targets.contains(child) || (child instanceof ModelNode &&
                ((ModelNode)child).lockInstance(targets))) {
                containsTargets = true;

            } else if (child instanceof ModelMesh) {
                ((ModelMesh)child).lockInstance();
            }
        }
        if (containsTargets) {
            return true;
        } else {
            updateWorldBound();
            lockedMode |= LOCKED_BOUNDS;
            return false;
        }
    }

    /**
     * Recursively culls all model nodes under this one in preparation for
     * activating the ones listed in an animation.
     */
    protected void cullModelNodes ()
    {
        for (int ii = 0, nn = getQuantity(); ii < nn; ii++) {
            Spatial child = getChild(ii);
            if (child instanceof ModelNode) {
                child.setCullMode(CULL_ALWAYS);
                ((ModelNode)child).cullModelNodes();
            }
        }
    }

    /**
     * Makes this node visible if and only if it has visible descendants and
     * has not been specifically culled.
     */
    protected void updateCullMode ()
    {
        setCullMode((_hasVisibleDescendants && !_forceCull) ?
            CULL_INHERIT : CULL_ALWAYS);
    }

    /** The node's transform in local and model space. */
    protected Matrix4f _localTransform = new Matrix4f(),
        _modelTransform = new Matrix4f();

    /** Whether or not this node has mesh descendants. */
    protected boolean _hasVisibleDescendants;

    /** If true, cull the node even if it has mesh descendants. */
    protected boolean _forceCull;

    private static final long serialVersionUID = 1;
}
