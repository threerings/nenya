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

import com.threerings.media.tile.TileSetIdMap;
import com.threerings.util.XmlUtil;

public class FringeTileSetRecord
{
    /** The tileset id of the fringe tileset. */
    public var fringe_tsid :int;

    /** Is this a mask? */
    public var mask :Boolean;

    public static function fromXml (xml :XML, idMap :TileSetIdMap) :FringeTileSetRecord
    {
        var rec :FringeTileSetRecord = new FringeTileSetRecord();
        rec.fringe_tsid = idMap.getTileSetId(xml.@name);
        rec.mask = XmlUtil.getBooleanAttr(xml, "mask", false);
        return rec;
    }

    /** Did everything parse well? */
    public function isValid () :Boolean
    {
        return (fringe_tsid != 0);
    }

    public function toString () :String
    {
        return "[fringe_tsid=" + fringe_tsid + ", mask=" + mask + "]";
    }

}
}