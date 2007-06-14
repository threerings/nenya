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
            dex = _anims.length;
            _anims.push(anim);
        }
        _anims[dex + 1] = getTimer(); // mark it as starting

        // and update it immediately
        anim.updateAnimation(0);

        // we re-check that the anims are non-empty because the first animation
        // to be added could be removed as a result of calling updateAnimation() on it.
        if (!_framer && _anims.length > 0) {
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
        if (_anims.length == 0 && _framer) {
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
            Animation(_anims[ii]).updateAnimation(now - Number(_anims[ii + 1]));
        }
    }

    /** The current timestamp, accessable to all animations. */
    protected static var _now :Number;

    /** All the currently running animations. */
    protected static var _anims :Array = [];

    protected static var _framer :Sprite;
}
}
