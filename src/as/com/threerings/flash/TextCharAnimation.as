package com.threerings.flash {

import flash.display.Sprite;

import flash.events.Event;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

import flash.utils.getTimer; // function import

public class TextCharAnimation extends Sprite
{
    public function TextCharAnimation (text :String, fn :Function, format :TextFormat)
    {
        _fn = fn;

        var alignment :Sprite = new Sprite();

        var w :Number = 0;
        var h :Number = 0;
        var tf :TextField;
        for (var ii :int = 0; ii < text.length; ii++) {
            tf = createTextField(format);
            tf.autoSize = TextFieldAutoSize.CENTER;
            tf.text = text.charAt(ii);
            tf.width = tf.textWidth + 5;
            tf.height = tf.textHeight + 5;

            tf.x = w;
            w += tf.width;
            h = Math.max(h, tf.height);

            alignment.addChild(tf);
            _texts.push(tf);
        }

        alignment.x = -(w/2);
        alignment.y = -(h/2);
        addChild(alignment);

        addEventListener(Event.ADDED_TO_STAGE, handleAdded);
        addEventListener(Event.REMOVED_FROM_STAGE, handleRemoved);
    }

    protected function createTextField (format :TextFormat) :TextField
    {
        var tf :TextField = new TextField();
        tf.defaultTextFormat = format;
        return tf;
    }

    protected function handleAdded (... ignored) :void
    {
        // TODO: use animation stuff instead?
        addEventListener(Event.ENTER_FRAME, handleFrame);
        _startStamp = getTimer();
        handleFrame(); // update immediately...
    }

    protected function handleRemoved (... ignored) :void
    {
        removeEventListener(Event.ENTER_FRAME, handleFrame);
    }

    protected function handleFrame (... ignored) :void
    {
        var elapsed :Number = getTimer() - _startStamp;
        for (var ii :int = 0; ii < _texts.length; ii++) {
            _texts[ii].y = _fn(elapsed, ii);
        }
    }

    protected var _startStamp :Number;

    protected var _texts :Array = [];

    protected var _fn :Function;
}
}
