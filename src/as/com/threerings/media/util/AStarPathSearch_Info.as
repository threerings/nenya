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

package com.threerings.media.util {

import com.threerings.util.Maps;

import com.threerings.util.Map;
import com.threerings.util.Set;
import com.threerings.util.Sets;

/**
 * A holding class to contain the wealth of information referenced
 * while performing an A* search for a path through a tile array.
 */
public class AStarPathSearch_Info
{
    /** Knows whether or not tiles are traversable. */
    public var tpred :TraversalPred;

    /** The tile array dimensions. */
    public var tilewid :int;
    public var tilehei :int;

    /** The traverser moving along the path. */
    public var trav :Object;

    /** The set of open nodes being searched. */
    // TODO - This would benefit from a more efficient implementation of SortedSet, but for now
    //  it is good enough.
    public var open :Set;

    /** The set of closed nodes being searched. */
    public var closed :Set;

    /** The destination coordinates in the tile array. */
    public var destx :int;
    public var desty :int;

    /** The maximum cost of any path that we'll consider. */
    public var maxcost :int;

    public function AStarPathSearch_Info (tpred :TraversalPred, trav :Object, maxcost :int,
        destx :int, desty :int)
    {
        // save off references
        this.tpred = tpred;
        this.trav = trav;
        this.destx = destx;
        this.desty = desty;

        // compute our maximum path cost
        this.maxcost = maxcost;

        // construct the open and closed lists
        open = Sets.newSortedSetOf(AStarPathSearch_Node);
        closed = Sets.newSetOf(AStarPathSearch_Node);
    }

    /**
     * Returns whether moving from the given source to destination coordinates is a valid
     * move.
     */
    protected function isStepValid (sx :int, sy :int, dx :int, dy :int) :Boolean
    {
        // not traversable if the destination itself fails test
        if (tpred is ExtendedTraversalPred) {
            if (!ExtendedTraversalPred(tpred).canTraverseBetween(trav, sx, sy, dx, dy)) {
                return false;
            }
        } else if (!isTraversable(dx, dy)) {
            return false;
        }

        // if the step is diagonal, make sure the corners don't impede our progress
        if ((Math.abs(dx - sx) == 1) && (Math.abs(dy - sy) == 1)) {
            return isTraversable(dx, sy) && isTraversable(sx, dy);
        }

        // non-diagonals are always traversable
        return true;
    }

    /**
     * Returns whether the given coordinate is valid and traversable.
     */
    protected function isTraversable (x :int, y :int) :Boolean
    {
        return tpred.canTraverse(trav, x, y);
    }

    /**
     * Get or create the node for the specified point.
     */
    public function getNode (x :int, y :int) :AStarPathSearch_Node
    {
        // note: this _could_ break for unusual values of x and y.
        // perhaps use a IntTuple as a key? Bleah.
        var key :int = (x << 16) | (y & 0xffff);
        var node :AStarPathSearch_Node = _nodes.get(key);
        if (node == null) {
            node = new AStarPathSearch_Node(x, y);
            _nodes.put(key, node);
        }
        return node;
    }

    /**
     * Consider the step <code>(n.x, n.y)</code> to <code>(x, y)</code> for possible inclusion
     * in the path.
     *
     * @param info the info object.
     * @param node the originating node for the step.
     * @param x the x-coordinate for the destination step.
     * @param y the y-coordinate for the destination step.
     */
    public function considerStep (node :AStarPathSearch_Node, x :int, y :int, cost :int,
        stepper :AStarPathSearch_Stepper) :void
    {
        // skip node if it's outside the map bounds or otherwise impassable
        if (!isStepValid(node.x, node.y, x, y)) {
            return;
        }

        // calculate the new cost for this node
        var newg :int = node.g + cost;

        // make sure the cost is reasonable
        if (newg > maxcost) {
            return;
        }

        // retrieve the node corresponding to this location
        var np :AStarPathSearch_Node = getNode(x, y);

        // skip if it's already in the open or closed list or if its
        // actual cost is less than the just-calculated cost
        if ((open.contains(np) || closed.contains(np)) && np.g <= newg) {
            return;
        }

        // remove the node from the open list since we're about to
        // modify its score which determines its placement in the list
        open.remove(np);

        // update the node's information
        np.parent = node;
        np.g = newg;
        np.h = stepper.getDistanceEstimate(np.x, np.y, destx, desty);
        np.f = np.g + np.h;

        // remove it from the closed list if it's present
        closed.remove(np);

        // add it to the open list for further consideration
        open.add(np);
    }

    /** The nodes being considered in the path. */
    protected var _nodes :Map = Maps.newMapOf(int);
}
}