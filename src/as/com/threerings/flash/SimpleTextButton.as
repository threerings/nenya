//
// $Id$

package com.threerings.flash {

import flash.display.Shape;
import flash.display.SimpleButton;
import flash.display.Sprite;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;

/**
 * Displays a simple button with a rounded rectangle or rectangle for a face.
 */
public class SimpleTextButton extends SimpleButton
{
    public function SimpleTextButton (
        text :String, rounded :Boolean = true, foreground :uint = 0x003366,
        background :uint = 0x6699CC, highlight :uint = 0x0066FF, padding :int = 5)
    {
        upState = makeFace(text, rounded, foreground, background, padding);
        overState = makeFace(text, rounded, highlight, background, padding);
        downState = makeFace(text, rounded, background, highlight, padding);
        hitTestState = upState;
    }

    protected function makeFace (
        text :String, rounded :Boolean, foreground :uint, background :uint, padding :int) :Sprite
    {
        var face :Sprite = new Sprite();

        // create the label so that we can measure its size
        var label :TextField = new TextField();
        label.text = text;
        label.textColor = foreground;
        label.autoSize = TextFieldAutoSize.LEFT;

        var w :Number = label.textWidth + 2 * padding;
        var h :Number = label.textHeight + 2 * padding;

        // create our button background (and outline)
        var button :Shape = new Shape();
        button.graphics.beginFill(background);
        if (rounded) {
            button.graphics.drawRoundRect(0, 0, w, h, padding, padding);
        } else {
            button.graphics.drawRect(0, 0, w, h);
        }
        button.graphics.endFill();
        button.graphics.lineStyle(1, foreground);
        if (rounded) {
            button.graphics.drawRoundRect(0, 0, w, h, padding, padding);
        } else {
            button.graphics.drawRect(0, 0, w, h);
        }

        face.addChild(button);

        label.x = padding;
        label.y = padding;
        face.addChild(label);

        return face;
    }
}
}
