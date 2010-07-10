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

package com.threerings.media.tile {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.DisplayObject;

public class Tile
{
    /** The key associated with this tile. */
    public var key :Tile_Key;

    /**
     * Configures this tile with its tile image.
     */
    public function setImage (image :DisplayObject) :void
    {
        _image = image;

        // Notify them all and clear our list.
        for each (var func :Function in _notifyOnLoad) {
            func(this);
        }
    }

    public function getImage () :DisplayObject
    {
        if (_image is Bitmap) {
            // TODO - handle this more consistently...
            return new Bitmap(Bitmap(_image).bitmapData);
        } else {
            return _image;
        }
    }

    /**
     * Returns the width of this tile.
     */
    public function getWidth () :int
    {
        return _image.width;
    }

    /**
     * Returns the height of this tile.
     */
    public function getHeight () :int
    {
        return _image.height;
    }

    /**
     * Returns true if the specified coordinates within this tile contains
     * a non-transparent pixel.
     */
    public function hitTest (x :int, y :int) :Boolean
    {
        return _image.hitTestPoint(x, y, true);
    }

    public function toString () :String
    {
        return "[" + toStringBuf(new String()) + "]";
    }

    /**
     * This should be overridden by derived classes (which should be sure
     * to call <code>super.toString()</code>) to append the derived class
     * specific tile information to the string buffer.
     */
    protected function toStringBuf (buf :String) :String
    {
        if (_image == null) {
            return buf.concat("null-image");
        } else {
            return buf.concat(_image.width, "x", _image.height);
        }
    }

    public function notifyOnLoad (func :Function) :void
    {
        _notifyOnLoad.push(func);
    }

    /**
     * Returns the x offset into the tile image of the origin (which will be aligned with the
     * bottom center of the origin tile) or <code>Integer.MIN_VALUE</code> if the origin is not
     * explicitly specified and should be computed from the image size and tile footprint.
     */
    public function getOriginX () :int
    {
        throw new Error("abstract");
    }

    /**
     * Returns the y offset into the tile image of the origin (which will be aligned with the
     * bottom center of the origin tile) or <code>Integer.MIN_VALUE</code> if the origin is not
     * explicitly specified and should be computed from the image size and tile footprint.
     */
    public function getOriginY () :int
    {
        throw new Error("abstract")
    }

    /**
     * Returns the object footprint width in tile units.
     */
    public function getBaseWidth () :int
    {
        throw new Error("abstract");
    }

    /**
     * Returns the object footprint height in tile units.
     */
    public function getBaseHeight () :int
    {
        throw new Error("abstract");
    }

    /** Our tileset image. */
    protected var _image :DisplayObject;

    /** Everyone who cares when we're loaded. */
    protected var _notifyOnLoad :Array = [];
}
}
