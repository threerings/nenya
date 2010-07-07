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
import com.threerings.util.StringUtil;

public class ColorPository
{
    private const log :Log = Log.getLog(ColorPository);

    public function getClasses () :Array
    {
        return _classes.values();
    }

    /**
     * Returns an array containing the records for the colors in the
     * specified class.
     */
    public function getColors (className :String) :Array
    {
        // make sure the class exists
        var record :ClassRecord = getClassRecordByName(className);
        if (record == null) {
            return null;
        }

        // create the array
        return record.colors.values();
    }

    /**
     * Returns an array containing the ids of the colors in the specified
     * class.
     */
    public function getColorIds (className :String) :Array
    {
        // make sure the class exists
        var record :ClassRecord = getClassRecordByName(className);
        if (record == null) {
            return null;
        }

        return record.colors.values().map(
            function(element :ColorRecord, index :int, arr :Array) :int {
                return element.colorId;
            });
    }

    /**
     * Returns true if the specified color is legal for use at character creation time. false is
     * always returned for non-existent colors or classes.
     */
    public function isLegalStartColor (classId :int, colorId :int) :Boolean
    {
        var color :ColorRecord = getColorRecord(classId, colorId);
        return (color == null) ? false : color.starter;
    }

    /**
     * Returns a random starting color from the specified color class.
     */
    public function getRandomStartingColor (className :String) :ColorRecord
    {
        // make sure the class exists
        var record :ClassRecord = getClassRecordByName(className);
        return (record == null) ? null : record.randomStartingColor();
    }

    /**
     * Looks up a colorization by id.
     */
    public function getColorization (classId :int, colorId :int) :Colorization
    {
        var color :ColorRecord = getColorRecord(classId, colorId);
        return (color == null) ? null : color.getColorization();
    }

    /**
     * Looks up a colorization by color print.
     */
    public function getColorizationByPrint (colorPrint :int) :Colorization
    {
        return getColorization(colorPrint >> 8, colorPrint & 0xFF);
    }

    /**
     * Looks up a colorization by class and color names.
     */
    public function getColorizationByName (className :String, colorName :String) :Colorization
    {
        var crec :ClassRecord = getClassRecordByName(className);
        if (crec != null) {
            var colorId :int = crec.getColorId(colorName);

            var color :ColorRecord = crec.colors.get(colorId);
            if (color != null) {
                return color.getColorization();
            }
        }
        return null;
    }

    /**
     * Looks up a colorization by class and color Id.
     */
    public function getColorizationByNameAndId (className :String, colorId :int) :Colorization
    {
        var crec :ClassRecord = getClassRecordByName(className);
        if (crec != null) {
            var color :ColorRecord = crec.colors.get(colorId);
            if (color != null) {
                return color.getColorization();
            }
        }
        return null;
    }

    /**
     * Loads up a colorization class by name and logs a warning if it doesn't exist.
     */
    public function getClassRecordByName (className :String) :ClassRecord
    {
        for each (var crec :ClassRecord in _classes.values()) {
            if (crec.name == className) {
                return crec;
            }
        }
        log.warning("No such color class", "class", className, new Error());
        return null;
    }

    /**
     * Looks up the requested color class record.
     */
    public function getClassRecord (classId :int) :ClassRecord
    {
        return _classes.get(classId);
    }

    /**
     * Looks up the requested color record.
     */
    public function getColorRecord (classId :int, colorId :int) :ColorRecord
    {
        var record :ClassRecord = getClassRecord(classId);
        if (record == null) {
            // if they request color class zero, we assume they're just
            // decoding a blank colorprint, otherwise we complain
            if (classId != 0) {
                log.warning("Requested unknown color class",
                    "classId", classId, "colorId", colorId, new Error());
            }
            return null;
        }
        return record.colors.get(colorId);
    }

    /**
     * Looks up the requested color record by class & color names.
     */
    public function getColorRecordByName (className :String, colorName :String) :ColorRecord
    {
        var record :ClassRecord = getClassRecordByName(className);
        if (record == null) {
            log.warning("Requested unknown color class",
                "className", className, "colorName", colorName, new Error());
            return null;
        }

        var colorId :int = record.getColorId(colorName);

        return record.colors.get(colorId);
    }

    /**
     * Loads up a serialized color pository from the supplied resource
     * manager.
     */
    public static function fromXml (xml :XML) :ColorPository
    {
        var pos :ColorPository = new ColorPository();
        for each (var classXml :XML in xml.elements("class")) {
            pos._classes.put(classXml.@classId, ClassRecord.fromXml(classXml));
        }

        return pos;
    }

    /** Our mapping from class names to class records. */
    protected var _classes :Map = Maps.newMapOf(int);
}
}