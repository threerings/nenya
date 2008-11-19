//
// $Id#

package com.threerings.flash.media {

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;

import flash.events.MouseEvent;

import flash.geom.Point;

import com.threerings.util.ValueEvent;

/**
 * An extremely simple video display. Click to pause/play.
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
        _player.addEventListener(MediaPlayerCodes.SIZE, handlePlayerSize);

        addChild(_player.getDisplay());

        var masker :Shape = new Shape();
        this.mask = masker;
        addChild(masker);

        // create the mask...
        var g :Graphics = masker.graphics;
        g.clear();
        g.beginFill(0xFFFFFF);
        g.drawRect(0, 0, NATIVE_WIDTH, NATIVE_HEIGHT);
        g.endFill();

        addEventListener(MouseEvent.CLICK, handleClick);
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

    protected function handleClick (event :MouseEvent) :void
    {
        // the buck stops here!
        event.stopImmediatePropagation();

        switch (_player.getState()) {
        case MediaPlayerCodes.STATE_READY:
        case MediaPlayerCodes.STATE_PAUSED:
            _player.play();
            break;

        case MediaPlayerCodes.STATE_PLAYING:
            _player.pause();
            break;

        default:
            trace("Click while player in unhandled state: " + _player.getState());
            break;
        }
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

    protected var _player :VideoPlayer;
}
}
