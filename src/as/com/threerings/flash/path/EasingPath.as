//
// $Id: LinePath.as 359 2007-12-03 21:08:33Z dhoover $

package com.threerings.flash.path {

import flash.display.DisplayObject;

/**
 * Adapts mx.effects.easing or Tweener functions for use as a Path.
 */
[Deprecated(replacement="caurina.transitions.Tweener")]
public class EasingPath extends Path
{
    /**
     * @param destX the destination x coordinate
     * @param destY the destination y coordinate
     * @param duration the duration of the path, in milliseconds.
     * @param xfunc a Function to provide x coordinates. Signature: 
     *        function (t :Number, b :Number, c :Number, d :Number) :Number
     *        t = elapsed time
     *        b = initial position
     *        c = total change in position
     *        d = duration
     * @param yfunc a Function to provide y coordinates. If omitted, the xfunc is used.
     * @param startX the starting x coordinate. If omitted, the DisplayObject's current coordinate
     *        is used.
     * @param startY the starting y coordinate. If omitted, the DisplayObject's current coordinate
     *        is used.
     */
    public function EasingPath (
        target :DisplayObject, destX :Number, destY :Number, duration :int,
        xfunc :Function, yfunc :Function = null, startX :Number = NaN, startY :Number = NaN)
    {
        if (isNaN(startX)) {
            startX = target.x;
        }
        if (isNaN(startY)) {
            startY = target.y;
        }
        if (yfunc == null) {
            yfunc = xfunc;
        }
        _duration = duration;
        _startX = startX;
        _startY = startY;
        _deltaX = destX - startX;
        _deltaY = destY - startY;
        _xfunc = xfunc;
        _yfunc = yfunc;
        init(target);
    }

    override protected function tick (curStamp :int) :int
    {
        var t :Number = curStamp - _startStamp;
        _target.x = _xfunc(t, _startX, _deltaX, _duration);
        _target.y = _yfunc(t, _startY, _deltaY, _duration);

        // return the number of milliseconds before we're done
        return _duration - t;
    }

    protected var _xfunc :Function;
    protected var _yfunc :Function;
    protected var _startX :Number;
    protected var _startY :Number;
    protected var _deltaX :Number;
    protected var _deltaY :Number;
    protected var _duration :int;
}
}
