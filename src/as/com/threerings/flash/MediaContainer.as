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

import flash.display.Bitmap;
import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.Shape;
import flash.display.Sprite;

import flash.errors.IOError;

import flash.events.Event;
import flash.events.EventDispatcher;
import flash.events.ErrorEvent;
import flash.events.IOErrorEvent;
import flash.events.MouseEvent;
import flash.events.NetStatusEvent;
import flash.events.ProgressEvent;
import flash.events.SecurityErrorEvent;
import flash.events.StatusEvent;
import flash.events.TextEvent;

import flash.geom.Point;
import flash.geom.Rectangle;

import flash.net.URLRequest;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.system.Security;
import flash.system.SecurityDomain;

import flash.utils.ByteArray;

import com.threerings.util.Log;
import com.threerings.util.StringUtil;
import com.threerings.util.ValueEvent;
import com.threerings.util.Util;

import com.threerings.flash.video.FlvVideoPlayer;
import com.threerings.flash.video.SimpleVideoDisplay;
import com.threerings.flash.video.VideoPlayer;
import com.threerings.flash.video.VideoPlayerCodes;

/**
 * Dispatched when the size of the media being loaded is known.
 *
 * @eventType com.threerings.flash.MediaContainer.SIZE_KNOWN
 */
[Event(name="mediaSizeKnown", type="com.threerings.util.ValueEvent")]

/**
 * Dispatched when we've initialized our content. This is merely a redispatch
 * of the INIT event we get from the loader.
 *
 * @eventType flash.events.Event.INIT
 */
[Event(name="init", type="flash.events.Event")]

/**
 * Dispatched when we've shown new media.
 *
 * @eventType flash.events.Event.COMPLETE
 */
[Event(name="complete", type="flash.events.Event")]

/**
 * Dispatched when we've unloaded our content... always. The LoaderInfo's UNLOAD event
 * is only dispatched if the INIT event has already been dispatched and not if you cancel a
 * load before it INIT.
 */
[Event(name="unload", type="flash.events.Event")]

/**
 * A wrapper class for all media that will be placed on the screen.
 * Subject to change.
 */
public class MediaContainer extends Sprite
{
    /** A ValueEvent we dispatch when our size is known.
     * Value: [ width, height ].
     *
     * @eventType mediaSizeKnown
     */
    public static const SIZE_KNOWN :String = "mediaSizeKnown";

    /** A log instance that can be shared by sprites. */
    protected static const log :Log = Log.getLog(MediaContainer);

    /**
     * Constructor.
     */
    public function MediaContainer (url :String = null)
    {
        if (url != null) {
            setMedia(url);
        }

        mouseEnabled = false;
        mouseChildren = true;
    }

    /**
     * Return true if the content has been initialized.
     * For most content, this is true if the media is non-null, but
     * for anything loaded with a Loader, it is only true after the INIT event is dispatched.
     */
    public function isContentInitialized () :Boolean
    {
        return (_media != null) && (_initialized || !(_media is Loader));
    }

    /**
     * Get the media. If the media was loaded using a URL, this will
     * likely be the Loader object holding the real media.
     */
    public function getMedia () :DisplayObject
    {
        return _media;
    }

    /**
     * Configure the media to display.
     */
    public function setMedia (url :String) :void
    {
        if (Util.equals(_url, url)) {
            return; // no change
        }

        // shutdown any previous media
        if (_media != null) {
            shutdown(false);
        }
        _url = url;

        // set up the new media
        if (url != null) {
            willShowNewMedia();
            showNewMedia(url);
            didShowNewMedia();
        }
    }

    /**
     * Set the media to display as a ByteArray.
     */
    public function setMediaBytes (bytes :ByteArray) :void
    {
        if (_media != null) {
            shutdown(false);
        }
        _url = null;

        willShowNewMedia();
        startedLoading();
        initLoader().loadBytes(bytes, getContext(null));
        didShowNewMedia();
    }

    /**
     * Configure our media as an instance of the specified class.
     */
    public function setMediaClass (clazz :Class) :void
    {
        setMediaObject(new clazz() as DisplayObject);
    }

    /**
     * Configure an already-instantiated DisplayObject as our media.
     */
    public function setMediaObject (disp :DisplayObject) :void
    {
        if (_media != null) {
            shutdown(false);
        }
        _url = null;

        willShowNewMedia();
        addChildAt(disp, 0);
        _media = disp;
        updateContentDimensions(disp.width, disp.height);
        didShowNewMedia();
    }

    /**
     * Sets whether this MediaContainer automatically shuts down when removed
     * from the stage. By default this is not enabled.
     */
    public function setShutdownOnRemove (enable :Boolean = true) :void
    {
        var fn :Function = enable ? addEventListener : removeEventListener;
        fn(Event.REMOVED_FROM_STAGE, handleShutdownOnRemove);
    }

    /**
     * A place where subclasses can initialize things prior to showing new media.
     */
    protected function willShowNewMedia () :void
    {
        _initialized = false;
        _isImage = false;
    }

    protected function showNewMedia (url :String) :void
    {
        if (StringUtil.endsWith(url.toLowerCase(), ".flv")) {
            setupVideo(url);

        } else {
            setupSwfOrImage(url);
        }
    }

    /**
     * A place where subclasses can configure things after we've setup new media.
     */
    protected function didShowNewMedia () :void
    {
        // nada in the base class...
    }

    /**
     * Configure this sprite to show a video.
     */
    protected function setupVideo (url :String) :void
    {
        var player :FlvVideoPlayer = new FlvVideoPlayer();
        _media = createVideoUI(player);
        addChildAt(_media, 0);
        player.load(url);
        updateContentDimensions(_media.width, _media.height);
    }

    /**
     * Create the actual display for the VideoPlayer.
     */
    protected function createVideoUI (player :VideoPlayer) :DisplayObject
    {
        return new SimpleVideoDisplay(player);
    }

    /**
     * Configure this sprite to show an image or flash movie.
     */
    protected function setupSwfOrImage (url :String) :void
    {
        startedLoading();

        var loader :Loader = initLoader();
        loader.load(new URLRequest(url), getContext(url));

        try {
            var info :LoaderInfo = loader.contentLoaderInfo;
            updateContentDimensions(info.width, info.height);
        } catch (err :Error) {
            // an error is thrown trying to access these props before they're
            // ready
        }
    }

    /**
     * Initialize a Loader as our _media, and configure it however needed to prepare
     * loading user content.
     */
    protected function initLoader () :Loader
    {
        // create our loader and set up some event listeners
        var loader :Loader = new Loader();
        _media = loader;
        addChildAt(loader, 0);

        addListeners(loader.contentLoaderInfo);
        return loader;
    }

    /**
     * Display a 'broken image' to indicate there were troubles with
     * loading the media.
     */
    protected function setupBrokenImage (w :int = -1, h :int = -1) :void
    {
        if (w == -1) {
            w = 100;
        }
        if (h == -1) {
            h = 100;
        }
        setMediaObject(ImageUtil.createErrorImage(w, h));
    }

    /**
     * Unload the media we're displaying, clean up any resources.
     *
     * @param completely if true, we're going away and should stop
     * everything. Otherwise, we're just loading up new media.
     */
    public function shutdown (completely :Boolean = true) :void
    {
        // remove the mask (but only if we added it)
        if (_media != null && _media.mask != null) {
            try {
                removeChild(_media.mask);
                _media.mask = null;

            } catch (argErr :ArgumentError) {
                // If we catch this error, it was thrown in removeChild
                // and means that we did not add the media's mask,
                // and so shouldn't remove it either. The action we
                // take here is NOT setting _media.mask = null.
                // Then, we continue happily...
            }
        }

        shutdownMedia();
        if (_media != null) {
            removeChild(_media);
            dispatchEvent(new Event(Event.UNLOAD));
        }

        // clean everything up
        _w = 0;
        _h = 0;
        _media = null;
    }

    /**
     * Get the width of the content, bounded by the maximum.
     */
    public function getContentWidth () :int
    {
        return Math.min(Math.abs(_w * getMediaScaleX()), getMaxContentWidth());
    }

    /**
     * Get the height of the content, bounded by the maximum.
     */
    public function getContentHeight () :int
    {
        return Math.min(Math.abs(_h * getMediaScaleY()), getMaxContentHeight());
    }

    /**
     * Get the maximum allowable width for our content.
     */
    public function getMaxContentWidth () :int
    {
        return int.MAX_VALUE;
    }

    /**
     * Get the maximum allowable height for our content.
     */
    public function getMaxContentHeight () :int
    {
        return int.MAX_VALUE;
    }

    /**
     * Get the X scaling factor to use on the actual media.
     */
    public function getMediaScaleX () :Number
    {
        return 1;
    }

    /**
     * Get the Y scaling factor to use on the actual media.
     */
    public function getMediaScaleY () :Number
    {
        return 1;
    }

    /**
     * Called by MediaWrapper as notification that its size has changed.
     */
    public function containerDimensionsUpdated (newWidth :Number, newHeight :Number) :void
    {
        // do nothing in base MediaContainer 
    }

    /**
     * Note: This method is NOT used in normal mouseOver calculations.
     * Normal mouseOver stuff seems to be completely broken for transparent
     * images: the transparent portion is a 'hit'. I've (Ray) tried
     * just about everything to fix this, more than once.
     *
     * But if someone *does* call this method (we do, in whirled), then
     * attempt to do the right thing.
     */
    override public function hitTestPoint (
        x :Number, y :Number, shapeFlag :Boolean = false) :Boolean
    {
        // if we're holding a bitmap, do something smarter than the
        // flash built-in and actually check for *GASP* transparent pixels!
        try {
            // We should be able to test if the media is a Loader (we can),
            // then test if childAllowsParent, but even CHECKING that value causes a security
            // exception. WTF?!? That's supposed to be the value to check to AVOID getting a
            // security exception. So instead, we track whether or not we've
            // loaded an image and use that value.
            if (shapeFlag && _isImage) {
                var b :Bitmap = Bitmap(Loader(_media).content);
                var p :Point = b.globalToLocal(new Point(x, y));
                // check that it's within the content bounds, and then check the bitmap directly
                if (p.x >= 0 && p.x <= getMaxContentWidth() && p.y >= 0 &&
                        p.y <= getMaxContentHeight() &&
                        b.bitmapData.hitTest(new Point(0, 0), 0, p)) {
                    return true;

                } else {
                    // the bitmap was not hit, see if other children were hit...
                    for (var ii :int = numChildren - 1; ii >= 0; ii--) {
                        var child :DisplayObject = getChildAt(ii);
                        if (child != _media && child.hitTestPoint(x, y, shapeFlag)) {
                            return true;
                        }
                    }
                    return false;
                }
            }
        } catch (err :Error) {
            // nada
        }

        // normal hit testing
        return super.hitTestPoint(x, y, shapeFlag);
    }

    override public function toString () :String
    {
        return "MediaContainer[url=" + _url + "]";
    }

    /**
     * Return the LoaderContext that should be used to load the media
     * at the specified url.
     */
    protected function getContext (url :String) :LoaderContext
    {
        if (isImage(url)) {
            _isImage = true;
            // load images into our domain so that we can view their pixels
            return new LoaderContext(true, 
                new ApplicationDomain(ApplicationDomain.currentDomain),
                getSecurityDomain(url));

        } else {
            // share nothing, trust nothing
            return new LoaderContext(false, new ApplicationDomain(null), null);
        }
    }

    /**
     * Return the security domain to use for the specified image url.
     */
    protected function getSecurityDomain (imageURL :String) :SecurityDomain
    {
        switch (Security.sandboxType) {
        case Security.LOCAL_WITH_FILE:
        case Security.LOCAL_WITH_NETWORK:
        case Security.LOCAL_TRUSTED:
            return null;

        default:
            return SecurityDomain.currentDomain;
        }
    }

    /**
     * Does the specified url represent an image?
     */
    protected function isImage (url :String) :Boolean
    {
        if (url == null) {
            return false;
        }

        // look at the last 4 characters in the lowercased url
        switch (url.toLowerCase().slice(-4)) {
        case ".png":
        case ".jpg":
        case ".gif":
            return true;

        default:
            return false;
        }
    }

    /**
     * Add our listeners to the LoaderInfo object.
     */
    protected function addListeners (info :LoaderInfo) :void
    {
        info.addEventListener(Event.COMPLETE, handleComplete);
        info.addEventListener(Event.INIT, handleInit);
        info.addEventListener(IOErrorEvent.IO_ERROR, handleError);
        info.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleError);
        info.addEventListener(ProgressEvent.PROGRESS, handleProgress);
    }

    /**
     * Remove our listeners from the LoaderInfo object.
     */
    protected function removeListeners (info :LoaderInfo) :void
    {
        info.removeEventListener(Event.COMPLETE, handleComplete);
        info.removeEventListener(Event.INIT, handleInit);
        info.removeEventListener(IOErrorEvent.IO_ERROR, handleError);
        info.removeEventListener(SecurityErrorEvent.SECURITY_ERROR, handleError);
        info.removeEventListener(ProgressEvent.PROGRESS, handleProgress);
    }

    /**
     * A callback to receive IO_ERROR and SECURITY_ERROR events.
     */
    protected function handleError (event :ErrorEvent) :void
    {
        stoppedLoading();
        setupBrokenImage(-1, -1);
    }

    /**
     * A callback to receive PROGRESS events.
     */
    protected function handleProgress (event :ProgressEvent) :void
    {
        updateLoadingProgress(event.bytesLoaded, event.bytesTotal);
        var info :LoaderInfo = (event.target as LoaderInfo);
        try {
            updateContentDimensions(info.width, info.height);
        } catch (err :Error) {
            // an error is thrown trying to access these props before they're
            // ready
        }
    }

//    /**
//     * Handles video size known.
//     */
//    protected function handleVideoSizeKnown (event :ValueEvent) :void
//    {
//        const size :Point = Point(event.value);
//        updateContentDimensions(size.x, size.y);
//    }

//    /**
//     * Handles an error loading a video.
//     */
//    protected function handleVideoError (event :ValueEvent) :void
//    {
//        log.warning("Error loading video [cause=" + event.value + "].");
//        stoppedLoading();
//        setupBrokenImage(-1, -1);
//    }

    /**
     * Handles the INIT event for content loaded with a Loader.
     */
    protected function handleInit (event :Event) :void
    {
        _initialized = true;

        // redispatch
        dispatchEvent(event);
    }

    /**
     * Callback function to receive COMPLETE events for swfs or images.
     */
    protected function handleComplete (event :Event) :void
    {
        var info :LoaderInfo = (event.target as LoaderInfo);
        removeListeners(info);

//        trace("Loading complete: " + info.url +
//            ", childAllowsParent=" + info.childAllowsParent +
//            ", parentAllowsChild=" + info.parentAllowsChild +
//            ", sameDomain=" + info.sameDomain);

        updateContentDimensions(info.width, info.height);
        updateLoadingProgress(1, 1);
        stoppedLoading();

        // redispatch
        dispatchEvent(event);
    }

    /**
     * Handle shutting us down when we're removed. Only called if
     * setShutdownOnRemove() was enabled.
     */
    protected function handleShutdownOnRemove (event :Event) :void
    {
        shutdown();
    }

    /**
     * Configure the mask for this object.
     */
    protected function configureMask (rect :Rectangle) :void
    {
        var mask :Shape;
        if (_media.mask != null) {
            // see if the mask was added by us
            try {
                getChildIndex(_media.mask);

            } catch (argErr :ArgumentError) {
                // oy! We are not the controllers of this mask, it must
                // have been added by someone else. This probably means
                // that the _media is not a Loader, and so we should just
                // leave it alone with its custom mask.
                return;
            }

            // otherwise, it's a mask we previously configured
            mask = (_media.mask as Shape);

        } else {
            mask = new Shape();
            // the mask must be added to the display list (which is wacky)
            addChildAt(mask, 0);
            _media.mask = mask;
        }

        mask.graphics.clear();
        mask.graphics.beginFill(0xFFFFFF);
        mask.graphics.drawRect(rect.x, rect.y, rect.width, rect.height);
        mask.graphics.endFill();
    }

    /**
     * Called during loading as we figure out how big the content we're
     * loading is.
     */
    protected function updateContentDimensions (ww :int, hh :int) :void
    {
        // update our saved size, and possibly notify our container
        if (_w != ww || _h != hh) {
            _w = ww;
            _h = hh;
            contentDimensionsUpdated();
            // dispatch an event to any listeners
            dispatchEvent(new ValueEvent(SIZE_KNOWN, [ ww, hh ]));
        }
    }

    /**
     * Called when we know the true size of the content. Subclasses may override and use this
     * opportunity to do their thing, too.
     */
    protected function contentDimensionsUpdated () :void
    {
        // update the mask
        var r :Rectangle = getMaskRectangle();
        if (r != null) {
            configureMask(r);
        }
    }

    /**
     * Get the mask area, or null if no mask is needed.
     */
    protected function getMaskRectangle () :Rectangle
    {
        var maxW :int = getMaxContentWidth();
        var maxH :int = getMaxContentHeight();
        if ((maxW < int.MAX_VALUE) && (maxH < int.MAX_VALUE)) {
            // if we should do masking, then mask to the lesser of maximum and actual size
            return new Rectangle(0, 0, Math.min(maxW, _w), Math.min(maxH, _h));
        }

        return null;
    }

    /**
     * Update the graphics to indicate how much is loaded.
     */
    protected function updateLoadingProgress (soFar :Number, total :Number) :void
    {
        // nada, by default
    }

    /**
     * Called when we've started loading new media. Will not be called
     * for new media that does not require loading.
     */
    protected function startedLoading () :void
    {
        // nada
    }

    /**
     * Called when we've stopped loading, which may be as a result of
     * completion, an error while loading, or early termination.
     */
    protected function stoppedLoading () :void
    {
        // nada
    }

    /**
     * Do whatever is necessary to shut down the media.
     */
    protected function shutdownMedia () :void
    {
        if (_media is Loader) {
            try {
                var loader :Loader = (_media as Loader);
                //var url :String = loader.contentLoaderInfo.url;

                // remove any listeners
                removeListeners(loader.contentLoaderInfo);

                // dispose of media
                LoaderUtil.unload(loader);

                //var extra :String  = (url == _url) ? "" : (", _url=" + _url);
                //log.debug("Unloaded media [url=" + url + extra + "].");
            } catch (ioe :IOError) {
                log.warning("Error shutting down media", ioe);
            }

        } else if (_media is SimpleVideoDisplay) {
            var vid :SimpleVideoDisplay = SimpleVideoDisplay(_media);
//            vid.removeEventListener(VideoPlayerCodes.SIZE, handleVideoSizeKnown);
            vid.unload();
        }
    }

    /** The unaltered URL of the content we're displaying. */
    protected var _url :String;

    /** The unscaled width of our content. */
    protected var _w :int;

    /** The unscaled height of our content. */
    protected var _h :int;

    /** Either a Loader or a VideoDisplay. */
    protected var _media :DisplayObject;

    /** If we're using a Loader, true once the INIT event has been dispatched. */
    protected var _initialized :Boolean;

    /** Are we displaying an image? */
    protected var _isImage :Boolean;
}
}
