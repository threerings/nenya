package com.threerings.flash {

import flash.display.DisplayObject;

import flash.events.TimerEvent;

import flash.utils.getTimer; // func import
import flash.utils.Timer;

public class Animation
{
    public function Animation ()
    {
    }

    public function start () :void
    {
        addAnimation(this);
    }

    public function stop () :void
    {
        removeAnimation(this);
//        didStop();
    }

    /**
     * The primary working method for your animation.
     */
    protected function enterFrame () :void
    {
        // do your business here
    }
    
    /**
     * Add the specified anim to the list of active anims.
     */
    protected static function addAnimation (anim :Animation) :void
    {
        // add it
        _anims.push(anim);

        // set up the timer
        if (!_timer) {
            _timer = new Timer(10); // 10ms: will be limited by frame rate
            _timer.addEventListener(TimerEvent.TIMER, frameHandler);
        }
        _timer.start();
    }

    /**
     * Remove the specified anim.
     */
    protected static function removeAnimation (anim :Animation) :void
    {
        var dex :int = _anims.indexOf(anim);
        if (dex != -1) {
            _anims.splice(dex, 1);

        } else {
            Log.getLog(Animation).warning("Removing unknown Animation: " + anim);
        }

        if (_anims.length == 0) {
            _timer.reset();
        }
    }

    /**
     * Handle our timer event.
     */
    protected static function frameHandler (event :TimerEvent) :void
    {
        _now = getTimer();
        var an :Animation;
        for (var ii :int = _anims.length - 1; ii >= 0; ii--) {
            an = Animation(_anims[ii]);
            if (isNaN(an._start)) {
                an._start = _now;
//                an.didStart();
            }
            an.enterFrame();
        }

        // and instruct the flash player to update the display list (now!)
        event.updateAfterEvent();
    }

    /** Our own personal start stamp. */
    protected var _start :Number = NaN;

    /** The current timestamp, accessable to all animations. */
    protected static var _now :Number;

    /** All the currently running animations. */
    protected static var _anims :Array = [];

    /** The timer we use to manage our animations. */
    protected static var _timer :Timer;
}
}
