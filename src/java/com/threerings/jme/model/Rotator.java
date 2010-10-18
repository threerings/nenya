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

import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Controller;
import com.jme.scene.Spatial;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.OutputCapsule;

import com.threerings.jme.util.JmeUtil;

/**
 * A procedural animation that rotates a node around at a constant angular
 * velocity.
 */
public class Rotator extends ModelController
{
    @Override
    public void configure (Properties props, Spatial target)
    {
        super.configure(props, target);
        _axis = JmeUtil.parseAxis(props.getProperty("axis", "x"));
        _velocity = Float.parseFloat(props.getProperty("velocity", "3.14"));
    }
    
    // documentation inherited
    public void update (float time)
    {
        if (!isActive()) {
            return;
        }
        _rot.fromAngleNormalAxis(time * _velocity, _axis);
        _target.getLocalRotation().multLocal(_rot);
    }
    
    @Override
    public Controller putClone (
        Controller store, Model.CloneCreator properties)
    {
        Rotator rstore;
        if (store == null) {
            rstore = new Rotator();
        } else {
            rstore = (Rotator)store;
        }
        super.putClone(rstore, properties);
        rstore._axis = _axis;
        rstore._velocity = _velocity;
        return rstore;
    }
    
    @Override
    public void read (JMEImporter im)
        throws IOException
    {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        _axis = (Vector3f)capsule.readSavable("axis", null);
        _velocity = capsule.readFloat("velocity", 0f);
    }
    
    @Override
    public void write (JMEExporter ex)
        throws IOException
    {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(_axis, "axis", null);
        capsule.write(_velocity, "velocity", 0f);
    }
    
    /** The axis about which to rotate. */
    protected Vector3f _axis;
    
    /** The velocity at which to rotate in radians per second. */
    protected float _velocity;
    
    /** A temporary quaternion. */
    protected Quaternion _rot = new Quaternion();
    
    private static final long serialVersionUID = 1;
}
