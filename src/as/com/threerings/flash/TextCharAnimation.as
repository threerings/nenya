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

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

import flash.utils.getTimer; // function import

public class TextCharAnimation extends Sprite
    implements Animation
{
    public function TextCharAnimation (text :String, fn :Function, textArgs :Object)
    {
        _fn = fn;

        var alignment :Sprite = new Sprite();

        var w :Number = 0;
        var h :Number = 0;
        var tf :TextField;
        for (var ii :int = 0; ii < text.length; ii++) {
            tf = TextFieldUtil.createField(text.charAt(ii), textArgs);

            tf.x = w;
            w += tf.width;
            h = Math.max(h, tf.height);

            alignment.addChild(tf);
            _texts.push(tf);
        }

        alignment.x = -(w/2);
        alignment.y = -(h/2);
        addChild(alignment);

        AnimationManager.addDisplayAnimation(this);
    }

    // from Animation
    public function updateAnimation (elapsed :Number) :void
    {
        for (var ii :int = 0; ii < _texts.length; ii++) {
            _texts[ii].y = _fn(elapsed, ii);
        }
    }

    protected var _texts :Array = [];

    protected var _fn :Function;
}
}
