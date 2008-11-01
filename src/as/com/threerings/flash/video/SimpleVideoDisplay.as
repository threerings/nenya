//
// $Id#

package com.threerings.flash.video {

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;

import flash.events.MouseEvent;

import flash.geom.Point;

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
        _player.addEventListener(VideoPlayerCodes.ERROR, handlePlayerError);

        addChild(_player.getDisplay());

        _mask = new Shape();
        this.mask = _mask;
        addChild(_mask);

        addEventListener(MouseEvent.ROLL_OVER, handleRollOver);
        addEventListener(MouseEvent.ROLL_OUT, handleRollOut);

        _hud = new Sprite();
        _hud.addEventListener(MouseEvent.CLICK, handleClick);
        redrawHUD();

        // create the mask...
        var g :Graphics = _mask.graphics;
        g.clear();
        g.beginFill(0xFFFFFF);
        g.drawRect(0, 0, NATIVE_WIDTH, NATIVE_HEIGHT);
        g.endFill();

        g = this.graphics;
        g.beginFill(0xFF000000);
        g.drawRect(0, 0, NATIVE_WIDTH, NATIVE_HEIGHT);
        g.endFill();

        // and update the HUD location (even if not currently showing)
        _hud.x = NATIVE_WIDTH/2;
        _hud.y = NATIVE_HEIGHT/2;
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

    protected function handlePlayerError (event :ValueEvent) :void
    {
        // TODO.. maybe just redispatch
        log.warning("player error: " + event.value);
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

        if (state == VideoPlayerCodes.STATE_READY ||
                state == VideoPlayerCodes.STATE_PLAYING ||
                state == VideoPlayerCodes.STATE_PAUSED) {
            // draw a nice circle
            g.beginFill(0x333333);
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

            } else {
                g.lineStyle(0, 0, 0);
                g.beginFill(0x00FF00);
                g.moveTo(-4, -10);
                g.lineTo(4, 0);
                g.lineTo(-4, 10);
                g.lineTo(-4, -10);
                g.endFill();
            }

        } else {
            // draw something loading-like
            g.beginFill(0x000033);
            g.drawCircle(5, 5, 10);
            g.drawCircle(-5, 5, 10);
            g.drawCircle(-5, -5, 10);
            g.drawCircle(5, -5, 10);
            g.endFill();
        }
    }

    protected const log :Log = Log.getLog(this);

    protected var _player :VideoPlayer;

    protected var _hud :Sprite;
    
    /** Our mask, also defines our boundaries for clicking. */
    protected var _mask :Shape;
}
}
