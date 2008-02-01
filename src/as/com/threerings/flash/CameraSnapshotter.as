//
// $Id$

package com.threerings.flash {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.MouseEvent;

import flash.media.Camera;
import flash.media.Video;

import flash.utils.ByteArray;

import com.threerings.util.ValueEvent;

// TODO: use PreferredCamera?
public class CameraSnapshotter extends Sprite
{
    public function CameraSnapshotter (width :int, height :int)
    {
        _video = new Video(width, height);
        _video.attachCamera(Camera.getCamera());
        _videoInterface.addChild(_video);

        var btn :SimpleTextButton;
        btn = new SimpleTextButton("snapshot");
        btn.addEventListener(MouseEvent.CLICK, handleTakeSnapshot);
        btn.y = height;
        _videoInterface.addChild(btn);

        btn = new SimpleTextButton("cancel");
        btn.addEventListener(MouseEvent.CLICK, handleCancel);
        btn.x = width - btn.width;
        btn.y = height;
        _videoInterface.addChild(btn);

        _bmp = new BitmapData(width, height, false);
        _previewInterface.addChild(new Bitmap(_bmp));

        btn = new SimpleTextButton("accept");
        btn.addEventListener(MouseEvent.CLICK, handleAccept);
        btn.y = height;
        _previewInterface.addChild(btn);

        btn = new SimpleTextButton("clear");
        btn.addEventListener(MouseEvent.CLICK, handleClear);
        btn.x = width - btn.width;
        btn.y = height;
        _previewInterface.addChild(btn);

        addChild(_videoInterface);
    }

    public static function hasCamera () :Boolean
    {
        var names :Array = Camera.names;
        return (names != null && names.length > 0);
    }

    protected function handleTakeSnapshot (event :MouseEvent) :void
    {
        _bmp.draw(_video);
        removeChild(_videoInterface);
        addChild(_previewInterface);
    }

    protected function handleClear (event :MouseEvent) :void
    {
        removeChild(_previewInterface);
        addChild(_videoInterface);
    }

    protected function handleCancel (event :MouseEvent) :void
    {
        dispatchEvent(new ValueEvent(Event.COMPLETE, null));
    }

    protected function handleAccept (event :MouseEvent) :void
    {
        dispatchEvent(new ValueEvent(Event.COMPLETE, _bmp));
    }

    protected var _video :Video;

    protected var _bmp :BitmapData;

    protected var _videoInterface :Sprite = new Sprite();

    protected var _previewInterface :Sprite = new Sprite();
}
}
