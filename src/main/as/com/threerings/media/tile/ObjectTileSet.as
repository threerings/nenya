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

import com.threerings.util.ArrayUtil;
import com.threerings.util.StringUtil;

/**
 * The object tileset supports the specification of object information for object tiles in
 * addition to all of the features of the swiss army tileset.
 *
 * @see ObjectTile
 */
public class ObjectTileSet extends SwissArmyTileSet
    implements RecolorableTileSet
{
    /** A constraint prefix indicating that the object must have empty space in
     * the suffixed direction (N, E, S, or W). */
    public static const SPACE :String = "SPACE_";

    /** A constraint indicating that the object is a surface (e.g., table). */
    public static const SURFACE :String = "SURFACE";

    /** A constraint indicating that the object must be placed on a surface. */
    public static const ON_SURFACE :String = "ON_SURFACE";

    /** A constraint prefix indicating that the object is a wall facing the
     * suffixed direction (N, E, S, or W). */
    public static const WALL :String = "WALL_";

    /** A constraint prefix indicating that the object must be placed on a
     * wall facing the suffixed direction (N, E, S, or W). */
    public static const ON_WALL :String = "ON_WALL_";

    /** A constraint prefix indicating that the object must be attached to a
     * wall facing the suffixed direction (N, E, S, or W). */
    public static const ATTACH :String = "ATTACH_";

    /** The low suffix for walls and attachments. Low attachments can be placed
     * on low or normal walls; normal attachments can only be placed on normal
     * walls. */
    public static const LOW :String = "_LOW";

    /**
     * Sets the widths (in unit tile count) of the objects in this tileset. This must be
     * accompanied by a call to {@link #setObjectHeights}.
     */
    public function setObjectWidths (objectWidths :Array) :void
    {
        _owidths = objectWidths;
    }

    /**
     * Sets the heights (in unit tile count) of the objects in this tileset. This must be
     * accompanied by a call to {@link #setObjectWidths}.
     */
    public function setObjectHeights (objectHeights :Array) :void
    {
        _oheights = objectHeights;
    }

    /**
     * Sets the x offset in pixels to the image origin.
     */
    public function setXOrigins (xorigins :Array) :void
    {
        _xorigins = xorigins;
    }

    /**
     * Sets the y offset in pixels to the image origin.
     */
    public function setYOrigins (yorigins :Array) :void
    {
        _yorigins = yorigins;
    }

    /**
     * Sets the default render priorities for our object tiles.
     */
    public function setPriorities (priorities :Array) :void
    {
        _priorities = priorities;
    }

    /**
     * Provides a set of colorization classes that apply to objects in this tileset.
     */
    public function setColorizations (zations :Array) :void
    {
        _ozations = zations;
    }

    /**
     * Sets the x offset to the "spots" associated with our object tiles.
     */
    public function setXSpots (xspots :Array) :void
    {
        _xspots = xspots;
    }

    /**
     * Sets the y offset to the "spots" associated with our object tiles.
     */
    public function setYSpots (yspots :Array) :void
    {
        _yspots = yspots;
    }

    /**
     * Sets the orientation of the "spots" associated with our object
     * tiles.
     */
    public function setSpotOrients (sorients :Array) :void
    {
        _sorients = sorients;
    }

    /**
     * Sets the lists of constraints associated with our object tiles.
     */
    public function setConstraints (constraints :Array) :void
    {
        _constraints = constraints;
    }

    /**
     * Returns the x coordinate of the spot associated with the specified tile index.
     */
    public function getXSpot (tileIdx :int) :int
    {
        return (_xspots == null) ? 0 : _xspots[tileIdx];
    }

    /**
     * Returns the y coordinate of the spot associated with the specified tile index.
     */
    public function getYSpot (tileIdx :int) :int
    {
        return (_yspots == null) ? 0 : _yspots[tileIdx];
    }

    /**
     * Returns the orientation of the spot associated with the specified tile index.
     */
    public function getSpotOrient (tileIdx :int) :int
    {
        return (_sorients == null) ? 0 : _sorients[tileIdx];
    }

    /**
     * Returns the list of constraints associated with the specified tile index, or
     * <code>null</code> if the index has no constraints.
     */
    public function getConstraints (tileIdx :int) :Array
    {
        return (_constraints == null) ? null : _constraints[tileIdx];
    }

    /**
     * Checks whether the tile at the specified index has the given constraint.
     */
    public function hasConstraint (tileIdx :int, constraint :String) :Boolean
    {
        return (_constraints == null) ? false :
            ArrayUtil.contains(_constraints[tileIdx], constraint);
    }

    // documentation inherited from interface RecolorableTileSet
    public function getColorizations () :Array
    {
        return _ozations;
    }

    override protected function toStringBuf (buf :String) :String
    {
        buf = super.toStringBuf(buf);
        buf = buf.concat(", owidths=", StringUtil.toString(_owidths));
        buf = buf.concat(", oheights=", StringUtil.toString(_oheights));
        buf = buf.concat(", xorigins=", StringUtil.toString(_xorigins));
        buf = buf.concat(", yorigins=", StringUtil.toString(_yorigins));
        buf = buf.concat(", prios=", StringUtil.toString(_priorities));
        buf = buf.concat(", zations=", StringUtil.toString(_ozations));
        buf = buf.concat(", xspots=", StringUtil.toString(_xspots));
        buf = buf.concat(", yspots=", StringUtil.toString(_yspots));
        buf = buf.concat(", sorients=", StringUtil.toString(_sorients));
        buf = buf.concat(", constraints=", StringUtil.toString(_constraints));
        return buf;
    }

    override protected function getTileColorizations (tileIndex :int, rizer :Colorizer) :Array
    {
        var zations :Array = null;
        if (rizer != null && _ozations != null) {
            zations = new Array(_ozations.length);
            for (var ii :int = 0; ii < _ozations.length; ii++) {
                zations[ii] = rizer.getColorization(ii, _ozations[ii]);
            }
        }
        return zations;
    }

    override protected function createTile () :Tile
    {
        return new ObjectTile();
    }

    override protected function initTile (tile :Tile, tileIndex :int, zations :Array) :void
    {
        super.initTile(tile, tileIndex, zations);

        var otile :ObjectTile = ObjectTile(tile);
        if (_owidths != null) {
            otile.setBase(_owidths[tileIndex], _oheights[tileIndex]);
        }
        if (_xorigins != null) {
            otile.setOrigin(_xorigins[tileIndex], _yorigins[tileIndex]);
        }
        if (_priorities != null) {
            otile.setPriority(_priorities[tileIndex]);
        }
        if (_xspots != null) {
            otile.setSpot(_xspots[tileIndex], _yspots[tileIndex], _sorients[tileIndex]);
        }
        if (_constraints != null) {
            otile.setConstraints(_constraints[tileIndex]);
        }
    }

    public static function fromXml (xml :XML) :SwissArmyTileSet
    {
        var set :ObjectTileSet = new ObjectTileSet();
        set.populateFromXml(xml);
        return set;
    }

    override protected function populateFromXml (xml :XML) :void
    {
        super.populateFromXml(xml);
        _owidths = toIntArray(xml.objectWidths);
        _oheights = toIntArray(xml.objectHeights);
        _xorigins = toIntArray(xml.xOrigins);
        _yorigins = toIntArray(xml.yOrigins);
        _priorities = toIntArray(xml.priorities);
        _ozations = toStrArray(xml.zations);
        _xspots = toIntArray(xml.xSpots);
        _yspots = toIntArray(xml.ySpots);
        _sorients = toIntArray(xml.sorients);
        var constraintStrArr :Array = toStrArray(xml.constraints);
        if (constraintStrArr != null) {
            _constraints =
                constraintStrArr.map(function(element :*, index :int, arr :Array) :Array {
                        return element.split("|");
                });
        }
    }

    override public function populateClone (clone :TileSet) :void
    {
        super.populateClone(clone);
        var oClone :ObjectTileSet = ObjectTileSet(clone);
        oClone._owidths = _owidths == null ? null : ArrayUtil.copyOf(_owidths);
        oClone._oheights = _oheights == null ? null : ArrayUtil.copyOf(_oheights);
        oClone._xorigins = _xorigins == null ? null : ArrayUtil.copyOf(_xorigins);
        oClone._yorigins = _yorigins == null ? null : ArrayUtil.copyOf(_yorigins);
        oClone._priorities = _priorities == null ? null : ArrayUtil.copyOf(_priorities);
        oClone._ozations = _ozations == null ? null : ArrayUtil.copyOf(_ozations);
        oClone._xspots = _xspots == null ? null : ArrayUtil.copyOf(_xspots);
        oClone._yspots = _yspots == null ? null : ArrayUtil.copyOf(_yspots);
        oClone._sorients = _sorients == null ? null : ArrayUtil.copyOf(_sorients);
        oClone._constraints = _constraints == null ? null : ArrayUtil.copyOf(_constraints);
    }

    override public function createClone () :TileSet
    {
        return new ObjectTileSet();
    }

    /** The width (in tile units) of our object tiles. */
    protected var _owidths :Array;

    /** The height (in tile units) of our object tiles. */
    protected var _oheights :Array;

    /** The x offset in pixels to the origin of the tile images. */
    protected var _xorigins :Array;

    /** The y offset in pixels to the origin of the tile images. */
    protected var _yorigins :Array;

    /** The default render priorities of our objects. */
    protected var _priorities :Array;

    /** Colorization classes that apply to our objects. */
    protected var _ozations :Array;

    /** The x offset to the "spots" associated with our tiles. */
    protected var _xspots :Array;

    /** The y offset to the "spots" associated with our tiles. */
    protected var _yspots :Array;

    /** The orientation of the "spots" associated with our tiles. */
    protected var _sorients :Array;

    /** Lists of constraints associated with our tiles. */
    protected var _constraints :Array;
}
}