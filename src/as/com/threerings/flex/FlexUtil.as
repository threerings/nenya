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

package com.threerings.flex {

import mx.controls.Label;
import mx.controls.Spacer;

import mx.core.UIComponent;

/**
 * Flex-related utility methods.
 */
public class FlexUtil
{
    /**
     * How hard would it have been for them to make Label accept an optional text argument?
     */
    public static function createLabel (text :String, style :String = null) :Label
    {
        var label :Label = new Label();
        label.text = text;
        if (style != null) {
            label.styleName = style;
        }
        return label;
    }

    /**
     * How hard would it have been for them make Spacer accept two optional arguments?
     */
    public static function createSpacer (width :int = 0, height :int = 0) :Spacer
    {
        var spacer :Spacer = new Spacer();
        spacer.width = width;
        spacer.height = height;
        return spacer;
    }

    /**
     * In flex the 'visible' property controls visibility separate from whether
     * the component takes up space in the layout, which is controlled by 'includeInLayout'.
     * We usually want to set them together, so this does that for us.
     */
    public static function setVisible (component :UIComponent, visible :Boolean) :void
    {
        component.visible = visible;
        component.includeInLayout = visible;
    }
}
}
