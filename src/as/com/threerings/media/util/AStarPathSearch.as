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

import com.threerings.util.MathUtil;

/**
 * Searches for the shortest traversable path between locations.
 */
public class AStarPathSearch
{
    /**
     * Creates a searching object using tpred to check for traversability and stepper to find
     *  all the valid adjacent spots and the stepping costs & estimates.
     */
    public function AStarPathSearch (tpred :TraversalPred,
        stepper :AStarPathSearch_Stepper = null)
    {
        _tpred = tpred;

        if (stepper == null) {
            _stepper = new AStarPathSearch_Stepper();
        } else {
            _stepper = stepper;
        }
    }

        /**
     * Return a list of <code>Point</code> objects representing a path from coordinates
     * <code>(ax, by)</code> to <code>(bx, by)</code>, inclusive, determined by performing an
     * A* search in the given scene's base tile layer. Assumes the starting and destination nodes
     * are traversable by the specified traverser.
     *
     * @param trav the traverser to follow the path.
     * @param longest the longest allowable path in tile traversals. This arg must be small enough
     *        that _stepper.getMaxCost(longest) < Integer.MAX_VALUE
     * @param ax the starting x-position in tile coordinates.
     * @param ay the starting y-position in tile coordinates.
     * @param bx the ending x-position in tile coordinates.
     * @param by the ending y-position in tile coordinates.
     * @param partial if true, a partial path will be returned that gets us as close as we can to
     * the goal in the event that a complete path cannot be located.
     *
     * @return the list of points in the path, or null if no path could be found.
     */
    public function getPath (trav :Object, longest :int, ax :int, ay :int, bx :int, by :int,
                             partial :Boolean) :Array
    {
        var info :AStarPathSearch_Info = new AStarPathSearch_Info(_tpred, trav,
            _stepper.getMaxCost(longest), bx, by);

        // set up the starting node
        var s :AStarPathSearch_Node = info.getNode(ax, ay);
        s.g = 0;
        s.h = _stepper.getDistanceEstimate(ax, ay, bx, by);
        s.f = s.g + s.h;

        // push starting node on the open list
        info.open.add(s);

        // track the best path
        var bestdist :Number = Number.POSITIVE_INFINITY;
        var bestpath :AStarPathSearch_Node = null;

        // while there are more nodes on the open list
        while (info.open.size() > 0) {
            // pop the best node so far from open
            var n :AStarPathSearch_Node = null;

            info.open.forEach(function (val :AStarPathSearch_Node) :Boolean {
                n = val;
                return true;
            });
            info.open.remove(n);

            // if node is a goal node
            if (n.x == bx && n.y == by) {
                // construct and return the acceptable path
                return n.getNodePath();

            } else if (partial) {
                var pathdist :Number = MathUtil.distance(n.x, n.y, bx, by);
                if (pathdist < bestdist) {
                    bestdist = pathdist;
                    bestpath = n;
                }
            }

            // consider each successor of the node
            _stepper.considerSteps(info, n, n.x, n.y);

            // push the node on the closed list
            info.closed.add(n);
        }

        // return the best path we could find if we were asked to do so
        if (bestpath != null) {
            return bestpath.getNodePath();
        }

        // no path found
        return null;
    }



    /** In charge of determining if we can walk across various bits. */
    protected var _tpred :TraversalPred;

    /** In charge of finding all the steps from a given spot. */
    protected var _stepper :AStarPathSearch_Stepper;
}
}

