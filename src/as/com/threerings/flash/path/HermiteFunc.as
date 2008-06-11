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
 * to zero. See http://en.wikipedia.org/wiki/Cubic_Hermite_spline for details.
 */
[Deprecated(replacement="caurina.transitions.Tweener")]
public class HermiteFunc extends InterpFunc
{
    public function HermiteFunc (start :int, end :int, startSlope :Number = 0, endSlope :Number = 0)
    {
        _p0 = start;
        _p1 = end;
        _m0 = startSlope;
        _m1 = endSlope;
    }

    // from InterpFunc
    override public function getValue (t :Number) :Number
    {
        if (t >= 1) {
            return _p1;
        } else if (t < 0) { // cope with a funny startOffset
            return _p0;
        } else {
            var tt :Number = t*t;
            var ttt :Number = tt * t;

            return _p0 * (2*ttt - 3*tt + 1) +
                   _m0 * (ttt - 2*tt + t) +
                   _p1 * (-2*ttt + 3*tt) +
                   _m1 * (ttt - tt);
        }
    }

    /** Get the derivative of this function at a point. */
    public function getSlope (t :Number) :Number
    {
        if (t >= 1 || t < 0) { // cope with a funny startOffset
            return 0;
        }
        var tt :Number = t*t;

        return (_p0 - _p1) * (6*tt - 6*t) +
               _m0 * (3*tt - 4*t + 1) +
               _m1 * (3*tt - 2*t);
    }

    /** The coefficient for the spline that interpolates the beginning point value. */
    protected var _p0 :Number;

    /** The coefficient for the spline that interpolates the end point value. */
    protected var _p1 :Number;

    /** The coefficient for the spline that interpolates the beginning point derivate. */
    protected var _m0 :Number;

    /** The coefficient for the spline that interpolates the end point derivate. */
    protected var _m1 :Number;
}
}
