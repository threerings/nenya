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

package com.threerings.media.tile.bundle {

import flash.display.DisplayObject;

import com.threerings.io.ObjectOutputStream;
import com.threerings.io.ObjectInputStream;
import com.threerings.io.Streamable;
import com.threerings.util.DataPack;
import com.threerings.util.maps.DictionaryMap;
import com.threerings.media.tile.BaseTileSet;
import com.threerings.media.tile.ObjectTileSet;
import com.threerings.media.tile.SwissArmyTileSet;
import com.threerings.media.tile.TileSet;
import com.threerings.media.tile.TileSetIdMap;

public class TileSetBundle extends DictionaryMap
{
    public function init (pack :DataPack) :void
    {
        _pack = pack;
    }

    public static function fromXml (xml :XML, idMap :TileSetIdMap) :TileSetBundle
    {
        var bundle :TileSetBundle = new TileSetBundle();
        for each (var objXml :XML in xml.object.tileset) {
            var objId :int = idMap.getTileSetId(objXml.@name);
            bundle.put(objId, ObjectTileSet.fromXml(objXml));
        }
        for each (var baseXml :XML in xml.base.tileset) {
            var baseId :int = idMap.getTileSetId(baseXml.@name);
            bundle.put(baseId, BaseTileSet.fromXml(baseXml));
        }
        for each (var fringeXml :XML in xml.fringe.tileset) {
            var fringeId :int = idMap.getTileSetId(fringeXml.@name);
            bundle.put(fringeId, SwissArmyTileSet.fromXml(fringeXml));
        }
        return bundle;
    }

    /**
     * Loads the image from our data pack.
     */
    public function loadImage (path :String) :DisplayObject
    {
        return DisplayObject(_pack.getFile(path));
    }

    /** The data pack from which we can grab images. */
    protected var _pack :DataPack;
}
}