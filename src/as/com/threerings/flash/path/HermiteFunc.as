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
 * Interpolates cubically between two values, with beginning and end derivates set
 * to zero. TODO: Add support for specifying derivate values as well.
 */
public class HermiteFunc extends InterpFunc
{
    public function HermiteFunc (start :int, end :int)
    {
        _start = start;
        _end = end;
    }

    // from InterpFunc
    override public function getValue (t :Number) :int
    {
        if (t >= 1) {
            return _end;
        } else if (t < 0) { // cope with a funny startOffset
            return _start;
        } else {
            var h00 :Number = 2*t*t*t - 3*t*t + 1;
            var h01 :Number = -2*t*t*t + 3*t*t;

            return int(_start * h00 + _end * h01);
        }
    }

    protected var _start :int;
    protected var _end :int;
}
}
