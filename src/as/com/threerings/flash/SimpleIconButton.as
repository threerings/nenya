//
// $Id$

package com.threerings.flash {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.SimpleButton;

import flash.geom.ColorTransform;

/**
 * Takes a BitmapData and makes a button that brightens on hover and depresses when pushed.
 */
public class SimpleIconButton extends SimpleButton
{
    /**
     * Constructor. @see #setIcon()
     */
    public function SimpleIconButton (icon :*)
    {
        setIcon(icon);
    }

    /**
     * Update the icon for this button.
     *
     * @param icon a BitmapData, or Bitmap (from which the BitmapData will be extracted), or
     *             a Class that instantiates into either a BitmapData or Bitmap.
     */
    public function setIcon (icon :*) :void
    {
        var bmp :BitmapData = ImageUtil.toBitmapData(icon);
        if (bmp == null) {
            throw new Error("Unknown icon spec: must be a Bitmap or BitmapData, or a Class " +
                "that becomes one.");
        }

        const bright :ColorTransform = new ColorTransform(1.25, 1.25, 1.25);
        upState = new Bitmap(bmp);
        overState = new Bitmap(bmp);
        overState.transform.colorTransform = bright;
        downState = new Bitmap(bmp);
        downState.y = 1;
        downState.transform.colorTransform = bright;
        hitTestState = upState;
    }
}
}
