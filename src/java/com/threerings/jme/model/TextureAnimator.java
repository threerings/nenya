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
import com.jme.math.Vector3f;
import com.jme.scene.Controller;
import com.jme.scene.Spatial;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.OutputCapsule;

import com.threerings.jme.util.JmeUtil;
import com.threerings.jme.util.JmeUtil.FrameState;

/**
 * Animates a model's textures by flipping between different parts.
 */
public class TextureAnimator extends TextureController
{
    @Override
    public void configure (Properties props, Spatial target)
    {
        super.configure(props, target);
        _frameWidth = Float.valueOf(props.getProperty("frame_width", "0.5"));
        _frameHeight = Float.valueOf(props.getProperty("frame_height", "0.5"));
        _frameCount = Integer.valueOf(props.getProperty("frame_count", "4"));
        _frameRate = Float.valueOf(props.getProperty("frame_rate", "1"));
        _repeatType = JmeUtil.parseRepeatType(props.getProperty("repeat_type"),
            Controller.RT_WRAP);
    }
    
    // documentation inherited
    public void update (float time)
    {
        super.update(time);
        if (!isActive()) {
            return;
        }
        _fstate.update(time, _frameRate, _frameCount, _repeatType);
        _translation.set(
            (_fstate.idx % _hframes) * _frameWidth,
            -(_fstate.idx / _hframes) * _frameHeight,
            0f);
    }
    
    @Override
    public Controller putClone (
        Controller store, Model.CloneCreator properties)
    {
        TextureAnimator tstore;
        if (store == null) {
            tstore = new TextureAnimator();
        } else {
            tstore = (TextureAnimator)store;
        }
        super.putClone(tstore, properties);
        tstore._frameWidth = _frameWidth;
        tstore._frameHeight = _frameHeight;
        tstore._frameCount = _frameCount;
        tstore._frameRate = _frameRate;
        tstore._repeatType = _repeatType;
        return tstore;
    }
    
    @Override
    public void read (JMEImporter im)
        throws IOException
    {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        _frameWidth = capsule.readFloat("frameWidth", 0.5f);
        _frameHeight = capsule.readFloat("frameHeight", 0.5f);
        _frameCount = capsule.readInt("frameCount", 4);
        _frameRate = capsule.readFloat("frameRate", 1f);
        _repeatType = capsule.readInt("repeatType", Controller.RT_WRAP);
    }
    
    @Override
    public void write (JMEExporter ex)
        throws IOException
    {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(_frameWidth, "frameWidth", 0.5f);
        capsule.write(_frameHeight, "frameHeight", 0.5f);
        capsule.write(_frameCount, "frameCount", 4);
        capsule.write(_frameRate, "frameRate", 1f);
        capsule.write(_repeatType, "repeatType", Controller.RT_WRAP);
    }
    
    @Override
    protected void initTextures ()
    {
        super.initTextures();
        
        // compute derived values
        _hframes = (int)Math.floor(1f / _frameWidth);
        _vframes = (int)Math.floor(1f / _frameHeight);
        
        // use the same translation vector for all textures
        _translation = new Vector3f();
        for (Texture texture : _textures) {
            texture.setTranslation(_translation);
        }
    }
    
    /** The width of the frames in texture units. */
    protected float _frameWidth;
    
    /** The height of the frames in texture units. */
    protected float _frameHeight;
    
    /** The number of frames in the texture. */
    protected int _frameCount;
 
    /** The rate at which to display the frames (frames per second). */
    protected float _frameRate;
    
    /** The repeat type (one of the constants in {@link Controller}). */
    protected int _repeatType;
    
    /** The number of frames on the horizontal and vertical axes. */
    protected transient int _hframes, _vframes;
    
    /** The animation position. */
    protected transient FrameState _fstate = new FrameState();
    
    /** The shared texture translation. */
    protected transient Vector3f _translation;
    
    private static final long serialVersionUID = 1;
}
