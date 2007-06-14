//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2007 Three Rings Design, Inc., All Rights Reserved
// http://www.threerings.net/code/nenya/
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

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
        tf.selectable = false;
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
            parent.removeChild(this);
            return;
        }

        alpha = 1 - perc;
        super.y = _startY + (dy * perc);
    }

    protected var _startY :Number;
}
}
