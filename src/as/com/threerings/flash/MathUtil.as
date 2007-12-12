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

package com.threerings.flash {    

/**
 * Collection of math utility functions.
 */
public class MathUtil
{
    /**
     * Returns the value of n clamped to be within the range [min, max].
     */
    public static function clamp (n :Number, min :Number, max :Number) :Number
    {
        return Math.min(Math.max(n, min), max);
    }

    /**
     * Returns distance from point (x1, y1) to (x2, y2) in 2D.
     *
     * <p>Supports various distance metrics: the common Euclidean distance, taxicab distance,
     * arbitrary Minkowski distances, and Chebyshev distance.
     *
     * <p>See the <a href="http://www.nist.gov/dads/HTML/lmdistance.html">NIST web page on 
     * distance definitions</a>.<p>
     *
     * @param x1 x value of the first point
     * @param y1 y value of the first point
     * @param x2 x value of the second point
     * @param y2 y value of the second point    
     * @param p Optional: p value of the norm function. Common cases:
     *          <ul><li>p = 2 (default): standard Euclidean distance on a plane
     *              <li>p = 1: taxicab distance (aka Manhattan distance)
     *              <li>p = Infinity: Chebyshev distance
     *          </ul>
     *          <b>Note</b>: p < 1 or p = NaN are treated as equivalent to p = Infinity
     */
    public static function distance (
        x1 :Number, y1 :Number, x2 :Number, y2 :Number, p :int = 2) :Number
    {
        if (! isFinite(p) || p < 1) {
            p = Infinity;
        }

        var dx :Number = x2 - x1;
        var dy :Number = y2 - y1;
        
        switch (p) {
        case 1:
            // optimized version for taxicab distance
            return dx + dy;
        case 2:
            // optimized version for Euclidean
            return Math.sqrt(dx * dx + dy * dy);
        case Infinity:
            // optimized version for Chebyshev
            return Math.max(dx, dy);
        default:
            // generic version
            var xx :Number = Math.pow(dx, p);
            var yy :Number = Math.pow(dy, p);
            return Math.pow(xx + yy, 1 / p);
        }
    }
            
}

}
        
