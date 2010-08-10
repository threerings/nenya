//
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

package com.threerings.cast {

import com.threerings.util.Hashable;

/** Used to cache composited frames for a particular action and
 * orientation. */
public class CompositedFramesKey
    implements Hashable
{
    public function CompositedFramesKey (owner :CompositedActionFrames, orient :int) {
        _orient = orient;
        _owner = owner;
    }

    public function setOrient (orient :int) :void {
        _orient = orient;
    }

    public function getOwner () :CompositedActionFrames {
        return _owner;
    }

    public function equals (other :Object) :Boolean  {
        var okey :CompositedFramesKey = CompositedFramesKey(other);
        return ((getOwner() == okey.getOwner()) &&
                (_orient == okey._orient));
    }

    public function hashCode () :int {
        return _owner.hashCode() ^ _orient;
    }

    protected var _orient :int;

    protected var _owner :CompositedActionFrames;
}
}