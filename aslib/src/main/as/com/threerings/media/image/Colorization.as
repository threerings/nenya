//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// http://code.google.com/p/nenya/
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

package com.threerings.media.image {

import com.threerings.display.ColorUtil;
import com.threerings.util.Hashable;
import com.threerings.util.Short;
import com.threerings.util.StringUtil;

public class Colorization
    implements Hashable
{
    /** Every colorization must have a unique id that can be used to
     * compare a particular colorization record with another. */
    public var colorizationId :int;

    /** The root color for the colorization. */
    public var rootColor :uint;

    /** The range around the root color that will be colorized (in
     * delta-hue, delta-saturation, delta-value. Note that this range is inclusive. */
    public var range :Array;

    /** The adjustments to make to hue, saturation and value. */
    public var offsets :Array;

    /**
     * Constructs a colorization record with the specified identifier.
     */
    public function Colorization (colorizationId :int, rootColor :uint,
                                  range :Array, offsets :Array)
    {
        this.colorizationId = colorizationId;
        this.rootColor = rootColor;
        this.range = range;
        this.offsets = offsets;

        // compute our HSV and fixed HSV
        _hsv = ColorUtil.RGBtoHSB(ColorUtil.getRed(rootColor), ColorUtil.getGreen(rootColor),
             ColorUtil.getBlue(rootColor));
        _fhsv = toFixedHSV(_hsv);
    }

    /**
     * Returns the root color adjusted by the colorization.
     */
    public function getColorizedRoot () :uint
    {
        return recolorColor(_hsv);
    }

    /**
     * Adjusts the supplied color by the offests in this colorization,
     * taking the appropriate measures for hue (wrapping it around) and
     * saturation and value (clipping).
     *
     * @return the RGB value of the recolored color.
     */
    public function recolorColor (hsv :Array) :uint
    {
        // for hue, we wrap around
        var hue :Number = hsv[0] + offsets[0];
        if (hue > 1.0) {
            hue -= 1.0;
        }

        // otherwise we clip
        var sat :Number = Math.min(Math.max(hsv[1] + offsets[1], 0), 1);
        var val :Number = Math.min(Math.max(hsv[2] + offsets[2], 0), 1);

        // convert back to RGB space
        return ColorUtil.HSBtoRGB(hue, sat, val);
    }

    /**
     * Returns true if this colorization matches the supplied color, false
     * otherwise.
     *
     * @param hsv the HSV values for the color in question.
     * @param fhsv the HSV values converted to fixed point via {@link
     * #toFixedHSV} for the color in question.
     */
    public function matches (hsv :Array, fhsv :Array) :Boolean
    {
        // check to see that this color is sufficiently "close" to the
        // root color based on the supplied distance parameters
        if (distance(fhsv[0], _fhsv[0], Short.MAX_VALUE) >
            range[0] * Short.MAX_VALUE) {
            return false;
        }

        // saturation and value don't wrap around like hue
        if (Math.abs(_hsv[1] - hsv[1]) > range[1] ||
            Math.abs(_hsv[2] - hsv[2]) > range[2]) {
            return false;
        }

        return true;
    }

    public function hashCode () :int
    {
        return colorizationId ^ rootColor;
    }

    public function equals (other :Object) :Boolean
    {
        if (other is Colorization) {
            return (Colorization(other)).colorizationId == colorizationId;
        } else {
            return false;
        }
    }

    public function toString () :String
    {
        return String(colorizationId);
    }

    /**
     * Returns a long string representation of this colorization.
     */
    public function toVerboseString () :String
    {
        return StringUtil.toString(this);
    }

    /**
     * Converts floating point HSV values to a fixed point integer
     * representation.
     *
     * @param hsv the HSV values to be converted.
     * @param fhsv the destination array into which the fixed values will
     * be stored. If this is null, a new array will be created of the
     * appropriate length.
     *
     * @return the <code>fhsv</code> parameter if it was non-null or the
     * newly created target array.
     */
    public static function toFixedHSV (hsv :Array) :Array
    {
        var fhsv :Array = new Array(hsv.length);

        for (var ii :int = 0; ii < hsv.length; ii++) {
            fhsv[ii] = int(hsv[ii] * Short.MAX_VALUE);
        }
        return fhsv;
    }

    /**
     * Returns the distance between the supplied to numbers modulo N.
     */
    public static function distance (a :int, b :int, N :int) :int
    {
        return (a > b) ? Math.min(a - b, b + N - a) : Math.min(b - a, a + N - b);
    }

    /** Fixed HSV values for our root color; used when calculating
     * recolorizations using this colorization. */
    protected var _fhsv :Array;

    /** HSV values for our root color; used when calculating
     * recolorizations using this colorization. */
    protected var _hsv :Array;
}
}