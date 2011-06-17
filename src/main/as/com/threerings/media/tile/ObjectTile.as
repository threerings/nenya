//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2011 Three Rings Design, Inc., All Rights Reserved
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

import flash.geom.Point;

import com.threerings.media.tile.Tile;
import com.threerings.util.Arrays;
import com.threerings.util.DirectionUtil;
import com.threerings.util.Integer;
import com.threerings.util.StringUtil;

public class ObjectTile extends Tile
{
    /**
     * Returns the object footprint width in tile units.
     */
    public override function getBaseWidth () :int
    {
        return _base.x;
    }

    /**
     * Returns the object footprint height in tile units.
     */
    public override function getBaseHeight () :int
    {
        return _base.y;
    }

    /**
     * Sets the object footprint in tile units.
     */
    public function setBase (width :int, height :int) :void
    {
        _base.x = width;
        _base.y = height;
    }

    /**
     * Returns the x offset into the tile image of the origin (which will be aligned with the
     * bottom center of the origin tile) or <code>Integer.MIN_VALUE</code> if the origin is not
     * explicitly specified and should be computed from the image size and tile footprint.
     */
    public override function getOriginX () :int
    {
        return _origin.x;
    }

    /**
     * Returns the y offset into the tile image of the origin (which will be aligned with the
     * bottom center of the origin tile) or <code>Integer.MIN_VALUE</code> if the origin is not
     * explicitly specified and should be computed from the image size and tile footprint.
     */
    public override function getOriginY () :int
    {
        return _origin.y;
    }

    /**
     * Sets the offset in pixels from the origin of the tile image to the origin of the object.
     * The object will be rendered such that its origin is at the bottom center of its origin
     * tile. If no origin is specified, the bottom of the image is aligned with the bottom of the
     * origin tile and the left side of the image is aligned with the left edge of the left-most
     * base tile.
     */
    public function setOrigin (x :int, y :int) :void
    {
        _origin.x = x;
        _origin.y = y;
    }

    /**
     * Returns this object tile's default render priority.
     */
    public function getPriority () :int
    {
        return _priority;
    }

    /**
     * Sets this object tile's default render priority.
     */
    public function setPriority (priority :int) :void
    {
        _priority = priority;
    }

    /**
     * Configures the "spot" associated with this object.
     */
    public function setSpot (x :int, y :int, orient :int) :void
    {
        _spot = new Point(x, y);
        _sorient = orient;
    }

    /**
     * Returns true if this object has a spot.
     */
    public function hasSpot () :Boolean
    {
        return (_spot != null);
    }

    /**
     * Returns the x-coordinate of the "spot" associated with this object.
     */
    public function getSpotX () :int
    {
        return (_spot == null) ? 0 : _spot.x;
    }

    /**
     * Returns the x-coordinate of the "spot" associated with this object.
     */
    public function getSpotY () :int
    {
        return (_spot == null) ? 0 : _spot.y;
    }

    /**
     * Returns the orientation of the "spot" associated with this object.
     */
    public function getSpotOrient () :int
    {
        return _sorient;
    }

    /**
     * Returns the list of constraints associated with this object, or <code>null</code> if the
     * object has no constraints.
     */
    public function getConstraints () :Array
    {
        return _constraints;
    }

    /**
     * Checks whether this object has the given constraint.
     */
    public function hasConstraint (constraint :String) :Boolean
    {
        return (_constraints == null) ? false :
            Arrays.contains(_constraints, constraint);
    }

    /**
     * Configures this object's constraints.
     */
    public function setConstraints (constraints :Array) :void
    {
        _constraints = constraints;
    }

    protected override function toStringBuf (buf :String) :String
    {
        buf = super.toStringBuf(buf);
        buf.concat(", base=", StringUtil.toString(_base));
        buf.concat(", origin=", StringUtil.toString(_origin));
        buf.concat(", priority=", _priority);
        if (_spot != null) {
            buf.concat(", spot=", StringUtil.toString(_spot));
            buf.concat(", sorient=");
            buf.concat(DirectionUtil.toShortString(_sorient));
        }
        if (_constraints != null) {
            buf.concat(", constraints=", StringUtil.toString(_constraints));
        }

        return buf;
    }

    /** The object footprint width in unit tile units. */
    protected var _base :Point = new Point(1, 1);

    /** The offset from the origin of the tile image to the object's origin or MIN_VALUE if the
     * origin should be calculated based on the footprint. */
    protected var _origin :Point = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);

    /** This object tile's default render priority. */
    protected var _priority :int;

    /** The coordinates of the "spot" associated with this object. */
    protected var _spot :Point;

    /** The orientation of the "spot" associated with this object. */
    protected var _sorient :int;

    /** The list of constraints associated with this object. */
    protected var _constraints :Array;
}
}