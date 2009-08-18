//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2007 Three Rings Design, Inc., All Rights Reserved
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


package com.threerings.flex.menuClasses {

import flash.display.DisplayObject;

import mx.controls.Menu;
import mx.controls.menuClasses.IMenuDataDescriptor;
import mx.controls.menuClasses.MenuItemRenderer;

public class CommandMenuItemRenderer extends MenuItemRenderer
{
    public function CommandMenuItemRenderer ()
    {
        super();
    }

    override protected function commitProperties () :void
    {
        super.commitProperties();
        // Note: a menu with no items will have a null listData

        if ((icon == null) && (listData is CommandListData)) {
            icon = CommandListData(listData).iconObject;
            if (icon != null) {
                addChild(DisplayObject(icon));
            }
        }

        if (label.visible && (listData != null)) {
            var dataDescriptor :IMenuDataDescriptor = Menu(listData.owner).dataDescriptor;
            var typeVal :String = dataDescriptor.getType(data);
            if (typeVal == "title") {
                dataDescriptor.setEnabled(data, false);
                label.enabled = false;
                label.styleName = "menuTitle";
                invalidateDisplayList();
            }
        }
    }
}
}
