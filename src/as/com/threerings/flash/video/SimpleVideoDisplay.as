//
// $Id#

package com.threerings.flash.video {

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.MouseEvent;

import flash.geom.Point;
import flash.geom.Rectangle;

import com.threerings.util.Log;
import com.threerings.util.ValueEvent;

/**
 *
 * @eventType com.threerings.flash.video.VideoPlayerCodes.SIZE
 */
[Event(name="size", type="com.threerings.util.ValueEvent")]

/**
 * A simple video display.
 */
public class SimpleVideoDisplay extends Sprite
{
    public static const NATIVE_WIDTH :int = 320;

    public static const NATIVE_HEIGHT :int = 240;

    /**
     * Create a video displayer.
     */
    public function SimpleVideoDisplay (player :VideoPlayer)
    {
        _player = player;
        _player.addEventListener(VideoPlayerCodes.STATE, handlePlayerState);
        _player.addEventListener(VideoPlayerCodes.SIZE, handlePlayerSize);
        _player.addEventListener(VideoPlayerCodes.DURATION, handlePlayerDuration);
        _player.addEventListener(VideoPlayerCodes.POSITION, handlePlayerPosition);
        _player.addEventListener(VideoPlayerCodes.ERROR, handlePlayerError);

        addChild(_player.getDisplay());

        var masker :Shape = new Shape();
        this.mask = masker;
        addChild(masker);

        addEventListener(MouseEvent.ROLL_OVER, handleRollOver);
        addEventListener(MouseEvent.ROLL_OUT, handleRollOut);

        _hud = new Sprite();
        _hud.addEventListener(MouseEvent.CLICK, handleClick);
        redrawHUD();

        // create the mask...
        var g :Graphics = masker.graphics;
        g.clear();
        g.beginFill(0xFFFFFF);
        g.drawRect(0, 0, NATIVE_WIDTH, NATIVE_HEIGHT);
        g.endFill();

        // and update the HUD location (even if not currently showing)
        _hud.x = NATIVE_WIDTH/2;
        _hud.y = NATIVE_HEIGHT/2;

        _track = new Sprite();
        _track.x = PAD - _hud.x;
        _track.y = NATIVE_HEIGHT - PAD - TRACK_HEIGHT - _hud.y;
        g = _track.graphics;
        g.beginFill(0x000000, .7);
        g.drawRect(0, 0, TRACK_WIDTH, TRACK_HEIGHT);
        g.endFill();
        g.lineStyle(2, 0xFFFFFF);
        g.drawRect(0, 0, TRACK_WIDTH, TRACK_HEIGHT);
        // _track is not added to _hud until we know the duration

        const trackMask :Shape = new Shape();
        g = trackMask.graphics;
        g.beginFill(0xFFFFFF);
        g.drawRect(0, 0, TRACK_WIDTH, TRACK_HEIGHT);
        _track.addChild(trackMask);
        _track.mask = trackMask;

        _knob = new Sprite();
        _knob.y = TRACK_HEIGHT / 2;
        g = _knob.graphics;
        g.lineStyle(1, 0xFFFFFF);
        g.beginFill(0x000099);
        g.drawCircle(0, 0, TRACK_HEIGHT/2 - 1);
        // _knob is not added to _track until we know the position

        _track.addEventListener(MouseEvent.CLICK, handleTrackClick);
        _knob.addEventListener(MouseEvent.MOUSE_DOWN, handleKnobDown);
    }

    override public function get width () :Number
    {
        return NATIVE_WIDTH;
    }

    override public function get height () :Number
    {
        return NATIVE_HEIGHT;
    }

    /**
     * Stop playing our video.
     */
    public function unload () :void
    {
        _player.unload();
    }

    protected function handleRollOver (event :MouseEvent) :void
    {
        showHUD(true);
    }

    protected function handleRollOut (event :MouseEvent) :void
    {
        showHUD(false);
    }

    protected function handleClick (event :MouseEvent) :void
    {
        // the buck stops here!
        event.stopImmediatePropagation();

        switch (_player.getState()) {
        case VideoPlayerCodes.STATE_READY:
        case VideoPlayerCodes.STATE_PAUSED:
            _player.play();
            break;

        case VideoPlayerCodes.STATE_PLAYING:
            _player.pause();
            break;

        default:
            log.info("Click while player in unhandled state: " + _player.getState());
            break;
        }
    }

    protected function handleTrackClick (event :MouseEvent) :void
    {
        event.stopImmediatePropagation();

        adjustSeek(event.localX);
    }

    protected function handleKnobDown (event :MouseEvent) :void
    {
        event.stopImmediatePropagation();

        _dragging = true;
        _knob.startDrag(false, new Rectangle(0, TRACK_HEIGHT/2, TRACK_WIDTH, 0));
        addEventListener(Event.ENTER_FRAME, handleKnobSeekCheck);
        addEventListener(MouseEvent.MOUSE_UP, handleKnobUp);
    }

    protected function handleKnobUp (event :MouseEvent) :void
    {
        event.stopImmediatePropagation();

        _dragging = false;
        _knob.stopDrag();
        removeEventListener(Event.ENTER_FRAME, handleKnobSeekCheck);
        removeEventListener(MouseEvent.MOUSE_UP, handleKnobUp);
        handleKnobSeekCheck(null);
    }

    protected function handleKnobSeekCheck (event :Event) :void
    {
        adjustSeek(_knob.x);
    }

    protected function handlePlayerState (event :ValueEvent) :void
    {
        redrawHUD();
    }

    protected function handlePlayerSize (event :ValueEvent) :void
    {
        const size :Point = Point(event.value);

        const disp :DisplayObject = _player.getDisplay();
        // TODO: siggggghhhhh
//        disp.scaleX = NATIVE_WIDTH / size.x;
//        disp.scaleY = NATIVE_HEIGHT / size.y;
        disp.width = NATIVE_WIDTH;
        disp.height = NATIVE_HEIGHT;

        // and, we redispatch
        dispatchEvent(event);
    }

    protected function handlePlayerDuration (event :ValueEvent) :void
    {
// TODO: temporarily disabled
//        _hud.addChild(_track);
    }

    protected function handlePlayerPosition (event :ValueEvent) :void
    {
        if (_dragging) {
            return;
        }
        _lastKnobX = int.MIN_VALUE;
        const pos :Number = Number(event.value);
        _knob.x = (pos / _player.getDuration()) * TRACK_WIDTH;
        if (_knob.parent == null) {
            _track.addChild(_knob);
        }
    }

    protected function handlePlayerError (event :ValueEvent) :void
    {
        // TODO.. maybe just redispatch
        log.warning("player error: " + event.value);
    }

    protected function adjustSeek (trackX :Number) :void
    {
        // see if we can seek to a position
        const dur :Number = _player.getDuration();
        if (isNaN(dur)) {
            log.debug("Duration is NaN, not seeking.");
            return;
        }
        if (trackX == _lastKnobX) {
            return;
        }
        _lastKnobX = trackX;
        var perc :Number = trackX / TRACK_WIDTH;
        perc = Math.max(0, Math.min(1, perc));
        log.debug("Seek", "x", trackX, "perc", perc, "pos", (perc * dur));
        _player.seek(perc * dur);
    }

    protected function showHUD (show :Boolean) :void
    {
        if (show == (_hud.parent == null)) {
            if (show) {
                addChild(_hud);
            } else {
                removeChild(_hud);
            }
        }
    }

    protected function redrawHUD () :void
    {
        const state :int = _player.getState();

        const g :Graphics = _hud.graphics;
        g.clear();

        if ((state != VideoPlayerCodes.STATE_READY) &&
                (state != VideoPlayerCodes.STATE_PLAYING) &&
                (state != VideoPlayerCodes.STATE_PAUSED)) {
            // draw something loading-like
            // TODO animated swf
            g.beginFill(0x000033);
            g.drawCircle(5, 5, 10);
            g.drawCircle(-5, 5, 10);
            g.drawCircle(-5, -5, 10);
            g.drawCircle(5, -5, 10);
            g.endFill();
            return;
        }

        // draw a nice circle
        g.beginFill(0x000000, .7);
        g.drawCircle(0, 0, 20);
        g.endFill();
        g.lineStyle(2, 0xFFFFFF);
        g.drawCircle(0, 0, 20);

        if (state == VideoPlayerCodes.STATE_PLAYING) {
            g.lineStyle(2, 0x00FF00);
            g.moveTo(-4, -10);
            g.lineTo(-4, 10);
            g.moveTo(4, -10);
            g.lineTo(4, 10);

        } else { // READY or PAUSED
            g.lineStyle(0, 0, 0);
            g.beginFill(0x00FF00);
            g.moveTo(-4, -10);
            g.lineTo(4, 0);
            g.lineTo(-4, 10);
            g.lineTo(-4, -10);
            g.endFill();
        }
    }

    protected const log :Log = Log.getLog(this);

    protected var _player :VideoPlayer;

    protected var _hud :Sprite;

    protected var _track :Sprite;

    /** The seek selector knob, positioned on the hud. */
    protected var _knob :Sprite;

    protected var _lastKnobX :int = int.MIN_VALUE;

    protected var _dragging :Boolean;
    
    protected static const PAD :int = 10;
    protected static const TRACK_HEIGHT :int = 20;
    protected static const TRACK_WIDTH :int = NATIVE_WIDTH - (PAD * 2);
}
}
