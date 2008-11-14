//
// $Id$

package com.threerings.flash.media {

import flash.display.DisplayObject;

import flash.geom.Point;

import com.threerings.util.ValueEvent;

/**
 * Disptached when the size of the video is known.
 * <b>value</b>: a Point expressing the width/height.
 *
 * @eventType com.threerings.flash.media.MediaPlayerCodes.SIZE
 */
[Event(name="size", type="com.threerings.util.ValueEvent")]

/**
 * Implemented by video-playing backends.
 */
public interface VideoPlayer extends MediaPlayer
{
    /**
     * Get the actual visualization of the video.
     */
    function getDisplay () :DisplayObject;

    /**
     * Get the size of the video, or null if not yet known.
     */
    function getSize () :Point;
}
}
