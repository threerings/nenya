//
// $Id$

package com.threerings.flash {

/**
 * Some easing functions.
 */
public class Easing
{
    /**
     * Interpolates cubically between two values, with beginning and end derivates set
     * to zero. See http://en.wikipedia.org/wiki/Cubic_Hermite_spline for details.
     */
    public static function cubicHermiteSpline (
        t :Number, b :Number, c :Number, d :Number, p_params :Object = null) :Number
    {
        // convert t to 0-1
        t = (t / d);
        if (t <= 0) {
            return b;
        }
        const end :Number = b + c;
        if (t >= 1) {
            return end;
        }
        const startSlope :Number = (p_params == null) ? 0 : Number(p_params["startSlope"]);
        const endSlope :Number = (p_params == null) ? 0 : Number(p_params["endSlope"]);
        const tt :Number = t * t;
        const ttt :Number = tt * t;
        return b * (2*ttt - 3*tt + 1) +
               startSlope * (ttt - 2*tt + t) +
               end * (-2*ttt + 3*tt) +
               endSlope * (ttt -tt);
    }
}
}
