//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
// http://www.threerings.net/code/nenya/
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

package com.threerings.flex {

import mx.core.ContainerCreationPolicy;

import mx.core.UIComponent;

import mx.containers.TabNavigator;
import mx.containers.VBox;

/**
 * A tab navigator that is specially set up for lazy-creating the
 * content in each tab.
 */
public class LazyTabNavigator extends TabNavigator
{
    public function LazyTabNavigator ()
    {
        super();
    }

    /**
     * Add a tab to the container. The creation function takes no args and
     * returns a UIComponent.
     */
    public function addTab (label :String, creation :Function) :void
    {
        addTabAt(label, creation, numChildren);
    }

    /**
     * Add a tab to the container at the specified index.
     * The creation function takes no args and returns a UIComponent.
     */
    public function addTabAt (
        label :String, creation :Function, index :int) :void
    {
        var box :LazyContainer = new LazyContainer(creation);
        box.label = label;
        addChildAt(box, index);
    }
}
}
