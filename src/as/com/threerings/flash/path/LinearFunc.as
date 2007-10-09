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

/**
 * Interpolates linearly between two values.
 */
public class LinearFunc extends InterpFunc
{
    public function LinearFunc (start :int, end :int)
    {
        _start = start;
        _end = end;
    }

    // from InterpFunc
    override public function getValue (complete :Number) :int
    {
        if (complete >= 1) {
            return _end;
        } else if (complete < 0) { // cope with a funny startOffset
            return _start;
        } else {
            return int((_end - _start) * complete) + _start;
        }
    }

    protected var _start :int;
    protected var _end :int;
}
}
