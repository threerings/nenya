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

import com.threerings.util.Comparable;

public class AStarPathSearch_Node
    implements Comparable
{
    /** The node coordinates. */
    public var x :int;
    public var y :int;

    /** The actual cheapest cost of arriving here from the start. */
    public var g :int;

    /** The heuristic estimate of the cost to the goal from here. */
    public var h :int;

    /** The score assigned to this node. */
    public var f :int;

    /** The node from which we reached this node. */
    public var parent :AStarPathSearch_Node;

    /** The node's monotonically-increasing unique identifier. */
    public var id :int;

    public function AStarPathSearch_Node (x :int, y :int)
    {
        this.x = x;
        this.y = y;
        id = _nextid++;
    }

    public function compareTo (o :Object) :int
    {
        var n :AStarPathSearch_Node = AStarPathSearch_Node(o);
        var bf :int = n.f;

        // since the set contract is fulfilled using the equality results returned here, and
        // we'd like to allow multiple nodes with equivalent scores in our set, we explicitly
        // define object equivalence as the result of object.equals(), else we use the unique
        // node id since it will return a consistent ordering for the objects.
        if (f == bf) {
            return (this == n) ? 0 : (id - n.id);
        }

        return f - bf;
    }

    /**
     * Return an array of <code>Point</code> objects detailing the path from the first node (the
     * given node's ultimate parent) to the ending node (the given node itself.)
     *
     * @param node the ending node in the path.
     *
     * @return the list detailing the path.
     */
    public function getNodePath () :Array
    {
        var cur :AStarPathSearch_Node = this;
        var path :Array = [];

        while (cur != null) {
            // add to the head of the list since we're traversing from
            // the end to the beginning
            path.unshift(new Point(cur.x, cur.y));

            // advance to the next node in the path
            cur = cur.parent;
        }

        return path;
    }

    /** The next unique node id. */
    protected static var _nextid :int = 0;
}
}