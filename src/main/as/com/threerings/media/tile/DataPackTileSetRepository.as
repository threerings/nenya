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

package com.threerings.media.tile {

import nochump.util.zip.ZipEntry;

import flash.net.URLLoader;
import flash.net.URLLoaderDataFormat;
import flash.net.URLRequest;
import flash.events.Event;
import flash.utils.ByteArray;

import com.threerings.io.ObjectInputStream;
import com.threerings.media.tile.TileDataPack;
import com.threerings.media.tile.bundle.TileSetBundle;
import com.threerings.util.DataPack;
import com.threerings.util.Integer;
import com.threerings.util.Log;
import com.threerings.util.Maps;
import com.threerings.util.Map;
import com.threerings.util.Set;
import com.threerings.util.StringUtil;

public class DataPackTileSetRepository
    implements TileSetRepository, TileSetIdMap
{
    private static var log :Log = Log.getLog(DataPackTileSetRepository);

    public function DataPackTileSetRepository (tileSetUrl :String)
    {
        _rootUrl = tileSetUrl;
        loadTileSetMeta(tileSetUrl + "metadata.jar");
    }

    protected function loadTileSetMeta (tileSetMetaUrl :String) :void
    {
        _metaPack = new TileDataPack(tileSetMetaUrl, metaLoadingComplete);
    }

    protected function parseTileSetMap (map :String) :void
    {
        var lines :Array = map.split("\n");
        var tileCount :int = int(lines[0]);
        for (var ii :int = 1; ii < lines.length; ii++) {
            var line :String = lines[ii];
            var toks :Array = line.split(" := ");
            if (toks.length >= 2) {
                var id :int = int(toks[1]);
                var name :String = String(toks[0]);
                _nameMap.put(name, id);
            }
        }
    }

    /**
     * Handle the successful completion of datapack loading.
     *
     * @private
     */
    protected function metaLoadingComplete (event :Event) :void
    {
        parseTileSetMap(_metaPack.getFileAsString("tilesetmap.txt"));

        for each (var entry :ZipEntry in _metaPack.getFiles()) {
            var bundleName :String = entry.name;
            if (StringUtil.endsWith(bundleName, "/bundle.xml")) {
                loadTileSetsFromPack(bundleName.substr(0, bundleName.length - 11));
            }
        }

        // Notify them all and clear our list.
        for each (var func :Function in _notifyOnLoad) {
            func();
        }
        _notifyOnLoad = [];
    }


    public function notifyOnLoad (func :Function) :void
    {
        _notifyOnLoad.push(func);
    }

    public function enumerateTileSetIds () :Array
    {
        return _idMap.keys();
    }

    public function enumerateTileSets () :Array
    {
        return _idMap.values();
    }

    public function getTileSet (tileSetId :int) :TileSet
    {
        var tileSet :TileSet = _idMap.get(tileSetId);

        // We don't have this tileset loaded just yet.... start grabbing the data pack.
        var packName :String = _packMap.get(tileSetId);
        if (packName == null) {
            log.warning("Attempted to load unknown tile set", "tileSetId", tileSetId);
            throw new NoSuchTileSetError(tileSetId);
        }

        var pack :TileDataPack = _packs.get(packName);

        if (pack == null || !pack.isComplete()) {
            loadPack(packName, tileSetId, tileSet);

            // Loading the pack should've put us in the map, so let's go retrieve it...
            tileSet = _idMap.get(tileSetId);
        } else {
            tileSet.setImageProvider(_packs.get(packName));
        }

        return tileSet;
    }

    protected function loadPack (packName :String, tileSetId :int, tileSet :TileSet) :void
    {
        var pack :TileDataPack = _packs.get(packName);

        if (pack != null && pack.isComplete()) {
            // Already loaded
            return;
        }

        var completeListener :Function = function () :void {
            tileSet.setImageProvider(_packs.get(packName));
        };

        var listeners :Array = _packListeners.get(packName);
        if (listeners == null) {
            listeners = [];
            _packListeners.put(packName, listeners);

            var packCompleteListener :Function = function () :void {
                for each (var listener :Function in listeners) {
                    listener();
                }
                _packListeners.remove(packName);
            }

            pack = new TileDataPack(getPackUrl(packName), packCompleteListener);
            _packs.put(packName, pack);
        }

        listeners.push(completeListener);
    }

    protected function loadTileSetsFromPack (packName :String) :void
    {
        var xml :XML = _metaPack.getFileAsXML(packName + "/bundle.xml");
        var bundle :TileSetBundle = TileSetBundle.fromXml(xml, this);
        bundle.forEach(function(key :*, val :*) :void {
            _idMap.put(key, val);
            _packMap.put(key, packName);
        });
    }

    protected function getPackUrl (packName :String) :String
    {
        return _rootUrl + packName + "/bundle.jar";
    }

    public function getTileSetId (setName :String) :int
    {
        return _nameMap.get(setName);
    }

    public function getTileSetByName (setName :String) :TileSet
    {
        return getTileSet(getTileSetId(setName));
    }

    protected function getPackName (name :String) :String
    {
        var lastSlashIdx :int = name.lastIndexOf("/");
        if (lastSlashIdx == -1) {
            return name;
        } else {
            return name.substring(0, lastSlashIdx).toLowerCase();
        }
    }

    public function ensureLoaded (tileSets :Set, completeCallback :Function,
        progressCallback :Function) :void
    {
        var completeCt :int = 0;

        var size :int = tileSets.size();

        // Handles the completion of a single tileset, which if it's our last may call the callback.
        function completeOneSet () :void {
            completeCt++;

            progressCallback(completeCt / Number(size));

            if (completeCt >= tileSets.size()) {
                completeCallback();

                // Ensure we stop making callbacks.
                completeCallback = null;
                progressCallback = null;
            }
        };

        tileSets.forEach(function (setId :int) :void {
                var thisSet :TileSet = getTileSet(setId);
                if (thisSet.isLoaded()) {
                    completeOneSet();
                } else {
                    thisSet.notifyOnLoad(completeOneSet);
                }
            });
    }

    /** Contains the tileset meta-data for all tilesets. */
    protected var _metaPack : DataPack;

    /** Maps name to tileSetId. */
    protected var _nameMap :Map = Maps.newMapOf(String);

    /** Maps tileSetId to actual TileSet. */
    protected var _idMap : Map = Maps.newMapOf(int);

    /** Maps tileSetId to DataPack name. */
    protected var _packMap :Map = Maps.newMapOf(int);

    /** The URL for our tilesets. */
    protected var _rootUrl :String;

    /** Map of pack name to listeners waiting for that pack to resolve. */
    protected var _packListeners :Map = Maps.newMapOf(String);

    /** The map of packs we've already loaded up and resolved. */
    protected var _packs :Map = Maps.newMapOf(String);

    /** Everyone who cares when we're loaded. */
    protected var _notifyOnLoad :Array = [];
}
}