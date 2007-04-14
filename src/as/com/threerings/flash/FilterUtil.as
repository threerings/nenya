package com.threerings.flash {

import flash.display.DisplayObject;

import flash.filters.BevelFilter;
import flash.filters.BitmapFilter;
import flash.filters.BlurFilter;
import flash.filters.ColorMatrixFilter;
import flash.filters.ConvolutionFilter;
import flash.filters.DisplacementMapFilter;
import flash.filters.DropShadowFilter;
import flash.filters.GlowFilter;
import flash.filters.GradientBevelFilter;
import flash.filters.GradientGlowFilter;

import com.threerings.util.ClassUtil;

/**
 * Useful utility methods that wouldn't be needed if the flash API were not so retarded.
 */
public class FilterUtil
{
    /**
     * Add the specified filter to the DisplayObject.
     */
    public static function addFilter (disp :DisplayObject, filter :BitmapFilter) :void
    {
        checkArgs(disp, filter);
        var filts :Array = disp.filters;
        if (filts == null) {
            filts = [];
        }
        filts.push(filter);
        disp.filters = filts;
    }

    /**
     * Remove the specified filter from the DisplayObject.
     * Note that the filter set in the DisplayObject is a clone, so the values
     * of the specified filter are used to find a match.
     */
    public static function removeFilter (disp :DisplayObject, filter :BitmapFilter) :void
    {
        checkArgs(disp, filter);
        var filts :Array = disp.filters;
        if (filts != null) {
            for (var ii :int = filts.length - 1; ii >= 0; ii--) {
                var oldFilter :BitmapFilter = (filts[ii] as BitmapFilter);
                if (equals(oldFilter, filter)) {
                    filts.splice(ii, 1);
                    if (filts.length == 0) {
                        filts = null;
                    }
                    disp.filters = filts;
                    return; // SUCCESS
                }
            }
        }
    }

    /**
     * Are the two filters equals?
     */
    public static function equals (f1 :BitmapFilter, f2 :BitmapFilter) :Boolean
    {
        if (f1 === f2) { // catch same instance, or both null
            return true;

        } else if (f1 == null || f2 == null) { // otherwise nulls are no good
            return false;
        }

        var c1 :Class = ClassUtil.getClass(f1);
        if (c1 !== ClassUtil.getClass(f2)) {
            return false;
        }

        // when we have two filters of the same class, figure it out by hand...
        switch (c1) {
        case BevelFilter:
            // TODO
            return false;

        case BlurFilter:
            // TODO
            return false;

        case ColorMatrixFilter:
            // TODO
            return false;

        case ConvolutionFilter:
            // TODO
            return false;

        case DisplacementMapFilter:
            // TODO
            return false;

        case DropShadowFilter:
            // TODO
            return false;

        case GlowFilter:
            var gf1 :GlowFilter = (f1 as GlowFilter);
            var gf2 :GlowFilter = (f2 as GlowFilter);
            return (gf1.alpha == gf2.alpha) && (gf1.blurX == gf2.blurX) &&
                (gf1.blurY == gf2.blurY) && (gf1.color == gf2.color) && (gf1.inner == gf2.inner) &&
                (gf1.knockout == gf2.knockout) && (gf1.quality == gf2.quality) &&
                (gf1.strength == gf2.strength);

        case GradientBevelFilter:
            // TODO
            return false;

        case GradientGlowFilter:
            // TODO
            return false;

        default:
            throw new ArgumentError("OMG! Unknown filter type: " + c1);
        }
    }

    protected static function checkArgs (disp :DisplayObject, filter :BitmapFilter) :void
    {
        if (disp == null || filter == null) {
            throw new ArgumentError("args may not be null");
        }
    }
}
}
