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

import com.threerings.util.ArrayUtil;
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
            var bf1 :BevelFilter = (f1 as BevelFilter);
            var bf2 :BevelFilter = (f2 as BevelFilter);
            return (bf1.angle == bf2.angle) && (bf1.blurX == bf2.blurX) &&
                (bf1.blurY == bf2.blurY) && (bf1.distance == bf2.distance) &&
                (bf1.highlightAlpha == bf2.highlightAlpha) &&
                (bf1.highlightColor == bf2.highlightColor) && (bf1.knockout == bf2.knockout) &&
                (bf1.quality == bf2.quality) && (bf1.shadowAlpha == bf2.shadowAlpha) &&
                (bf1.shadowColor == bf2.shadowColor) && (bf1.strength == bf2.strength) &&
                (bf1.type == bf2.type);

        case BlurFilter:
            var blur1 :BlurFilter = (f1 as BlurFilter);
            var blur2 :BlurFilter = (f2 as BlurFilter);
            return (blur1.blurX == blur2.blurX) && (blur1.blurY == blur2.blurY) &&
                (blur1.quality == blur2.quality);

        case ColorMatrixFilter:
            var cmf1 :ColorMatrixFilter = (f1 as ColorMatrixFilter);
            var cmf2 :ColorMatrixFilter = (f2 as ColorMatrixFilter);
            return ArrayUtil.equals(cmf1.matrix, cmf2.matrix);

        case ConvolutionFilter:
            var cf1 :ConvolutionFilter = (f1 as ConvolutionFilter);
            var cf2 :ConvolutionFilter = (f2 as ConvolutionFilter);
            return (cf1.alpha == cf2.alpha) && (cf1.bias == cf2.bias) && (cf1.clamp == cf2.clamp) &&
                (cf1.color == cf2.color) && (cf1.divisor == cf2.divisor) &&
                ArrayUtil.equals(cf1.matrix, cf2.matrix) && (cf1.matrixX == cf2.matrixX) &&
                (cf1.matrixY == cf2.matrixY) && (cf1.preserveAlpha == cf2.preserveAlpha);

        case DisplacementMapFilter:
            var dmf1 :DisplacementMapFilter = (f1 as DisplacementMapFilter);
            var dmf2 :DisplacementMapFilter = (f2 as DisplacementMapFilter);
            return (dmf1.alpha == dmf2.alpha) && (dmf1.color == dmf2.color) &&
                (dmf1.componentX == dmf2.componentX) && (dmf1.componentY == dmf2.componentY) &&
                (dmf1.mapBitmap == dmf2.mapBitmap) && dmf1.mapPoint.equals(dmf2.mapPoint) &&
                (dmf1.mode == dmf2.mode) && (dmf1.scaleX == dmf2.scaleX) &&
                (dmf1.scaleY == dmf2.scaleY);

        case DropShadowFilter:
            var dsf1 :DropShadowFilter = (f1 as DropShadowFilter);
            var dsf2 :DropShadowFilter = (f2 as DropShadowFilter);
            return (dsf1.alpha == dsf2.alpha) && (dsf1.angle = dsf2.angle) &&
                (dsf1.blurX = dsf2.blurX) && (dsf1.blurY = dsf2.blurY) &&
                (dsf1.color = dsf2.color) && (dsf1.distance = dsf2.distance) &&
                (dsf1.hideObject = dsf2.hideObject) && (dsf1.inner = dsf2.inner) &&
                (dsf1.knockout = dsf2.knockout) && (dsf1.quality = dsf2.quality) &&
                (dsf1.strength = dsf2.strength);

        case GlowFilter:
            var gf1 :GlowFilter = (f1 as GlowFilter);
            var gf2 :GlowFilter = (f2 as GlowFilter);
            return (gf1.alpha == gf2.alpha) && (gf1.blurX == gf2.blurX) &&
                (gf1.blurY == gf2.blurY) && (gf1.color == gf2.color) && (gf1.inner == gf2.inner) &&
                (gf1.knockout == gf2.knockout) && (gf1.quality == gf2.quality) &&
                (gf1.strength == gf2.strength);

        case GradientBevelFilter:
            var gbf1 :GradientBevelFilter = (f1 as GradientBevelFilter);
            var gbf2 :GradientBevelFilter = (f2 as GradientBevelFilter);
            return ArrayUtil.equals(gbf1.alphas, gbf2.alphas) && (gbf1.angle == gbf2.angle) &&
                (gbf1.blurX == gbf2.blurX) && (gbf1.blurY == gbf2.blurY) &&
                ArrayUtil.equals(gbf1.colors, gbf2.colors) && (gbf1.distance == gbf2.distance) &&
                (gbf1.knockout == gbf2.knockout) && (gbf1.quality == gbf2.quality) &&
                ArrayUtil.equals(gbf1.ratios, gbf2.ratios) && (gbf1.strength == gbf2.strength) &&
                (gbf1.type == gbf2.type);

        case GradientGlowFilter:
            var ggf1 :GradientGlowFilter = (f1 as GradientGlowFilter);
            var ggf2 :GradientGlowFilter = (f2 as GradientGlowFilter);
            return ArrayUtil.equals(ggf1.alphas, ggf2.alphas) && (ggf1.angle == ggf2.angle) &&
                (ggf1.blurX == ggf2.blurX) && (ggf1.blurY == ggf2.blurY) &&
                ArrayUtil.equals(ggf1.colors, ggf2.colors) && (ggf1.distance == ggf2.distance) &&
                (ggf1.knockout == ggf2.knockout) && (ggf1.quality == ggf2.quality) &&
                ArrayUtil.equals(ggf1.ratios, ggf2.ratios) && (ggf1.strength == ggf2.strength) &&
                (ggf1.type == ggf2.type);

        default:
            throw new ArgumentError("OMG! Unknown filter type: " + c1);
        }
    }

    /**
    * Shift the color matrix filter by the given amount.  This is adapted from the code found at
    * http://www.kirupa.com/forum/showthread.php?t=230706
    */
    public static function shiftHueBy (original :ColorMatrixFilter, 
        hueShift :int) :ColorMatrixFilter
    {
        var M1 :Array = [ 0.213, 0.715, 0.072,
                          0.213, 0.715, 0.072,
                          0.213, 0.715, 0.072 ];
        var M2 :Array = [ 0.787, -0.715, -0.072,
                         -0.212, 0.285, -0.072,
                         -0.213, -0.715, 0.928 ];
        var M3 :Array = [-0.213, -0.715, 0.928,
                          0.143, 0.140, -0.283,
                         -0.787, 0.715, 0.072 ];
        var M4 :Array = add(M1, add(multiply(Math.cos(hueShift * Math.PI / 180), M2), 
            multiply(Math.sin(hueShift * Math.PI / 180), M3)));

        var originalMatrix :Array = original.matrix;
        if (originalMatrix == null) {
            originalMatrix = original.matrix;
        }

        return new ColorMatrixFilter(concat(originalMatrix, [ M4[0], M4[1], M4[2], 0, 0,
                                                              M4[3], M4[4], M4[5], 0, 0,
                                                              M4[6], M4[7], M4[8], 0, 0,
                                                                  0,     0,     0, 1, 0 ]));
    }

    protected static function checkArgs (disp :DisplayObject, filter :BitmapFilter) :void
    {
        if (disp == null || filter == null) {
            throw new ArgumentError("args may not be null");
        }
    }

    protected static function identity () :Array
    {
        return [ 1, 0, 0, 0, 0,
                 0, 1, 0, 0, 0,
                 0, 0, 1, 0, 0,
                 0, 0, 0, 1, 0 ];
    }

    protected static function add (A :Array, B :Array) :Array
    {
        var C :Array = [];
        for(var ii :int = 0; ii < A.length; ii++)
        {
            C.push( A[ii] + B[ii] );
        }
        return C;
    }
    
    protected static function multiply(x :Number, B :Array) :Array
    {
        var A :Array = [];
        for each (var n :Number in B)
        {
            A.push(x * n);
        }
        return A;
    }
    
    protected static function concat(A :Array, B :Array) :Array
    {
        var nM :Array = [];
        
        nM[0] = (A[0] * B[0]) + (A[1] * B[5]) + (A[2] * B[10]) + (A[3] * B[15]);
        nM[1] = (A[0] * B[1]) + (A[1] * B[6]) + (A[2] * B[11]) + (A[3] * B[16]);
        nM[2] = (A[0] * B[2]) + (A[1] * B[7]) + (A[2] * B[12]) + (A[3] * B[17]);
        nM[3] = (A[0] * B[3]) + (A[1] * B[8]) + (A[2] * B[13]) + (A[3] * B[18]);
        nM[4] = (A[0] * B[4]) + (A[1] * B[9]) + (A[2] * B[14]) + (A[3] * B[19]) + A[4];
        
        nM[5] = (A[5] * B[0]) + (A[6] * B[5]) + (A[7] * B[10]) + (A[8] * B[15]);
        nM[6] = (A[5] * B[1]) + (A[6] * B[6]) + (A[7] * B[11]) + (A[8] * B[16]);
        nM[7] = (A[5] * B[2]) + (A[6] * B[7]) + (A[7] * B[12]) + (A[8] * B[17]);
        nM[8] = (A[5] * B[3]) + (A[6] * B[8]) + (A[7] * B[13]) + (A[8] * B[18]);
        nM[9] = (A[5] * B[4]) + (A[6] * B[9]) + (A[7] * B[14]) + (A[8] * B[19]) + A[9];
        
        nM[10] = (A[10] * B[0]) + (A[11] * B[5]) + (A[12] * B[10]) + (A[13] * B[15]);
        nM[11] = (A[10] * B[1]) + (A[11] * B[6]) + (A[12] * B[11]) + (A[13] * B[16]);
        nM[12] = (A[10] * B[2]) + (A[11] * B[7]) + (A[12] * B[12]) + (A[13] * B[17]);
        nM[13] = (A[10] * B[3]) + (A[11] * B[8]) + (A[12] * B[13]) + (A[13] * B[18]);
        nM[14] = (A[10] * B[4]) + (A[11] * B[9]) + (A[12] * B[14]) + (A[13] * B[19]) + A[14];
        
        nM[15] = (A[15] * B[0]) + (A[16] * B[5]) + (A[17] * B[10]) + (A[18] * B[15]);
        nM[16] = (A[15] * B[1]) + (A[16] * B[6]) + (A[17] * B[11]) + (A[18] * B[16]);
        nM[17] = (A[15] * B[2]) + (A[16] * B[7]) + (A[17] * B[12]) + (A[18] * B[17]);
        nM[18] = (A[15] * B[3]) + (A[16] * B[8]) + (A[17] * B[13]) + (A[18] * B[18]);
        nM[19] = (A[15] * B[4]) + (A[16] * B[9]) + (A[17] * B[14]) + (A[18] * B[19]) + A[19];

        return nM;
    }
}
}
