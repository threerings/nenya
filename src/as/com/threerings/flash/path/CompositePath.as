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

package com.threerings.flash.path {

import flash.display.Sprite;

import com.threerings.util.Util;

/**
 * Executes a series of paths in order.
 */
public class CompositePath extends Path
{
    /**
     * Creates a composite path with the supplied list of individual paths.
     */
    public function CompositePath (... paths)
    {
        _paths = Util.unfuckVarargs(paths);
        init(new Sprite()); // any old display object will do (yay actionscript)
    }

    // from Path
    override protected function tick (curStamp :int) :int
    {
        var remain :int = 0;
        if (_pathIdx >= 0) {
            remain = tickPath(_paths[_pathIdx] as Path, curStamp);
        }
        while (remain <= 0 && ++_pathIdx < _paths.length) {
            remain = startPath(_paths[_pathIdx] as Path, curStamp, remain);
        }
        return remain;
    }

    protected var _paths :Array;
    protected var _pathIdx :int = -1;
}
}
