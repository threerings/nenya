//
// $Id$

package com.threerings.flash.path {

import flash.display.DisplayObject;

/**
 * Moves a display object along a line path in a specified amount of time.
 */
public class LinePath extends Path
{
    public function LinePath (target :DisplayObject, xfunc :InterpFunc, yfunc :InterpFunc,
                              duration :int)
    {
        _xfunc = xfunc;
        _yfunc = yfunc;
        _duration = duration;
        init(target);
    }

    override protected function tick (curStamp :int) :int
    {
        var complete :Number = (curStamp - _startStamp) / _duration;
        _target.x = _xfunc.getValue(complete);
        _target.y = _yfunc.getValue(complete);

        // return the number of milliseconds before we're done
        return _duration - (curStamp - _startStamp);
    }

    protected var _xfunc :InterpFunc;
    protected var _yfunc :InterpFunc;
    protected var _duration :int;
}
}
