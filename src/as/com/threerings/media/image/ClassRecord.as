//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
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

package com.threerings.media.image {

import com.threerings.util.Log;
import com.threerings.util.Map;
import com.threerings.util.Maps;
import com.threerings.util.RandomUtil;
import com.threerings.util.StringUtil;

public class ClassRecord
{
    private const log :Log = Log.getLog(ClassRecord);

    /** An integer identifier for this class. */
    public var classId :int;

    /** The name of the color class. */
    public var name :String;

    /** The source color to use when recoloring colors in this class. */
    public var source :uint;

    /** Data identifying the range of colors around the source color
     * that will be recolored when recoloring using this class. */
    public var range :Array;

    /** The default starting legality value for this color class. See
     * {@link ColorRecord#starter}. */
    public var starter :Boolean;

    /** The default colorId to use for recoloration in this class, or
     * 0 if there is no default defined. */
    public var defaultId :int;

    /** A table of target colors included in this class. */
    public var colors :Map = Maps.newMapOf(int);

    /** Used when parsing the color definitions. */
    public function addColor (record :ColorRecord) :void
    {
        // validate the color id
        if (record.colorId > 127) {
            log.warning("Refusing to add color record; colorId > 127",
                        "class", this, "record", record);
        } else if (colors.containsKey(record.colorId)) {
            log.warning("Refusing to add duplicate colorId",
                        "class", this, "record", record, "existing", colors.get(record.colorId));
        } else {
            record.cclass = this;
            colors.put(record.colorId, record);
        }
    }

    /**
     * Translates a color identified in string form into the id that should be used to look up
     * its information. Throws an exception if no color could be found that associates with
     * that name.
     */
    public function getColorId (name :String) :int
    {
        // Check if the string is itself a number
        var id :int = name as int;
        if (colors.containsKey(id)) {
            return id;
        }

        // Look for name matches among all colors
        for each (var color :ColorRecord in colors) {
                if (StringUtil.compareIgnoreCase(color.name, name) == 0) {
                return color.colorId;
            }
        }

        // That input wasn't a color
        throw new Error("No color named '" + name + "'", 0);
    }

    /** Returns a random starting id from the entries in this class. */
    public function randomStartingColor () :ColorRecord
    {
        // figure out our starter ids if we haven't already
        if (_starters == null) {
            _starters = [];
            for each (var color :ColorRecord in colors) {
                if (color.starter) {
                    _starters.push(color);
                }
            }
        }

        // sanity check
        if (_starters.length < 1) {
            log.warning("Requested random starting color from colorless component class",
                        "class", this);
            return null;
        }

        // return a random entry from the array
        return RandomUtil.pickRandom(_starters);
    }

    /**
     * Get the default ColorRecord defined for this color class, or
     * null if none.
     */
    public function getDefault () :ColorRecord
    {
        return colors.get(defaultId);
    }

    public function toString () :String
    {
        return "[id=" + classId + ", name=" + name + ", source=#" +
            StringUtil.toHex(source & 0xFFFFFF, 6) +
            ", range=" + StringUtil.toString(range) +
            ", starter=" + starter + ", colors=" +
            StringUtil.toString(colors.values()) + "]";
    }

    public static function fromXml (xml :XML) :ClassRecord
    {
        var rec :ClassRecord = new ClassRecord();
        for each (var colorXml :XML in xml.color) {
            rec.colors.put(colorXml.@colorId, ColorRecord.fromXml(colorXml));
        }

        rec.name = xml.@name;
        var srcStr :String = xml.@source;
        rec.source = parseInt(srcStr.substring(1, srcStr.length - 1), 16);
        rec.range = toNumArray(xml.@range);
        rec.defaultId = xml.@defaultId;

        return rec;
    }

    protected static function toNumArray (str :String) :Array
    {
        if (str == null) {
            return null;
        }

        return str.split(",").map(function(element :*, index :int, arr :Array) :int {
            return Number(element);
        });
    }

    protected var _starters :Array;
}
}