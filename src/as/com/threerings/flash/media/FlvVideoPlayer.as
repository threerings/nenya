//
// $Id$

package com.threerings.flash.media {

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

import flash.media.SoundTransform;
import flash.media.Video;

import flash.net.NetConnection;
import flash.net.NetStream;

import flash.utils.Timer;

import com.threerings.util.Log;
import com.threerings.util.MethodQueue;
import com.threerings.util.ValueEvent;

public class FlvVideoPlayer extends EventDispatcher
    implements VideoPlayer
{
    public function FlvVideoPlayer (autoPlay :Boolean = true)
    {
        _autoPlay = autoPlay;
        _sizeChecker = new Timer(100);
        _sizeChecker.addEventListener(TimerEvent.TIMER, handleSizeCheck);

        _positionChecker = new Timer(250);
        _positionChecker.addEventListener(TimerEvent.TIMER, handlePositionCheck);
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
        _netStream.soundTransform = new SoundTransform(_volume);
        _netStream.addEventListener(NetStatusEvent.NET_STATUS, handleStreamNetStatus);

        _vid.attachNetStream(_netStream);
        _sizeChecker.start();

        _netStream.play(url);
        if (_autoPlay) {
            updateState(MediaPlayerCodes.STATE_PLAYING);

        } else {
            _netStream.pause();
            updateState(MediaPlayerCodes.STATE_READY);
        }
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
            updateState(MediaPlayerCodes.STATE_PLAYING);
        }
        // TODO: throw an error if _netStream == null??
    }

    // from VideoPlayer
    public function pause () :void
    {
        if (_netStream != null) {
            _netStream.pause();
            updateState(MediaPlayerCodes.STATE_PAUSED);
        }
    }

    // from VideoPlayer
    public function getDuration () :Number
    {
        return _duration;
    }

    // from VideoPlayer
    public function getPosition () :Number
    {
        if (_netStream != null) {
            return _netStream.time;
        }
        return NaN;
    }

    // from VideoPlayer
    public function seek (position :Number) :void
    {
        if (_netStream != null) {
            _netStream.seek(position);
        }
    }

    // from VideoPlayer
    public function getVolume () :Number
    {
        return _volume;
    }

    // from VideoPlayer
    public function setVolume (volume :Number) :void
    {
        _volume = Math.max(0, Math.min(1, volume));
        if (_netStream != null) {
            _netStream.soundTransform = new SoundTransform(_volume);
        }
    }

    // from VideoPlayer
    public function getMetadata () :Object
    {
        return _metadata;
    }

    // from VideoPlayer
    public function unload () :void
    {
        _metadata = null;
        _duration = NaN;
        _gotDurationFromMetadata = false;
        _sizeChecker.reset();
        _positionChecker.reset();
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
    protected function handleSizeCheck (event :TimerEvent) :void
    {
        if (_vid.videoWidth == 0 || _vid.videoHeight == 0) {
            return; // not known yet!
        }

        // stop the checker timer
        _sizeChecker.stop();
        _size = new Point(_vid.videoWidth, _vid.videoHeight);

        // set up the width/height
        _vid.width = _size.x;
        _vid.height = _size.y;

        dispatchEvent(new ValueEvent(MediaPlayerCodes.SIZE, _size.clone()));
    }

    protected function handlePositionCheck (event :TimerEvent = null) :void
    {
        const pos :Number = getPosition();
        if (!_gotDurationFromMetadata && (isNaN(_duration) || (pos > _duration))) {
            _duration = pos;
            dispatchEvent(new ValueEvent(MediaPlayerCodes.DURATION, _duration));
        }
        dispatchEvent(new ValueEvent(MediaPlayerCodes.POSITION, pos));
    }

    protected function handleNetStatus (event :NetStatusEvent) :void
    {
        var info :Object = event.info;
        if ("error" == info.level) {
            log.warning("NetStatus error: " + info.code);
            dispatchEvent(new ValueEvent(MediaPlayerCodes.ERROR, info.code));
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
//        log.debug("NetStatus", "level", event.info.level, "code", event.info.code);

        switch (event.info.code) {
        case "NetStream.Play.Stop":
            if (_state == MediaPlayerCodes.STATE_PLAYING) {
                handlePositionCheck(); // if we never got metadata, retrieve final position as dur
                // rewind to the beginning
                _netStream.seek(0);
                _netStream.pause();
                updateState(MediaPlayerCodes.STATE_STOPPED);
            }
            break;

        case "NetStream.Seek.Notify":
            MethodQueue.callLater(handlePositionCheck);
            break;
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
        dispatchEvent(new ValueEvent(MediaPlayerCodes.ERROR, event.text));
    }

    /**
     * Called when metadata (if any) is found in the video stream.
     */
    protected function handleMetaData (info :Object) :void
    {
        if ("duration" in info) {
            _duration = Number(info.duration);
            _gotDurationFromMetadata = true;
            if (!isNaN(_duration)) {
                dispatchEvent(new ValueEvent(MediaPlayerCodes.DURATION, _duration));
            }
        }

        _metadata = info;
        dispatchEvent(new ValueEvent(MediaPlayerCodes.METADATA, info));
    }

    protected function updateState (newState :int) :void
    {
        const oldState :int = _state;
        _state = newState;

        if (_state == MediaPlayerCodes.STATE_PLAYING) {
            _positionChecker.start();

        } else {
            _positionChecker.reset();
            if (oldState == MediaPlayerCodes.STATE_PLAYING) {
                handlePositionCheck();
            }
        }

        dispatchEvent(new ValueEvent(MediaPlayerCodes.STATE, newState));
    }

    protected const log :Log = Log.getLog(this);

    protected var _autoPlay :Boolean;
    
    protected var _vid :Video = new Video();

    protected var _netCon :NetConnection;

    protected var _netStream :NetStream;

    protected var _state :int = MediaPlayerCodes.STATE_UNREADY;

    /** Our size, null until known. */
    protected var _size :Point;

    protected var _duration :Number = NaN;

    protected var _gotDurationFromMetadata :Boolean;

    protected var _volume :Number = 1;

    protected var _metadata :Object;

    /** Checks the video every 100ms to see if the dimensions are now know.
     * Yes, this is how to do it. We could trigger on ENTER_FRAME, but then
     * we may not know the dimensions unless we're added on the display list,
     * and we want this to work in the general case. */
    protected var _sizeChecker :Timer;

    protected var _positionChecker :Timer;
}
}
