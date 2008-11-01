//
// $Id$

package com.threerings.flash.video {

import flash.display.DisplayObject;

import flash.events.IEventDispatcher;

import flash.geom.Point;

import com.threerings.util.ValueEvent;

/**
 *
 * @eventType com.threerings.flash.video.VideoPlayerCodes.STATE
 */
[Event(name="state", type="com.threerings.util.ValueEvent")]

/**
 *
 * @eventType com.threerings.flash.video.VideoPlayerCodes.DURATION
 */
[Event(name="duration", type="com.threerings.util.ValueEvent")]

/**
 *
 * @eventType com.threerings.flash.video.VideoPlayerCodes.POSITION
 */
[Event(name="position", type="com.threerings.util.ValueEvent")]

/**
 *
 * @eventType com.threerings.flash.video.VideoPlayerCodes.SIZE
 */
[Event(name="size", type="com.threerings.util.ValueEvent")]

/**
 *
 * @eventType com.threerings.flash.video.VideoPlayerCodes.ERROR
 */
[Event(name="error", type="com.threerings.util.ValueEvent")]

/**
 * Implemented by video playing backends.
 */
public interface VideoPlayer extends IEventDispatcher
{
    /**
     * Get the actual visualization of the video.
     */
    function getDisplay () :DisplayObject;

    /**
     * @return a VideoPlayerCodes state constant.
     */
    function getState () :int;

    /**
     * Get the size of the video, or null if not yet known.
     */
    function getSize () :Point;

    function play () :void;

    function pause () :void;

    /**
     * Get the duration of the video, or NaN if not yet known.
     */
    function getDuration () :Number;

    /**
     * Get the position of the video, or NaN if not yet ready.
     */
    function getPosition () :Number;

    /**
     * Seek to the specified position.
     */
    function seek (position :Number) :void;

    /**
     * Set the volume, between 0-1.
     */
    function setVolume (volume :Number) :void;

    /**
     * Get the volume, from 0 to 1.
     */
    function getVolume () :Number;

    /**
     * Unload the video.
     */
    function unload () :void;
}
}
