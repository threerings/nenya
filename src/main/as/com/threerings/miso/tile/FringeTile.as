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

package com.threerings.miso.tile {

import com.threerings.util.ArrayUtil;
import com.threerings.util.Hashable;

import com.threerings.media.tile.BaseTile;

public class FringeTile extends BaseTile
    implements Hashable
{
    public function FringeTile (fringeIds :Array, passable :Boolean)
    {
        setPassable(passable);
        _fringeIds = fringeIds;
    }

    public function equals (obj :Object) :Boolean
    {
        if (!(obj is FringeTile)) {
            return false;
        }
        var fObj :FringeTile = FringeTile(obj);
        return _passable == fObj._passable && ArrayUtil.equals(_fringeIds, fObj._fringeIds);
    }

    public function hashCode () :int
    {
        var result :int = 33; // can't use Arrays.hashCode(long) as it's 1.5 only
        for each (var key :int in _fringeIds) {
            result = result * 37 + key;
        }
        if (_passable) {
            result++;
        }
        return result;
    }

    /** The fringe keys of the tiles that went into this tile in the order they were drawn. */
    protected var _fringeIds :Array;
}
}