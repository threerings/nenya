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

import flash.display.Shape;
import flash.display.SimpleButton;
import flash.display.Sprite;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

/**
 * Displays a simple button with a rounded rectangle or rectangle for a face.
 */
public class SimpleTextButton extends SimpleButton
{
    public function SimpleTextButton (
        text :String, rounded :Boolean = true, foreground :uint = 0x003366,
        background :uint = 0x6699CC, highlight :uint = 0x0066FF, padding :int = 5,
        textFormat :TextFormat = null)
    {
        upState = makeFace(text, rounded, foreground, background, padding, textFormat);
        overState = makeFace(text, rounded, highlight, background, padding, textFormat);
        downState = makeFace(text, rounded, background, highlight, padding, textFormat);
        hitTestState = upState;
    }

    protected function makeFace (
        text :String, rounded :Boolean, foreground :uint, background :uint,
        padding :int, textFormat :TextFormat) :Sprite
    {
        var face :Sprite = new Sprite();

        // create the label so that we can measure its size
        var label :TextField = new TextField();
        if (textFormat) {
            label.defaultTextFormat = textFormat;
        }
        label.text = text;
        label.textColor = foreground;
        label.autoSize = TextFieldAutoSize.LEFT;
        face.addChild(label);

        var w :Number = label.width + 2 * padding;
        var h :Number = label.height + 2 * padding;

        // draw our button background (and outline)
        face.graphics.beginFill(background);
        face.graphics.lineStyle(1, foreground);
        if (rounded) {
            face.graphics.drawRoundRect(0, 0, w, h, padding, padding);
        } else {
            face.graphics.drawRect(0, 0, w, h);
        }
        face.graphics.endFill();

        label.x = padding;
        label.y = padding;

        return face;
    }
}
}
