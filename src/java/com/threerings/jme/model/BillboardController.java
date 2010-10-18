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

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.scene.Controller;
import com.jme.scene.Spatial;
import com.jme.system.DisplaySystem;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.OutputCapsule;

/**
 * Orients its target towards the camera plane.
 */
public class BillboardController extends ModelController
{
    /** Determines how the billboard is rotated. */
    public enum Alignment {
        DIR_X_POS_Z(false, true),
        DIR_X_DIR_Z(false, false),
        POS_X_POS_Z(true, true),
        DIR_Z(false),
        POS_Z(true);
        
        /** Determines whether rotation is limited to the Z axis. */
        public boolean isAxial () {
            return _axial;
        }
        
        /** Determines whether to tilt in the direction of the vector from the eyepoint to the
         * target's origin (the eye vector) as opposed to the camera direction. */
        public boolean isEyeRelativeX () {
            return _eyeRelativeX;
        }
        
        /** Determines whether to swivel in the direction of the eye vector as opposed to the
         * camera direction. */
        public boolean isEyeRelativeZ () {
            return _eyeRelativeZ;
        }
        
        Alignment (boolean eyeRelativeX, boolean eyeRelativeZ) {
            _eyeRelativeX = eyeRelativeX;
            _eyeRelativeZ = eyeRelativeZ;
        }
        
        Alignment (boolean eyeRelativeZ) {
            _axial = true;
            _eyeRelativeZ = eyeRelativeZ;
        }
        
        protected boolean _axial, _eyeRelativeX, _eyeRelativeZ;
    };
    
    @Override
    public void configure (Properties props, Spatial target)
    {
        super.configure(props, target);
        _alignment = Enum.valueOf(Alignment.class,
            props.getProperty("alignment", "dir_x_pos_z").toUpperCase());
    }
    
    // documentation inherited
    public void update (float time)
    {
        Camera cam = DisplaySystem.getDisplaySystem().getRenderer().getCamera();
        if (!isActive() || cam == null) {
            return;
        }
        if (_alignment.isEyeRelativeZ()) {
            _target.getWorldTranslation().subtract(cam.getLocation(), _yvec);
        } else {
            _yvec.set(cam.getDirection());
        }
        _zvec.set(_alignment.isAxial() ? Vector3f.UNIT_Z : cam.getUp());
        if (_alignment.isEyeRelativeX()) {
            // use the forward vector as-is
            _yvec.normalizeLocal().cross(_zvec, _xvec);
            if (_xvec.length() < FastMath.FLT_EPSILON) {
                return;
            }
            _xvec.normalizeLocal().cross(_yvec, _zvec);
            
        } else {
            // project the forward vector onto the up plane
            _yvec.scaleAdd(-_zvec.dot(_yvec), _zvec, _yvec);
            if (_yvec.length() < FastMath.FLT_EPSILON) {
                return;
            }
            _yvec.normalizeLocal().cross(_zvec, _xvec);
        }
        // compute the world rotation with the axes and rotate into
        // parent's coordinate system
        Quaternion lrot = _target.getLocalRotation();
        lrot.fromAxes(_xvec, _yvec, _zvec);
        Spatial parent = _target.getParent();
        if (parent == null) {
            _rot.loadIdentity();
        } else {
            _rot.set(parent.getWorldRotation()).inverseLocal();
        }
        _rot.mult(lrot, lrot);
    }
    
    @Override
    public Controller putClone (
        Controller store, Model.CloneCreator properties)
    {
        BillboardController bstore;
        if (store == null) {
            bstore = new BillboardController();
        } else {
            bstore = (BillboardController)store;
        }
        super.putClone(bstore, properties);
        bstore._alignment = _alignment;
        return bstore;
    }
    
    @Override
    public void read (JMEImporter im)
        throws IOException
    {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        _alignment = Enum.valueOf(Alignment.class,
            capsule.readString("alignment", "DIR_X_POS_Z"));
    }
    
    @Override
    public void write (JMEExporter ex)
        throws IOException
    {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(_alignment.name(), "alignment", "DIR_X_POS_Z");
    }
    
    /** The alignment mode. */
    protected Alignment _alignment;
    
    /** A temporary quaternion. */
    protected Quaternion _rot = new Quaternion();
    
    /** Temporary axis vectors. */
    protected Vector3f _xvec = new Vector3f(), _yvec = new Vector3f(), _zvec = new Vector3f();
    
    private static final long serialVersionUID = 1;
}
