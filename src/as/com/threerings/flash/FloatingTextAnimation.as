package com.threerings.flash {

import flash.events.Event;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

public class FloatingTextAnimation extends TextField
{
    public static function create (
        text :String, duration :Number = 1000, dy :int = -10,
        format :TextFormat = null) :FloatingTextAnimation
    {
        var fta :FloatingTextAnimation = new FloatingTextAnimation();
        if (format != null) {
            fta.defaultTextFormat = format;
        }
        fta.autoSize = TextFieldAutoSize.CENTER;
        fta.text = text;
        fta.width = fta.textWidth + 5;
        fta.height = fta.textHeight + 4;

        fta.duration = duration;
        fta.dy = dy;
        return fta;
    }

    public var duration :Number;

    public var dy :Number;

    public function FloatingTextAnimation ()
    {
        addEventListener(Event.ADDED_TO_STAGE, handleAdded);
        addEventListener(Event.REMOVED_FROM_STAGE, handleRemoved);

        _anim = new AnimationAdapter(enterFrame);
    }

    override public function set x (val :Number) :void
    {
        super.x = (val - width/2);
    }

    override public function set y (val :Number) :void
    {
        _startY = val;
        super.y = (val - height/2);
    }

    protected function enterFrame (elapsed :Number) :void
    {
        var perc :Number = elapsed / duration;
        if (perc >= 1) {
            parent.removeChild(this);
            return;
        }

        alpha = 1 - perc;
        this.y = _startY + (dy * perc);
    }

    protected function handleAdded (... ignored) :void
    {
        _anim.start();
    }

    protected function handleRemoved (... ignored) :void
    {
        _anim.stop();
    }

    protected var _startY :Number;

    protected var _anim :AnimationAdapter;
}
}
