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

import com.jme.math.Vector3f;
import com.jme.scene.Controller;
import com.jme.scene.Spatial;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.OutputCapsule;

import com.threerings.jme.util.JmeUtil;

/**
 * A procedural animation that moves a node along a straight line at a constant velocity (then
 * either repeats or moves it in the other direction).
 */
public class Translator extends ModelController
{
    @Override
    public void configure (Properties props, Spatial target)
    {
        super.configure(props, target);
        _from = JmeUtil.parseVector3f(props.getProperty("from", "0, 0, 0"));
        _to = JmeUtil.parseVector3f(props.getProperty("to", "10, 0, 0"));
        _duration = Float.parseFloat(props.getProperty("duration", "1"));
        _repeatType = JmeUtil.parseRepeatType(props.getProperty("repeat_type"),
            Controller.RT_WRAP);
    }
    
    // documentation inherited
    public void update (float time)
    {
        if (!isActive()) {
            return;
        }
        _elapsed += (time / _duration);
        float alpha;
        if (_repeatType == Controller.RT_CLAMP) {
            alpha = Math.min(_elapsed, 1f);
        } else if (_repeatType == Controller.RT_WRAP) {
            alpha = (_elapsed - (int)_elapsed);
        } else { // _repeatType == Controller.RT_CYCLE
            int ipart = (int)_elapsed;
            float fpart = (_elapsed - ipart);
            alpha = (ipart % 2 == 0) ? fpart : (1f - fpart);
        }
        _target.getLocalTranslation().interpolate(_from, _to, alpha);
    }
    
    @Override
    public Controller putClone (
        Controller store, Model.CloneCreator properties)
    {
        Translator tstore;
        if (store == null) {
            tstore = new Translator();
        } else {
            tstore = (Translator)store;
        }
        super.putClone(tstore, properties);
        tstore._from = _from;
        tstore._to = _to;
        tstore._duration = _duration;
        tstore._repeatType = _repeatType;
        return tstore;
    }
    
    @Override
    public void read (JMEImporter im)
        throws IOException
    {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        _from = (Vector3f)capsule.readSavable("from", null);
        _to = (Vector3f)capsule.readSavable("to", null);
        _duration = capsule.readFloat("duration", 1f);
        _repeatType = capsule.readInt("repeatType", Controller.RT_WRAP);
    }
    
    @Override
    public void write (JMEExporter ex)
        throws IOException
    {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(_from, "from", null);
        capsule.write(_to, "to", null);
        capsule.write(_duration, "duration", 1f);
        capsule.write(_repeatType, "repeatType", Controller.RT_WRAP);
    }
    
    /** The beginning and end of the path. */
    protected Vector3f _from, _to;
    
    /** The duration of the path. */
    protected float _duration;
    
    /** What to do after reaching the destination. */
    protected int _repeatType;
    
    /** The accumulated amount of time elapsed. */
    protected transient float _elapsed;
    
    private static final long serialVersionUID = 1;
}
