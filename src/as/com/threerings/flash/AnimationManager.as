package com.threerings.flash {

import flash.display.DisplayObject;
import flash.display.Sprite;

import flash.events.Event;

import flash.utils.getTimer; // func import

/**
 * Manages animations.
 */
public class AnimationManager
{
    public function AnimationManager ()
    {
        throw new Error("Static only");
    }
    
    /**
     * Start (or restart) the specified animation.
     */
    public static function start (anim :Animation) :void
    {
        var dex :int = _anims.indexOf(anim);
        if (dex == -1) {
            _anims.push(anim);
            _anims.push(NaN);

        } else {
            _anims[dex + 1] = NaN; // mark it as starting
        }

        if (!_framer) {
            _framer = new Sprite();
            _framer.addEventListener(Event.ENTER_FRAME, frameHandler);
        }
    }

    /**
     * Stop the specified animation.
     */
    public static function stop (anim :Animation) :void
    {
        var dex :int = _anims.indexOf(anim);
        if (dex == -1) {
            Log.getLog(AnimationManager).warning("Stopping unknown Animation: " + anim);
            return;
        }

        // remove it
        _anims.splice(dex, 2);

        // See if we should clean up a bit
        if (_anims.length == 0) {
            _framer.removeEventListener(Event.ENTER_FRAME, frameHandler);
            _framer = null;
        }
    }

    /**
     * Track a DisplayObject that is also an Animation- it will
     * automatically be started when added to the stage and 
     * stopped when removed.
     */
    public static function addDisplayAnimation (disp :DisplayObject) :void
    {
        if (!(disp is Animation)) {
            throw new ArgumentError("Must be an Animation");
        }

        disp.addEventListener(Event.ADDED_TO_STAGE, startDisplayAnim);
        disp.addEventListener(Event.REMOVED_FROM_STAGE, stopDisplayAnim);
        if (disp.stage != null) {
            // it's on the stage now!
            start(Animation(disp));
        }
    }

    /**
     * Stop tracking the specified DisplayObject animation.
     */
    public static function removeDisplayAnimation (disp :DisplayObject) :void
    {
        disp.removeEventListener(Event.ADDED_TO_STAGE, startDisplayAnim);
        disp.removeEventListener(Event.REMOVED_FROM_STAGE, stopDisplayAnim);
        if (disp.stage != null) {
            stop(Animation(disp));
        }
    }

    protected static function startDisplayAnim (event :Event) :void
    {
        start(event.currentTarget as Animation);
    }

    protected static function stopDisplayAnim (event :Event) :void
    {
        stop(event.currentTarget as Animation);
    }

    /**
     * Handle the ENTER_FRAME event.
     */
    protected static function frameHandler (event :Event) :void
    {
        var now :Number = getTimer();
        var anim :Animation;
        var startStamp :Number;
        for (var ii :int = _anims.length - 2; ii >= 0; ii -= 2) {
            anim = Animation(_anims[ii]);
            startStamp = Number(_anims[ii + 1]);
            if (isNaN(startStamp)) {
                _anims[ii + 1] = startStamp = now;
            }
            anim.updateAnimation(now - startStamp);
        }
    }

    /** The current timestamp, accessable to all animations. */
    protected static var _now :Number;

    /** All the currently running animations. */
    protected static var _anims :Array = [];

    protected static var _framer :Sprite;
}
}
