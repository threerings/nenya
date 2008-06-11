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
 * Doesn't actually move anything, but simply delays in the current position for some period of
 * time. This is generally used with a {@link CompositePath}.
 */
[Deprecated(replacement="caurina.transitions.Tweener")]
public class DelayPath extends Path
{
    public function DelayPath (delay :int)
    {
        _delay = delay;
    }

    override protected function tick (curStamp :int) :int
    {
        return _delay - (curStamp - _startStamp);
    }

    protected var _delay :int;
}
}
