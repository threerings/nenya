//
// $Id: SoundPlayer.as 10500 2008-08-07 19:14:38Z mdb $

package com.threerings.flash.media {

import flash.errors.IOError;

import flash.events.Event;
import flash.events.EventDispatcher;
import flash.events.IOErrorEvent;
import flash.events.ProgressEvent;
import flash.events.TimerEvent;

import flash.media.Sound;
import flash.media.SoundChannel;
import flash.media.SoundLoaderContext;
import flash.media.SoundTransform;

import flash.net.URLRequest;

import flash.utils.Timer;

import com.threerings.util.Log;
import com.threerings.util.ValueEvent;

public class Mp3AudioPlayer extends EventDispatcher
    implements AudioPlayer
{
    public function Mp3AudioPlayer (loop :Boolean = false)
    {
        _loop = loop;

        _positionChecker = new Timer(250);
        _positionChecker.addEventListener(TimerEvent.TIMER, handlePositionCheck);
    }

    /**
     * Load and immediately start playing some audio!
     */
    public function load (url :String) :void
    {
        _isComplete = false;
        _lastPosition = 0;
        _sound = new Sound();
        _sound.addEventListener(Event.ID3, handleId3);
        _sound.addEventListener(IOErrorEvent.IO_ERROR, handleError);
        _sound.addEventListener(Event.COMPLETE, handleLoadingComplete);
        _sound.load(new URLRequest(url), new SoundLoaderContext(1000, true));
        play();
    }

    // from AudioPlayer
    public function getState () :int
    {
        return _state;
    }

    // from AudioPlayer
    public function play () :void
    {
        play0();
        updateState(
            (_chan != null) ? MediaPlayerCodes.STATE_PLAYING : MediaPlayerCodes.STATE_PAUSED);
    }

    // from AudioPlayer
    public function pause () :void
    {
        handlePositionCheck(); // update _lastPosition and dispatch an event
        pause0();
        updateState(MediaPlayerCodes.STATE_PAUSED);
    }

    // from AudioPlayer
    public function getDuration () :Number
    {
        if (_sound == null) {
            return NaN;
        }
        return _sound.length / 1000;
    }

    // from AudioPlayer
    public function getPosition () :Number
    {
        if (_chan == null) {
            return _lastPosition;
        }
        // we mod by the duration because when we loop the position is ever-growing. hahah!
        return (_chan.position / 1000);
    }

    // from AudioPlayer
    public function seek (position :Number) :void
    {
        _lastPosition = position;
        if (_state == MediaPlayerCodes.STATE_PLAYING) {
            pause0();
            play0();

        } else {
            handlePositionCheck();
        }
    }

    // from AudioPlayer
    public function setVolume (volume :Number) :void
    {
        _volume = Math.max(0, Math.min(1, volume));
        if (_chan != null) {
            _chan.soundTransform = new SoundTransform(_volume);
        }
    }

    // from AudioPlayer
    public function getVolume () :Number
    {
        return _volume;
    }

    // from AudioPlayer
    public function getMetadata () :Object
    {
        return (_sound == null) ? null : _sound.id3;
    }

    // from AudioPlayer
    public function unload () :void
    {
        pause0();
        if (_sound != null) {
            try {
                _sound.close();
            } catch (err :IOError) {
                // ignore
            }
        }
        _positionChecker.reset();
        _lastPosition = NaN;
    }

    /**
     * Play without updating the current state.
     */
    protected function play0 () :void
    {
        pause0();
        _chan = _sound.play(_lastPosition * 1000, 0, new SoundTransform(_volume));
        if (_chan != null) {
            _chan.addEventListener(Event.SOUND_COMPLETE, handlePlaybackComplete);

        } else {
            Log.getLog(this).warning("All sound channels are in use; " +
                "unable to play sound.");
        }
    }

    /**
     * Pause without saving the position or updating the state.
     */
    protected function pause0 () :void
    {
        if (_chan != null) {
            _chan.stop();
            _chan = null;
        }
    }

    protected function handlePositionCheck (event :TimerEvent = null) :void
    {
        if (!_isComplete) {
            dispatchEvent(new ValueEvent(MediaPlayerCodes.DURATION, getDuration()));
        }
        _lastPosition = getPosition();
        dispatchEvent(new ValueEvent(MediaPlayerCodes.POSITION, _lastPosition));
    }

    protected function updateState (newState :int) :void
    {
        _state = newState;

        if (_state == MediaPlayerCodes.STATE_PLAYING) {
            _positionChecker.start();

        } else {
            _positionChecker.reset();
            // pause() ends up checking the position one last time before dispatching state..
        }

        dispatchEvent(new ValueEvent(MediaPlayerCodes.STATE, newState));
    }

    protected function handleId3 (event :Event) :void
    {
        dispatchEvent(new ValueEvent(MediaPlayerCodes.METADATA, _sound.id3));
    }

    /**
     */
    protected function handleLoadingComplete (event :Event) :void
    {
        handlePositionCheck(); // also updates duration
        _isComplete = true;
    }

    protected function handlePlaybackComplete (event :Event) :void
    {
        _lastPosition = 0;
        pause0();
        handlePositionCheck();
        if (_loop) {
            play0();
        } else {
            updateState(MediaPlayerCodes.STATE_PAUSED);
        }
    }

    protected function handleError (event :IOErrorEvent) :void
    {
        dispatchEvent(new ValueEvent(MediaPlayerCodes.ERROR, event.text));
    }

    protected var _sound :Sound;

    protected var _chan :SoundChannel;

    protected var _loop :Boolean;

    protected var _state :int = MediaPlayerCodes.STATE_UNREADY;

    protected var _volume :Number = 1;

    protected var _isComplete :Boolean;

    protected var _lastPosition :Number = NaN;

    protected var _positionChecker :Timer;
}
}
