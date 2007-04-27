package com.threerings.flash {

import flash.display.Sprite;

import flash.events.Event;

import flash.filters.GlowFilter;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

public class FloatingTextAnimation extends Sprite
    implements Animation
{
/*
    public static function create (
        text :String, duration :Number = 1000, dy :int = -10,
        size :int = 18, font :String = "Arial", bold :Boolean = true,
        color :uint = 0x000000, borderColor :uint = 0xFFFFFF) :FloatingTextAnimation
    {
        var fta :FloatingTextAnimation = new FloatingTextAnimation();
        var format :TextFormat = new TextFormat();
        format.size = size;
        format.font = font;
        format.bold = bold;
        format.color = color;
        fta.defaultTextFormat = format;
        fta.autoSize = TextFieldAutoSize.CENTER;
        fta.text = text;
        fta.width = fta.textWidth + 5;
        fta.height = fta.textHeight + 4;

        fta.duration = duration;
        fta.dy = dy;

        fta.filters = [ new GlowFilter(borderColor, 1, 2, 2, 255) ];

        return fta;
    }
    */

    public var duration :Number;

    public var dy :Number;

    public function FloatingTextAnimation (
        text :String, textArgs :Object = null, duration :Number = 1000, dy :int = -10)
    {
        var tf :TextField = TextFieldUtil.createField(text, textArgs);
        tf.x = -(tf.width/2)
        tf.y = -(tf.height/2);
        addChild(tf);

        this.duration = duration;
        this.dy = dy;

        AnimationManager.addDisplayAnimation(this);
    }

    override public function set y (val :Number) :void
    {
        super.y = _startY = val;
    }

    // from Animation
    public function updateAnimation (elapsed :Number) :void
    {
        var perc :Number = elapsed / duration;
        if (perc >= 1) {
            AnimationManager.removeDisplayAnimation(this);
            return;
        }

        alpha = 1 - perc;
        super.y = _startY + (dy * perc);
    }

    protected var _startY :Number;
}
}
