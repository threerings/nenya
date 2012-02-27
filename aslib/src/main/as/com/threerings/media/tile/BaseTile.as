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

package com.threerings.media.tile {

public class BaseTile extends Tile
{
    /**
     * Returns whether or not this tile can be walked upon by character
     * sprites.
     */
    public function isPassable () :Boolean
    {
        return _passable;
    }

    /**
     * Configures this base tile as passable or impassable.
     */
    public function setPassable (passable :Boolean) :void
    {
        _passable = passable;
    }

    /**
     * Returns the x offset into the tile image of the origin (which will be aligned with the
     * bottom center of the origin tile) or <code>Integer.MIN_VALUE</code> if the origin is not
     * explicitly specified and should be computed from the image size and tile footprint.
     */
    public override function getOriginX () :int
    {
        return getWidth()/2;
    }

    /**
     * Returns the y offset into the tile image of the origin (which will be aligned with the
     * bottom center of the origin tile) or <code>Integer.MIN_VALUE</code> if the origin is not
     * explicitly specified and should be computed from the image size and tile footprint.
     */
    public override function getOriginY () :int
    {
        return getHeight();
    }


    /**
     * Returns the object footprint width in tile units.
     */
    public override function getBaseWidth () :int
    {
        return 1;
    }

    /**
     * Returns the object footprint height in tile units.
     */
    public override function getBaseHeight () :int
    {
        return 1;
    }

    /** Whether the tile is passable. */
    protected var _passable :Boolean = true;
}
}