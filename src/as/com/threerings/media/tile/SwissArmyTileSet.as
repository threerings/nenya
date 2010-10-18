//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
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
import flash.geom.Rectangle;

import com.threerings.util.ArrayUtil;
import com.threerings.util.StringUtil;

/**
 * The swiss army tileset supports a diverse variety of tiles in the tileset image. Each row can
 * contain varying numbers of tiles and each row can have its own width and height. Tiles can be
 * separated from the edge of the tileset image by some border offset and can be separated from one
 * another by a gap distance.
 */
public class SwissArmyTileSet extends TileSet
{
    public override function getTileCount () :int
    {
        return _numTiles;
    }

    public override function computeTileBounds (tileIndex :int) :Rectangle
    {
        // find the row number containing the sought-after tile
        var ridx :int;
        var tcount :int;
        var ty :int;
        var tx :int;
        ridx = tcount = 0;

        // start tile image position at image start offset
        tx = _offsetPos.x;
        ty = _offsetPos.y;

        while ((tcount += _tileCounts[ridx]) < tileIndex + 1) {
            // increment tile image position by row height and gap distance
            ty += (_heights[ridx++] + _gapSize.y);
        }

        // determine the horizontal index of this tile in the row
        var xidx :int = tileIndex - (tcount - _tileCounts[ridx]);

        // final image x-position is based on tile width and gap distance
        tx += (xidx * (_widths[ridx] + _gapSize.x));

//         Log.info("Computed tile bounds [tileIndex=" + tileIndex +
//                  ", ridx=" + ridx + ", xidx=" + xidx + ", tx=" + tx + ", ty=" + ty + "].");

        // crop the tile-sized image chunk from the full image
        return new Rectangle(tx, ty, _widths[ridx], _heights[ridx]);
    }

    /**
     * Sets the tile counts which are the number of tiles in each row of the tileset image. Each
     * row can have an arbitrary number of tiles.
     */
    public function setTileCounts (tileCounts :Array) :void
    {
        _tileCounts = tileCounts;

        // compute our total tile count
        computeTileCount();
    }

    /**
     * Returns the tile count settings.
     */
    public function getTileCounts () :Array
    {
        return _tileCounts;
    }

    /**
     * Computes our total tile count from the individual counts for each row.
     */
    protected function computeTileCount () :void
    {
        // compute our number of tiles
        _numTiles = 0;
        for each (var count :int in _tileCounts) {
            _numTiles += count;
        }
    }

    /**
     * Sets the tile widths for each row. Each row can have tiles of a different width.
     */
    public function setWidths (widths :Array) :void
    {
        _widths = widths;
    }

    /**
     * Returns the width settings.
     */
    public function getWidths () :Array
    {
        return _widths;
    }

    /**
     * Sets the tile heights for each row. Each row can have tiles of a different height.
     */
    public function setHeights (heights :Array) :void
    {
        _heights = heights;
    }

    /**
     * Returns the height settings.
     */
    public function getHeights () :Array
    {
        return _heights;
    }

    /**
     * Sets the offset in pixels of the upper left corner of the first tile in the first row. If
     * the tileset image has a border, this can be set to account for it.
     */
    public function setOffsetPos (offsetPos :Point) :void
    {
        _offsetPos = offsetPos;
    }

    /**
     * Sets the size of the gap between tiles (in pixels). If the tiles have space between them,
     * this can be set to account for it.
     */
    public function setGapSize (gapSize :Point) :void
    {
        _gapSize = gapSize;
    }

    protected override function toStringBuf (buf :String) :String
    {
        buf = super.toStringBuf(buf);
        buf = buf.concat(", widths=", StringUtil.toString(_widths));
        buf = buf.concat(", heights=", StringUtil.toString(_heights));
        buf = buf.concat(", tileCounts=", StringUtil.toString(_tileCounts));
        buf = buf.concat(", offsetPos=", StringUtil.toString(_offsetPos));
        buf = buf.concat(", gapSize=", StringUtil.toString(_gapSize));
        return buf;
    }

    public static function fromXml (xml :XML) :SwissArmyTileSet
    {
        var set :SwissArmyTileSet = new SwissArmyTileSet();
        set.populateFromXml(xml);
        return set;
    }

    protected override function populateFromXml (xml :XML) :void
    {
        super.populateFromXml(xml);
        _tileCounts = toIntArray(xml.tileCounts);
        _widths = toIntArray(xml.widths);
        _heights = toIntArray(xml.heights);
        var offsetPosArr :Array = toIntArray(xml.offsetPos);
        if (offsetPosArr != null) {
            _offsetPos = new Point(offsetPosArr[0], offsetPosArr[1]);
        } else {
            _offsetPos = new Point(0, 0);
        }
        var gapSizeArr :Array = toIntArray(xml.gapSize);
        if (gapSizeArr != null) {
            _gapSize = new Point(gapSizeArr[0], gapSizeArr[1]);
        } else {
            _gapSize = new Point(0, 0);
        }
        computeTileCount();
    }

    protected static function toStrArray (str :String) :Array
    {
        if (str == null || str.length == 0) {
            return null;
        }

        return str.split(",").map(function(element :String, index :int, arr :Array) :String {
                return StringUtil.trim(element);
            });
    }

    protected static function toBoolArray (str :String) :Array
    {
        if (str == null || str.length == 0) {
            return null;
        }

        return str.split(",").map(function(element :*, index :int, arr :Array) :Boolean {
                return (StringUtil.trim(element) == "1");
        });
    }

    protected static function toIntArray (str :String) :Array
    {
        if (str == null || str.length == 0) {
            return null;
        }

        return str.split(",").map(function(element :*, index :int, arr :Array) :int {
            return int(StringUtil.trim(element));
        });
    }

    override public function populateClone (clone :TileSet) :void
    {
        super.populateClone(clone);
        var saClone :SwissArmyTileSet = SwissArmyTileSet(clone);
        saClone._tileCounts = _tileCounts == null ? null : ArrayUtil.copyOf(_tileCounts);
        saClone._numTiles = _numTiles;
        saClone._widths = _widths == null ? null : ArrayUtil.copyOf(_widths);
        saClone._heights = _heights == null ? null : ArrayUtil.copyOf(_heights);
        saClone._offsetPos = _offsetPos;
        saClone._gapSize = _gapSize;
    }

    override public function createClone () :TileSet
    {
        return new SwissArmyTileSet();
    }

    /** The number of tiles in each row. */
    protected var _tileCounts :Array;

    /** The number of tiles in the tileset. */
    protected var _numTiles :int;

    /** The width of the tiles in each row in pixels. */
    protected var _widths :Array;

    /** The height of the tiles in each row in pixels. */
    protected var _heights :Array;

    /** The offset distance (x, y) in pixels from the top-left of the image to the start of the
     * first tile image.  */
    protected var _offsetPos :Point = new Point();

    /** The distance (x, y) in pixels between each tile in each row horizontally, and between each
     * row of tiles vertically.  */
    protected var _gapSize :Point = new Point();
}
}