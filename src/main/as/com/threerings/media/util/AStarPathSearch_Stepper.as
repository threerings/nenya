//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
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

/**
 * Considers all the possible steps the piece in question can take.
 */
public class AStarPathSearch_Stepper
{
    public function AStarPathSearch_Stepper (considerDiagonals :Boolean = true)
    {
        _considerDiagonals = considerDiagonals;
    }

    public function getMaxCost (longest :int) :int
    {
        return longest * ADJACENT_COST;
    }

    /**
     * Return a heuristic estimate of the cost to get from <code>(ax, ay)</code> to
     * <code>(bx, by)</code>.
     */
    public function getDistanceEstimate (ax :int, ay :int, bx :int, by :int) :int
    {
        // we're doing all of our cost calculations based on geometric distance times ten
        var dx :int = bx - ax;
        var dy :int = by - ay;
        return int(Math.floor(ADJACENT_COST * Math.sqrt(dx * dx + dy * dy)));
    }

    /**
     * Should call {@link #considerStep} in turn on all possible steps from the specified
     * coordinates. No checking must be done as to whether the step is legal, that will be
     * handled later. Just enumerate all possible steps.
     */
    public function considerSteps (info :AStarPathSearch_Info, node :AStarPathSearch_Node,
        x :int, y :int) :void
    {
        info.considerStep(node, x, y - 1, ADJACENT_COST, this);
        info.considerStep(node, x, y + 1, ADJACENT_COST, this);
        info.considerStep(node, x - 1, y, ADJACENT_COST, this);
        info.considerStep(node, x + 1, y, ADJACENT_COST, this);
        if (_considerDiagonals) {
            info.considerStep(node, x - 1, y - 1, DIAGONAL_COST, this);
            info.considerStep(node, x + 1, y - 1, DIAGONAL_COST, this);
            info.considerStep(node, x - 1, y + 1, DIAGONAL_COST, this);
            info.considerStep(node, x + 1, y + 1, DIAGONAL_COST, this);
        }
    }

    protected var _considerDiagonals :Boolean;

    /** The standard cost to move between nodes. */
    protected static const ADJACENT_COST :int = 10;

    /** The cost to move diagonally. */
    protected static const DIAGONAL_COST :int = int(Math.sqrt((ADJACENT_COST * ADJACENT_COST) * 2));
}
}