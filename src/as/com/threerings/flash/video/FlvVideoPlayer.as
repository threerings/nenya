//
// $Id$

package com.threerings.flash.video {

import flash.display.DisplayObject;

import flash.events.AsyncErrorEvent;
import flash.events.IOErrorEvent;
import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.EventDispatcher;
import flash.events.NetStatusEvent;
import flash.events.SecurityErrorEvent;
import flash.events.TimerEvent;

import flash.geom.Point;

import flash.media.Video;

import flash.net.NetConnection;
import flash.net.NetStream;

import flash.utils.Timer;

import com.threerings.util.Log;
import com.threerings.util.ValueEvent;

public class FlvVideoPlayer extends EventDispatcher
    implements VideoPlayer
{
    public function FlvVideoPlayer ()
    {
        _videoChecker = new Timer(100);
        _videoChecker.addEventListener(TimerEvent.TIMER, handleVideoCheck);
    }

    /**
     * Start playing a video!
     */
    public function load (url :String) :void
    {
        _netCon = new NetConnection();
        _netCon.addEventListener(NetStatusEvent.NET_STATUS, handleNetStatus);

        // error handlers
        _netCon.addEventListener(AsyncErrorEvent.ASYNC_ERROR, handleAsyncError);
        _netCon.addEventListener(IOErrorEvent.IO_ERROR, handleIOError);
        _netCon.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleSecurityError);

        _netCon.connect(null);
        _netStream = new NetStream(_netCon);
        // pass in refs to some of our protected methods
        _netStream.client = {
            onMetaData: handleMetaData
        };
        _netStream.addEventListener(NetStatusEvent.NET_STATUS, handleStreamNetStatus);

        _vid.attachNetStream(_netStream);
        _videoChecker.start();

        _netStream.play(url);
        _netStream.pause(); // TODO Does this work?
        updateState(VideoPlayerCodes.STATE_READY);
    }

    // from VideoPlayer
    public function getDisplay () :DisplayObject
    {
        return _vid;
    }

    // from VideoPlayer
    public function getState () :int
    {
        return _state;
    }

    // from VideoPlayer
    public function getSize () :Point
    {
        return _size.clone();
    }

    // from VideoPlayer
    public function play () :void
    {
        if (_netStream != null) {
            _netStream.resume();
            updateState(VideoPlayerCodes.STATE_PLAYING);
        }
        // TODO: throw an error if _netStream == null??
    }

    // from VideoPlayer
    public function pause () :void
    {
        if (_netStream != null) {
            _netStream.pause();
            updateState(VideoPlayerCodes.STATE_PAUSED);
        }
    }

    // from VideoPlayer
    public function getDuration () :Number
    {
        return NaN; // TODO
    }

    // from VideoPlayer
    public function getPosition () :Number
    {
        return NaN; // TODO
    }

    // from VideoPlayer
    public function seek (position :Number) :void
    {
        // TODO
    }

    // from VideoPlayer
    public function getVolume () :Number
    {
        return 1; // TODO
    }

    // from VideoPlayer
    public function setVolume (volume :Number) :void
    {
        // TODO
    }

    // from VideoPlayer
    public function unload () :void
    {
        _videoChecker.reset();
        _vid.attachNetStream(null);

        if (_netStream != null) {
            _netStream.close();
            _netStream.removeEventListener(NetStatusEvent.NET_STATUS, handleStreamNetStatus);
            _netStream = null;
        }
        if (_netCon != null) {
            _netCon.close();
            _netCon.removeEventListener(NetStatusEvent.NET_STATUS, handleNetStatus);
            _netCon.removeEventListener(AsyncErrorEvent.ASYNC_ERROR, handleAsyncError);
            _netCon.removeEventListener(IOErrorEvent.IO_ERROR, handleIOError);
            _netCon.removeEventListener(SecurityErrorEvent.SECURITY_ERROR, handleSecurityError);
            _netCon = null;
        }
    }

    /**
     * Check to see if we now know the dimensions of the video.
     */
    protected function handleVideoCheck (event :TimerEvent) :void
    {
        if (_vid.videoWidth == 0 || _vid.videoHeight == 0) {
            return; // not known yet!
        }

        // stop the checker timer
        _videoChecker.stop();
        _size = new Point(_vid.videoWidth, _vid.videoHeight);

        // set up the width/height
        _vid.width = _size.x;
        _vid.height = _size.y;

        log.info("=================> size known: " + _size);
        dispatchEvent(new ValueEvent(VideoPlayerCodes.SIZE, _size.clone()));
    }

    protected function handleNetStatus (event :NetStatusEvent) :void
    {
        var info :Object = event.info;
        if ("error" == info.level) {
            log.warning("NetStatus error: " + info.code);
            dispatchEvent(new ValueEvent(VideoPlayerCodes.ERROR, info.code));
            return;
        }
        // else info.level == "status"
        switch (info.code) {
        case "NetConnection.Connect.Success":
        case "NetConnection.Connect.Closed":
            // these status events we ignore
            break;

        default:
            log.info("NetStatus status: " + info.code);
            break;
        }
    }

    protected function handleStreamNetStatus (event :NetStatusEvent) :void
    {
        if (event.info.code == "NetStream.Play.Stop") {
            _netStream.seek(0);
            _netStream.pause();
            updateState(VideoPlayerCodes.STATE_PAUSED);
        }
    }

    protected function handleAsyncError (event :AsyncErrorEvent) :void
    {
        redispatchError(event);
    }

    protected function handleIOError (event :IOErrorEvent) :void
    {
        redispatchError(event);
    }

    protected function handleSecurityError (event :SecurityErrorEvent) :void
    {
        redispatchError(event);
    }

    /**
     * Redispatch some error we received to our listeners.
     */
    protected function redispatchError (event :ErrorEvent) :void
    {
        log.warning("got video error: " + event);
        dispatchEvent(new ValueEvent(VideoPlayerCodes.ERROR, event.text));
    }

    /**
     * Called when metadata (if any) is found in the video stream.
     */
    protected function handleMetaData (info :Object) :void
    {
        log.info("Got video metadata:");
        for (var n :String in info) {
            log.info("    " + n + ": " + info[n]);
        }

        // TODO: total duration is info.duration
    }

    protected function updateState (newState :int) :void
    {
        _state = newState;
        dispatchEvent(new ValueEvent(VideoPlayerCodes.STATE, newState));
    }

    protected const log :Log = Log.getLog(this);
    
    protected var _vid :Video = new Video();

    protected var _netCon :NetConnection;

    protected var _netStream :NetStream;

    protected var _state :int = VideoPlayerCodes.STATE_UNREADY;

    /** Our size, null until known. */
    protected var _size :Point;

    /** Checks the video every 100ms to see if the dimensions are now know.
     * Yes, this is how to do it. We could trigger on ENTER_FRAME, but then
     * we may not know the dimensions unless we're added on the display list,
     * and we want this to work in the general case. */
    protected var _videoChecker :Timer;
}
}
