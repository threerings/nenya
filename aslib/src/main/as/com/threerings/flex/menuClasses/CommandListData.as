//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
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

package com.threerings.flex.menuClasses {

import mx.core.IFlexDisplayObject;
import mx.core.IUIComponent;

import mx.controls.menuClasses.MenuListData;

public class CommandListData extends MenuListData
{
    /** An already-instantiated icon, to use if the icon property is null. */
    public var iconObject :IFlexDisplayObject;

    public function CommandListData (text :String, icon :Class, iconObject :IFlexDisplayObject,
        labelField :String, uid :String, owner :IUIComponent,
        rowIndex :int = 0, columnIndex :int = 0)
    {
        super(text, icon, labelField, uid, owner, rowIndex, columnIndex);
        this.iconObject = iconObject;
    }
}
}
