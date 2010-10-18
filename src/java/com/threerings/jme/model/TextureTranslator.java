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

import java.util.Properties;

import com.jme.image.Texture;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.scene.Controller;
import com.jme.scene.Spatial;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.OutputCapsule;

import com.samskivert.util.StringUtil;

import static com.threerings.jme.Log.log;

/**
 * A procedural animation that translates the model's textures at a constant velocity.
 */
public class TextureTranslator extends TextureController
{
    @Override
    public void configure (Properties props, Spatial target)
    {
        super.configure(props, target);
        String velstr = props.getProperty("velocity", "1, 0");
        float[] vel = StringUtil.parseFloatArray(velstr);
        if (vel != null && vel.length == 2) {
            _velocity = new Vector2f(vel[0], vel[1]);
        } else {
            log.warning("Invalid velocity [velocity=" + velstr + "].");
        }
    }
    
    // documentation inherited
    public void update (float time)
    {
        super.update(time);
        if (!isActive()) {
            return;
        }
        _translation.addLocal(_velocity.x * time, _velocity.y * time, 0f);
    }
    
    @Override
    public Controller putClone (
        Controller store, Model.CloneCreator properties)
    {
        TextureTranslator tstore;
        if (store == null) {
            tstore = new TextureTranslator();
        } else {
            tstore = (TextureTranslator)store;
        }
        super.putClone(tstore, properties);
        tstore._velocity = _velocity;
        return tstore;
    }
    
    @Override
    public void read (JMEImporter im)
        throws IOException
    {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        _velocity = (Vector2f)capsule.readSavable("velocity", null);
    }
    
    @Override
    public void write (JMEExporter ex)
        throws IOException
    {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(_velocity, "velocity", null);
    }
    
    @Override
    protected void initTextures ()
    {
        super.initTextures();
        
        // use the same translation vector for all textures
        _translation = new Vector3f();
        for (Texture texture : _textures) {
            texture.setTranslation(_translation);
        }
    }
    
    /** The velocity at which to translate the texture. */
    protected Vector2f _velocity;
    
    /** The shared translation vector. */
    protected transient Vector3f _translation;
    
    private static final long serialVersionUID = 1;
}
