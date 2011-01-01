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

package com.threerings.miso.tile {

import com.threerings.util.Map;
import com.threerings.util.Maps;
import com.threerings.media.tile.TileSetIdMap;

/**
 * Used to manage data about which base tilesets fringe on which others
 * and how they fringe.
 */
public class FringeConfiguration
{
    public static function fromXml (xml :XML, idMap :TileSetIdMap) :FringeConfiguration
    {
        var config :FringeConfiguration = new FringeConfiguration();
        for each (var baseXml :XML in xml.base) {
            config.addFringeRecord(FringeRecord.fromXml(baseXml, idMap));
        }
        return config;
    }

    /**
     * Adds a parsed FringeRecord to this instance. This is used when parsing
     * the fringerecords from xml.
     */
    public function addFringeRecord (frec :FringeRecord) :void
    {
        _frecs.put(frec.base_tsid, frec);
    }

    /**
     * If the first base tileset fringes upon the second, return the
     * fringe priority of the first base tileset, otherwise return -1.
     */
    public function fringesOn (first :int, second :int) :int
    {
        // Short-circuit if we're fringing on ourselves.
        if (first == second) {
            return -1;
        }

        var f1 :FringeRecord = _frecs.get(first);

        // we better have a fringe record for the first
        if (null != f1) {

            // it had better have some tilesets defined
            if (f1.tilesets.length > 0) {

                var f2 :FringeRecord = _frecs.get(second);

                // and we only fringe if second doesn't exist or has a lower
                // priority
                if ((null == f2) || (f1.priority > f2.priority)) {
                    return f1.priority;
                }
            }
        }

        return -1;
    }

    /**
     * Get a random FringeTileSetRecord from amongst the ones
     * listed for the specified base tileset.
     */
    public function getFringe (baseset :int, hashValue :int) :FringeTileSetRecord
    {
        var f :FringeRecord = _frecs.get(baseset);
        return f.tilesets[hashValue % f.tilesets.length];
    }

    /**
     * Returns all the tilesets used to fringe with this baseset.
     */
    public function getFringeSets (baseset :int) :Array
    {
        var f :FringeRecord = _frecs.get(baseset);
        if (f == null) {
            return [];
        } else {
            return f.tilesets.map(
                function (element :FringeTileSetRecord, index:int, arr:Array) :int {
                    return element.fringe_tsid;
                });
        }
    }

    /** The mapping from base tileset id to fringerecord. */
    protected var _frecs :Map = Maps.newMapOf(int);
}
}