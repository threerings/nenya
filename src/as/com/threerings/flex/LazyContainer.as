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

import flash.display.DisplayObjectContainer;

import mx.core.ContainerCreationPolicy;
import mx.core.UIComponent;
import mx.containers.VBox;

public class LazyContainer extends VBox
{
    /**
     * Create a lazy container that will not create its children
     * until needed.
     *
     * @param creation a function that takes no args and returns
     * a UIComponent to be added to the container.
     */
    public function LazyContainer (creation :Function)
    {
        _creation = creation;
        creationPolicy = ContainerCreationPolicy.NONE;
    }

    override public function createComponentsFromDescriptors (
        recurse :Boolean = true) :void
    {
        super.createComponentsFromDescriptors(recurse);

        if (_creation != null) {
            addChild(_creation() as UIComponent);
            _creation = null; // assist gc
        }
    }

    protected var _creation :Function;
}
}
