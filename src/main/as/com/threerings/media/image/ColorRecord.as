//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2011 Three Rings Design, Inc., All Rights Reserved
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

import com.threerings.util.Log;
import com.threerings.util.StringUtil;

public class ColorRecord
{
    private const log :Log = Log.getLog(ColorRecord);

    /** The colorization class to which we belong. */
    public var cclass :ClassRecord;

    /** A unique colorization identifier (used in fingerprints). */
    public var colorId :int;

    /** The name of the target color. */
    public var name :String;

    /** Data indicating the offset (in HSV color space) from the
     * source color to recolor to this color. */
    public var offsets :Array;

    /** Tags this color as a legal starting color or not. This is a
     * shameful copout, placing application-specific functionality
     * into a general purpose library class. */
    public var starter :Boolean;

    /**
     * Returns a value that is the composite of our class id and color
     * id which can be used to identify a colorization record. This
     * value will always be a positive integer that fits into 16 bits.
     */
    public function getColorPrint () :int
    {
        return ((cclass.classId << 8) | colorId);
    }

    /**
     * Returns the data in this record configured as a colorization
     * instance.
     */
    public function getColorization () :Colorization
    {
        return new Colorization(getColorPrint(), cclass.source,
                                cclass.range, offsets);
    }

    public function toString () :String
    {
        return "[id=" + colorId + ", name=" + name +
            ", offsets=" + StringUtil.toString(offsets) +
            ", starter=" + starter + "]";
    }


    public static function fromXml (xml :XML, cclass :ClassRecord) :ColorRecord
    {
        var rec :ColorRecord = new ColorRecord();
        rec.colorId = xml.@colorId;
        rec.name = xml.@name;
        rec.offsets = toNumArray(xml.@offsets);
        rec.starter = xml.@starter;
        rec.cclass = cclass;

        return rec;
    }

    protected static function toNumArray (str :String) :Array
    {
        if (str == null) {
            return null;
        }

        return str.split(",").map(function(element :String, index :int, arr :Array) :Number {
            return Number(StringUtil.trim(element));
        });
    }

    /** Our data represented as a colorization. */
    protected var _zation :Colorization;
}
}