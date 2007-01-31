//
// $Id: Rotator.java 119 2007-01-24 00:22:12Z dhoover $
//
// Narya library - tools for developing networked games
// Copyright (C) 2002-2005 Three Rings Design, Inc., All Rights Reserved
// http://www.threerings.net/code/narya/
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

import com.jme.image.Texture;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;

import com.threerings.jme.util.SpatialVisitor;

/**
 * Base class for controllers that affect a model's textures.
 */
public abstract class TextureController extends ModelController
{
    @Override // documentation inherited
    public void resolveTextures (TextureProvider tprov)
    {
        super.resolveTextures(tprov);
        
        // find and clone all textures under the target
        final HashMap<Texture, Texture> clones = new HashMap<Texture, Texture>();
        new SpatialVisitor<ModelMesh>(ModelMesh.class) {
            protected void visit (ModelMesh mesh) {
                TextureState tstate = (TextureState)mesh.getRenderState(RenderState.RS_TEXTURE);
                if (tstate == null) {
                    return;
                }
                for (int ii = 0, nn = tstate.getNumberOfSetTextures(); ii < nn; ii++) {
                    Texture tex = tstate.getTexture(ii), ctex = clones.get(tex);
                    if (ctex == null) {
                        if (tex.getTextureId() == 0) {
                            tstate.load(ii);
                        }
                        clones.put(tex, ctex = tex.createSimpleClone());
                    }
                    tstate.setTexture(ctex, ii);
                }
            }
        }.traverse(_target);
        
        // remember them for updates
        _textures = clones.values().toArray(new Texture[0]);
    }
    
    /** The cloned textures to manipulate. */
    protected transient Texture[] _textures;
    
    private static final long serialVersionUID = 1;
}
