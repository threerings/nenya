//
// $Id$

package com.threerings.flash {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.SimpleButton;
import flash.display.Sprite;

import flash.geom.ColorTransform;

/**
 * Takes a BitmapData and makes a button that brightens on hover and depresses when pushed.
 */
public class SimpleIconButton extends SimpleButton
{
    /**
     * Constructor.
     * 
     * @param icon a BitmapData, or Bitmap (from which the BitmapData will be extracted), or
     *             a Class that instantiates into either a BitmapData or Bitmap.
     */
    public function SimpleIconButton (icon :*)
    {
        var bmp :BitmapData;
        if (icon is Class) {
            icon = new (Class(icon))();
        }
        if (icon is BitmapData ) {
            bmp = BitmapData(icon);
        } else if (icon is Bitmap) {
            bmp = Bitmap(icon).bitmapData;
        } else {
            throw new Error("Unknown icon spec: must be a Bitmap or BitmapData, or a Class " +
                "that becomes one.");
        }

        upState = new Bitmap(bmp);
        hitTestState = upState;
        overState = new Bitmap(bmp);

        var down :Sprite = new Sprite();
        var downBmp :Bitmap = new Bitmap(bmp);
        downBmp.y = 1;
        down.addChild(downBmp);
        downState = down;

        const bright :ColorTransform = new ColorTransform(1.25, 1.25, 1.25);
        overState.transform.colorTransform = bright;
        downState.transform.colorTransform = bright;
    }
}
}
