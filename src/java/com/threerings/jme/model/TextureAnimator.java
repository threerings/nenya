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

import com.threerings.jme.Log;
import com.threerings.jme.util.SpatialVisitor;

/**
 * Animates a model's textures by flipping between different parts.
 */
public class TextureAnimator extends TextureController
{
    @Override // documentation inherited
    public void configure (Properties props, Spatial target)
    {
        super.configure(props, target);
        _frameWidth = Float.valueOf(props.getProperty("frame_width", "0.5"));
        _frameHeight = Float.valueOf(props.getProperty("frame_height", "0.5"));
        _frameCount = Integer.valueOf(props.getProperty("frame_count", "4"));
        _frameRate = Float.valueOf(props.getProperty("frame_rate", "1"));
        String rtype = props.getProperty("repeat_type", "wrap");
        if (rtype.equals("clamp")) {
            _repeatType = Controller.RT_CLAMP;    
        } else if (rtype.equals("cycle")) {
            _repeatType = Controller.RT_CYCLE;
        } else if (rtype.equals("wrap")) {
            _repeatType = Controller.RT_WRAP;
        } else {
            Log.warning("Invalid repeat type [type=" + rtype + "].");
            _repeatType = Controller.RT_WRAP;
        }
    }
    
    @Override // documentation inherited
    public void resolveTextures (TextureProvider tprov)
    {
        super.resolveTextures(tprov);
        
        // compute derived values
        _hframes = (int)Math.floor(1f / _frameWidth);
        _vframes = (int)Math.floor(1f / _frameHeight);
        
        // use the same translation vector for all textures
        _translation = new Vector3f();
        for (Texture texture : _textures) {
            texture.setTranslation(_translation);
        }
    }
    
    // documentation inherited
    public void update (float time)
    {
        if (!isActive()) {
            return;
        }
        float spf = 1f / _frameRate;
        for (_faccum += time; _faccum >= spf; _faccum -= spf) {
            advanceFrame();
        }
        _translation.set(
            (_fidx % _hframes) * _frameWidth,
            (_vframes - 1 - (_fidx / _hframes)) * _frameHeight,
            0f);
    }
    
    @Override // documentation inherited
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
    
    @Override // documentation inherited
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
    
    @Override // documentation inherited
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
    
    /**
     * Advances by one frame.
     */
    protected void advanceFrame ()
    {
        if ((_fidx += _fdir) >= _frameCount) {
            if (_repeatType == Controller.RT_CLAMP) {
                _fidx = _frameCount - 1;
                _fdir = 0;
            } else if (_repeatType == Controller.RT_WRAP) {
                _fidx = 0;
            } else { // repeatType == Controller.RT_CYCLE
                _fidx = _frameCount - 2;
                _fdir = -1;
            }
        } else if (_fidx < 0) {
            _fidx = 1;
            _fdir = +1;
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
    
    /** The current animation frame and direction. */
    protected transient int _fidx, _fdir = +1;
    
    /** The time accumulated towards the next frame. */
    protected transient float _faccum;
    
    /** The shared texture translation. */
    protected transient Vector3f _translation;
    
    private static final long serialVersionUID = 1;
}
