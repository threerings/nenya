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

import java.util.HashMap;

import com.google.common.collect.Maps;

import com.jme.image.Texture;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;

import com.threerings.jme.util.SpatialVisitor;

/**
 * Base class for controllers that affect a model's textures.
 */
public abstract class TextureController extends ModelController
{
    @Override
    public void resolveTextures (TextureProvider tprov)
    {
        // reinitialize the cloned textures if we re-resolve
        super.resolveTextures(tprov);
        _textures = null;
    }
    
    // documentation inherited
    public void update (float time)
    {
        // initialize the textures before the first update
        if (_textures == null) {
            initTextures();
        }
    }
    
    /**
     * Performs any per-instance initialization required for textures.
     */
    protected void initTextures ()
    {
        // find and clone all textures under the target
        final HashMap<Texture, Texture> clones = Maps.newHashMap();
        new SpatialVisitor<ModelMesh>(ModelMesh.class) {
            protected void visit (ModelMesh mesh) {
                TextureState otstate = (TextureState)mesh.getRenderState(RenderState.RS_TEXTURE);
                if (otstate == null) {
                    return;
                }
                TextureState ntstate =
                    DisplaySystem.getDisplaySystem().getRenderer().createTextureState();
                for (int ii = 0, nn = otstate.getNumberOfSetTextures(); ii < nn; ii++) {
                    Texture tex = otstate.getTexture(ii), ctex = clones.get(tex);
                    if (ctex == null) {
                        if (tex.getTextureId() == 0) {
                            otstate.apply(); // load before cloning
                        }
                        clones.put(tex, ctex = tex.createSimpleClone());
                    }
                    ntstate.setTexture(ctex, ii);
                }
                mesh.setRenderState(ntstate);
            }
        }.traverse(_target);
        _target.updateRenderState();
        
        // remember them for updates
        _textures = clones.values().toArray(new Texture[0]);
    }
    
    /** The cloned textures to manipulate. */
    protected transient Texture[] _textures;
    
    private static final long serialVersionUID = 1;
}
