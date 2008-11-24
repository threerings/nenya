//
// $Id$

package com.threerings.flash {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.Shape;
import flash.display.SimpleButton;
import flash.display.Sprite;

import flash.text.TextField;
import flash.text.TextFormat;

/**
 * A simple skin that shifts the text in the down state.
 */
public class SimpleSkinButton extends SimpleButton
{
    /**
     * Create a SimpleSkinButton.
     * @param skin a BitmapData, Bitmap, or class that will turn into either.
     * @param text the text to place on the button
     * @param textFieldProps initProps for the TextField
     * @param textFormatProps initProps for the TextFormat
     * @param outline if a uint, the color of the outline to put over the skin.
     */
    public function SimpleSkinButton (
        skin :*, text :String, textFieldProps :Object = null, textFormatProps :Object = null,
        padding :int = 10, height :Number = NaN, xshift :int = 0, yshift :int = 1,
        outline :Object = null)
    {
        var bmp :BitmapData = ImageUtil.toBitmapData(skin);
        if (bmp == null) {
            throw new Error("Skin must be a Bitmap, BitmapData, or Class");
        }

        upState = createState(bmp, text, textFieldProps, textFormatProps,
            padding, height, 0, 0, outline);
        overState = upState;
        hitTestState = overState;
        downState = createState(bmp, text, textFieldProps, textFormatProps,
            padding, height, xshift, yshift, outline);
    }

    protected function createState (
        bmp :BitmapData, text :String, textFieldProps :Object, textFormatProps :Object,
        padding :int, height :Number, xshift :int, yshift :int, outline :Object) :Sprite
    {
        var state :Sprite = new Sprite();
        var field :TextField = TextFieldUtil.createField(text, textFieldProps, textFormatProps);

        if (isNaN(height)) {
            height = field.height;
        }

        var skin :Bitmap = new Bitmap(bmp);
        skin.width = field.width + (padding * 2);
        skin.height = height;
        state.addChild(skin);
    
        field.x = padding + xshift;
        field.y = (height - field.height) / 2 + yshift;
        state.addChild(field);

        if (outline != null) {
            var border :Shape = new Shape();
            border.graphics.lineStyle(1, uint(outline));
            border.graphics.drawRect(0, 0, skin.width - 1, skin.height - 1);
            state.addChild(border);
        }

        return state;
    }
}
}
