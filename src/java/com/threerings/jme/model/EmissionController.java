//
// $Id$
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
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.util.Properties;

import com.jme.scene.Controller;
import com.jme.scene.Spatial;

/**
 * A model controller whose target represents an emitter.
 */
public abstract class EmissionController extends ModelController
{
    @Override // documentation inherited
    public void configure (Properties props, Spatial target)
    {
        super.configure(props, target);
        _hideTarget = Boolean.parseBoolean(
            props.getProperty("hide_target", "true"));
    }
    
    @Override // documentation inherited
    public void init (Model model)
    {
        super.init(model);
        if (_hideTarget) {
            _target.setCullMode(Spatial.CULL_ALWAYS);
            if (_target instanceof ModelNode) {
                // make sure the node isn't turned back on by an
                // animation
                ((ModelNode)_target).setForceCull(true);
            }
        }
    }
    
    @Override // documentation inherited
    public Controller putClone (
        Controller store, Model.CloneCreator properties)
    {
        if (store == null) {
            return null;
        }
        EmissionController estore = (EmissionController)store;
        super.putClone(estore, properties);
        estore._hideTarget = _hideTarget;
        return estore;
    }
    
    @Override // documentation inherited
    public void writeExternal (ObjectOutput out)
        throws IOException
    {
        super.writeExternal(out);
        out.writeBoolean(_hideTarget);
    }
    
    @Override // documentation inherited
    public void readExternal (ObjectInput in)
        throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        _hideTarget = in.readBoolean();
    }
    
    /** Whether or not the target should be hidden from view. */
    protected boolean _hideTarget;
    
    private static final long serialVersionUID = 1;
}
