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

import com.threerings.media.image.Colorization;
import com.threerings.util.Arrays;
import com.threerings.util.Hashable;

/** Used when caching tiles. */
public class Tile_Key
    implements Hashable
{
    public var tileSet :TileSet;
    public var tileIndex :int;
    public var zations :Array;

    public function Tile_Key (tileSet :TileSet, tileIndex :int, zations :Array) {
        this.tileSet = tileSet;
        this.tileIndex = tileIndex;
        this.zations = zations;
    }

    public function equals (other :Object) :Boolean
    {
        if (other is Tile_Key) {
            var okey :Tile_Key = Tile_Key(other);
            return (tileSet == okey.tileSet &&
                    tileIndex == okey.tileIndex &&
                    Arrays.equals(zations, okey.zations));
        } else {
            return false;
        }
    }

    public function hashCode () :int
    {
        var code :int = (tileSet == null) ? tileIndex :
            (tileSet.hashCode() ^ tileIndex);
        var zcount :int = (zations == null) ? 0 : zations.length;
        for each (var zation :Colorization in zations) {
            if (zation != null) {
                code ^= zation.hashCode();
            }
        }
        return code;
    }
}
}