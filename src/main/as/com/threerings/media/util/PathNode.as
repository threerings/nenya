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

package com.threerings.media.util {

import flash.geom.Point;

/**
 * A path node is a single destination point in a {@link Path}.
 */
public class PathNode
{
/** The node coordinates in screen pixels. */
    public var loc :Point;

    /** The direction to face while heading toward the node. */
    public var dir :int;

    /**
     * Construct a path node object.
     *
     * @param x the node x-position.
     * @param y the node y-position.
     * @param dir the facing direction.
     */
    public function PathNode (x :Number, y :Number, dir :int)
    {
        loc = new Point(x, y);
        this.dir = dir;
    }

    public function toString () :String
    {
        return "[x=" + loc.x + ", y=" + loc.y + ", dir=" + dir + "]";
    }
}
}