//
// $Id$

package com.threerings.flash.media {

import flash.events.IEventDispatcher;

import com.threerings.util.ValueEvent;

/**
 * Dispatched when the state of the mediaplayer changes, usually in response to commands
 * such as play/pause, etc.
 * <b>value</b>: an int state. @see MediaPlayerCodes
 *
 * @eventType com.threerings.flash.media.MediaPlayerCodes.STATE
 */
[Event(name="state", type="com.threerings.util.ValueEvent")]

/**
 * Dispatched when the total duration of the media is known.
 * <b>value</b>: a Number expressing the duration in seconds.
 *
 * @eventType com.threerings.flash.media.MediaPlayerCodes.DURATION
 */
[Event(name="duration", type="com.threerings.util.ValueEvent")]

/**
 * Dispatched periodically as the position is updated, during playback.
 * <b>value</b>: a Number expressing the position in seconds.
 *
 * @eventType com.threerings.flash.media.MediaPlayerCodes.POSITION
 */
[Event(name="position", type="com.threerings.util.ValueEvent")]

/**
 * Dispatched when there's a problem.
 * <b>value</b>: a String error code/message.
 *
 * @eventType com.threerings.flash.media.MediaPlayerCodes.ERROR
 */
[Event(name="error", type="com.threerings.util.ValueEvent")]

/**
 * Implemented by media-playing backends.
 */
public interface MediaPlayer extends IEventDispatcher
{
    /**
     * @return a MediaPlayerCodes state constant.
     */
    function getState () :int;

    /**
     * Play the media, if not already.
     */
    function play () :void;

    /**
     * Pause the media, if not already.
     */
    function pause () :void;

    /**
     * Get the duration of the media, in seconds, or NaN if not yet known.
     */
    function getDuration () :Number;

    /**
     * Get the position of the media, in seconds, or NaN if not yet ready.
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
     * Unload the media.
     */
    function unload () :void;
}
}
