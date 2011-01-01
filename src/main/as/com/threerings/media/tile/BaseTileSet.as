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

public class BaseTileSet extends SwissArmyTileSet
{
    /**
     * Sets the passability information for the tiles in this tileset.
     * Each entry in the array corresponds to the tile at that tile index.
     */
    public function setPassability (passable :Array) :void
    {
        _passable = passable;
    }

    /**
     * Returns the passability information for the tiles in this tileset.
     */
    public function getPassability () :Array
    {
        return _passable;
    }

    override protected function createTile () :Tile
    {
        return new BaseTile();
    }

    override protected function initTile (tile :Tile, tileIndex :int, zations :Array) :void
    {
        super.initTile(tile, tileIndex, zations);
        (BaseTile(tile)).setPassable(_passable[tileIndex]);
    }

    override protected function toStringBuf (buf :String) :String
    {
        buf = super.toStringBuf(buf);
        buf = buf.concat(", passable=", StringUtil.toString(_passable));
        return buf;
    }

    public static function fromXml (xml :XML) :SwissArmyTileSet
    {
        var set :BaseTileSet = new BaseTileSet();
        set.populateFromXml(xml);
        return set;
    }

    override protected function populateFromXml (xml :XML) :void
    {
        super.populateFromXml(xml);
        _passable = toBoolArray(xml.passable);
    }

    override public function populateClone (clone :TileSet) :void
    {
        super.populateClone(clone);
        var bClone :BaseTileSet = BaseTileSet(clone);
        bClone._passable = _passable == null ? null : ArrayUtil.copyOf(_passable);
    }

    override public function createClone () :TileSet
    {
        return new BaseTileSet();
    }

    /** Whether each tile is passable. */
    protected var _passable :Array;
}
}