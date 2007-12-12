//
// $Id$

package com.threerings.flash.path {

import flash.display.DisplayObject;
import flash.events.Event;
import flash.utils.getTimer;

/**
 * Moves a display object along a particular path in a specified amount of time.
 */
public /*abstract*/ class Path
{
    /**
     * Moves the specified display object from the specified starting coordinates to the specified
     * ending coordinates in the specified number of milliseconds.
     */
    public static function move (target :DisplayObject, startx :int, starty :int,
                                 destx :int, desty :int, duration :int) :Path
    {
        return new LinePath(target, new LinearFunc(startx, destx),
                            new LinearFunc(starty, desty), duration);
    }

    /**
     * Moves the specified display object from its current location to the specified ending
     * coordinates in the specified number of milliseconds. <em>NOTE:</em> beware the fact that
     * Flash does not immediately apply a display object's location update, so setting x and y and
     * then calling moveTo() will not work. Use {@link #move} instead.
     */
    public static function moveTo (target :DisplayObject, destx :int, desty :int,
                                   duration :int) :Path
    {
        return move(target, target.x, target.y, destx, desty, duration);
    }

    /**
     * Creates a delay path of the desired duration. For use with CompositePath.
     */
    public static function delay (delay :int) :Path
    {
        return new DelayPath(delay);
    }

    /**
     * Creates a path that executes the supplied sequence of paths one after the other.
     */
    public static function connect (... paths) :Path
    {
        return new CompositePath(paths);
    }

    /**
     * Starts this path. The path will be ticked once immediately and subsequently ticked every
     * frame.
     *
     * @param onComplete an optional function to be called when it completes (or is aborted).
     * @param startOffset an optional number of milliseconds by which to adjust the time at which
     * the path believes that it was started.
     */
    public function start () :void
    {
        _target.addEventListener(Event.ENTER_FRAME, onEnterFrame);
        willStart(getTimer(), 0);
    }

    /**
     * Configures this path's onStart function. The function should have the following signature:
     * <code>function (path :Path) :void</code>. This should only be called before the path is
     * started.
     *
     * @return a reference to this path, for easy chaining.
     */
    public function setOnStart (onStart :Function) :Path
    {
        _onStart = onStart;
        return this;
    }

    /**
     * Configures this path's onComplete function. The function should have the following
     * signature: <code>function (path :Path) :void</code>. This should generally only be called
     * shortly after construction as the function will not be called if the path is already
     * completed or aborted.
     *
     * @return a reference to this path, for easy chaining.
     */
    public function setOnComplete (onComplete :Function) :Path
    {
        _onComplete = onComplete;
        return this;
    }

    /**
     * Aborts this path. Any onComplete() function will be called as if the path terminated
     * normally. The callback can call {@link #wasAborted} to discover whether the path was aborted
     * or terminated normally.
     */
    public function abort () :void
    {
        pathCompleted(true);
    }

    /**
     * Returns the target of this path.
     */
    public function get target () :DisplayObject
    {
        return _target;
    }

    /**
     * Returns true if this path was aborted, false if it completed normally. This return value is
     * only valid during a call to onComplete().
     */
    public function wasAborted () :Boolean
    {
        return _wasAborted;
    }

    /**
     * Derived classes must call this method to wire this path up to its target.
     */
    protected function init (target :DisplayObject) :void
    {
        _target = target;
    }

    /**
     * Called when this path is about to start, either due to a call to {@link #start} or to the
     * path being started by a {@link CompositePath}.
     */
    protected function willStart (now :int, startOffset :int = 0) :int
    {
        _startStamp = now + startOffset;
        if (_onStart != null) {
            _onStart(this);
        }
        var remain :int = tick(now);
        if (remain <= 0) {
           pathCompleted(false);
        }
        return remain;
    }

    /**
     * Derived classes should override this method and update their target based on the current
     * timestamp. They should return a positive number, indicating the number of milliseconds they
     * have remaining before they are complete, or zero if they completed perfectly on time, or a
     * negative number if they completed with milliseconds to spare since their last tick.
     */
    protected function tick (curStamp :int) :int
    {
        return 0;
    }

    /**
     * Called when this path is has completed or was aborted.
     */
    protected function didComplete (aborted :Boolean) :void
    {
        _wasAborted = aborted;
        if (_onComplete != null) {
            _onComplete(this);
        }
    }

    protected function onEnterFrame (event :Event) :void
    {
        if (tick(getTimer()) <= 0) {
            pathCompleted(false);
        }
    }

    protected function pathCompleted (aborted :Boolean) :void
    {
        _target.removeEventListener(Event.ENTER_FRAME, onEnterFrame);
        didComplete(aborted);
    }

    // needed by CompositePath
    protected static function tickPath (path :Path, curStamp :int) :int
    {
        return path.tick(curStamp);
    }

    // needed by CompositePath
    protected static function startPath (path :Path, curStamp :int, startOffset :int) :int
    {
        return path.willStart(curStamp, startOffset);
    }

    protected var _target :DisplayObject;
    protected var _onStart :Function;
    protected var _onComplete :Function;
    protected var _startStamp :int = -1;
    protected var _wasAborted :Boolean;
}

}
