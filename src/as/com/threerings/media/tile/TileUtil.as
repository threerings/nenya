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

public class TileUtil
{
    /**
     * Generates a fully-qualified tile id given the supplied tileset id
     * and tile index.
     */
    public static function getFQTileId (tileSetId :int, tileIndex :int) :int
    {
        return (tileSetId << 16) | tileIndex;
    }

    /**
     * Extracts the tile set id from the supplied fully qualified tile id.
     */
    public static function getTileSetId (fqTileId :int) :int
    {
        return (fqTileId >> 16);
    }

    /**
     * Extracts the tile index from the supplied fully qualified tile id.
     */
    public static function getTileIndex (fqTileId :int) :int
    {
        return (fqTileId & 0xFFFF);
    }

    /**
     * Compute some hash value for "randomizing" tileset picks
     * based on x and y coordinates.
     * NOTE: Because actionscript doesn't handle longs well, this does NOT match the implementation
     *  of the java version of getTileHash()
     *
     * @return a positive, seemingly random number based on x and y.
     */
    public static function getTileHash (x :int, y :int) :int
    {
        var seed :int = ((x ^ y) ^ MULTIPLIER) & MASK;
        var hash :int = (seed * MULTIPLIER + ADDEND) & MASK;
        return hash >>> 10;
    }

    protected static const MULTIPLIER :int = 0x5E66D;
    protected static const ADDEND :int = 0xB;
    protected static const MASK :int = (1 << 16) - 1;
}
}
