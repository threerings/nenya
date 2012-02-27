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

package com.threerings.cast {

import flash.geom.Point;

import com.threerings.media.tile.SwissArmyTileSet;
import com.threerings.util.DirectionUtil;
import com.threerings.util.StringUtil;

/**
 * The action sequence class describes a particular character animation
 * sequence. An animation sequence consists of one or more frames of
 * animation, renders at a particular frame rate, and has an origin point
 * that specifies the location of the base of the character in relation to
 * the bounds of the animation images.
 */
public class ActionSequence
{
    /**
     * Defines the name of the default action sequence. When component
     * tilesets are loaded to build a set of composited images for a
     * particular action sequence, a check is first made for a component
     * tileset specific to the action sequence and then for the
     * component's default tileset if the action specific tileset did not
     * exist.
     */
    public static const DEFAULT_SEQUENCE :String = "default";

    /** The action sequence name. */
    public var name :String;

    /** The number of frames per second to show when animating. */
    public var framesPerSecond :Number;

    /** The position of the character's base for this sequence. */
    public var origin :Point = new Point();

    public var tileset :SwissArmyTileSet;

    /** Orientation codes for the orientations available for this
     * action. */
    public var orients :Array;

    public static function fromXml (xml :XML) :ActionSequence
    {
        var seq :ActionSequence = new ActionSequence();
        seq.name = xml.@name;
        seq.framesPerSecond = xml.framesPerSecond;
        seq.orients = toOrientArray(xml.orients);
        seq.origin = toPoint(xml.origin);
        seq.tileset = SwissArmyTileSet.fromXml(xml.tileset[0]);

        return seq;
    }

    protected static function toOrientArray (str :String) :Array
    {
        if (str == null || str.length == 0) {
            return null;
        }

        return str.split(",").map(function(element :String, index :int, arr :Array) :int {
                return DirectionUtil.fromShortString(StringUtil.trim(element));
            });
    }

    protected static function toPoint (str :String) :Point
    {
        if (str == null || str.length == 0) {
            return null;
        }

        var coords :Array =
            str.split(",").map(function(element :String, index :int, arr :Array) :int {
                return int(StringUtil.trim(element));
            });

        return new Point(coords[0], coords[1]);
    }

    public function toString () :String
    {
        return "[name=" + name + ", framesPerSecond=" + framesPerSecond +
            ", origin=" + origin +
            ", orients=" + (orients == null ? 0 : orients.length) + "]";
    }
}
}