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

package com.threerings.miso.tile {

import com.threerings.media.tile.TileSetIdMap;
import com.threerings.util.StringUtil;

public class FringeRecord
{
    /** The tileset id of the base tileset to which this applies. */
    public var base_tsid :int;

    /** The fringe priority of this base tileset. */
    public var priority :int;

    /** A list of the possible tilesets that can be used for fringing. */
    public var tilesets :Array = [];

    public static function fromXml (xml :XML, idMap :TileSetIdMap) :FringeRecord
    {
        var rec :FringeRecord = new FringeRecord();
        rec.base_tsid = idMap.getTileSetId(xml.@name);
        rec.priority = xml.@priority;
        for each (var tsXml :XML in xml.tileset) {
            rec.addTileset(FringeTileSetRecord.fromXml(tsXml, idMap));
        }
        return rec;
    }

    /** Used when parsing the tilesets definitions. */
    public function addTileset (record :FringeTileSetRecord) :void
    {
        tilesets.push(record);
    }

    /** Did everything parse well? */
    public function isValid () :Boolean
    {
        return ((base_tsid != 0) && (priority > 0));
    }

    public function toString () :String
    {
        return "[base_tsid=" + base_tsid + ", priority=" + priority +
            ", tilesets=" + StringUtil.toString(tilesets) + "]";
    }
}
}