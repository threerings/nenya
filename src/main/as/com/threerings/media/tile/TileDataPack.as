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

package com.threerings.media.tile {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.DisplayObject;

import flash.geom.Point;
import flash.geom.Rectangle;

import flash.events.Event;

import flash.utils.ByteArray;

import nochump.util.zip.ZipEntry;
import nochump.util.zip.ZipError;
import nochump.util.zip.ZipFile;

import com.threerings.media.image.PngRecolorUtil;
import com.threerings.util.DataPack;
import com.threerings.util.Map;
import com.threerings.util.Maps;
import com.threerings.util.MultiLoader;
import com.threerings.util.StringUtil;

/**
 * Like a normal data pack, but we don't deal with any data, and all our filenames are a direct
 *  translation, so we need no metadata file.  We also serve as a tile ImageProvider.
 */
public class TileDataPack extends DataPack
    implements ImageProvider
{
    public function TileDataPack (
        source :Object, completeListener :Function = null, errorListener :Function = null)
    {
        super(source, completeListener, errorListener);
    }

    protected override function bytesAvailable (bytes :ByteArray) :void
    {
        bytes.position = 0;
        try {
            _zip = new ZipFile(bytes);
        } catch (zipError :ZipError) {
            dispatchError("Unable to read datapack: " + zipError.message);
            return;
        }

        // Put something there so we know we're loaded.
        _metadata = <xml></xml>;

        // yay, we're completely loaded!
        dispatchEvent(new Event(Event.COMPLETE));
    }

    /**
     * We have no metadata, so everything is undefined.
     */
    public override function getData (name :String, formatType :String = null) :*
    {
        return undefined;
    }

    /**
     * We have no XML metadata, so our filename is exactly the original name.
     */
    protected override function getFileName (name :String) :String
    {
        return name;
    }

    public function getTileSetImage (path :String, zations :Array, callback :Function) :void
    {
        var key :String = zations == null ? path : path + ":" + StringUtil.toString(zations);

        if (_cache.containsKey(key)) {
            callback(_cache.get(key));
        } else if (_pending.containsKey(key)) {
            _pending.get(key).push(callback);
        } else {
            _pending.put(key, [callback]);
            MultiLoader.getContents(PngRecolorUtil.recolorPNG(getFile(path), zations),
                function(result :Bitmap) :void {
                    _cache.put(key, result);
                    for each (var func :Function in _pending.remove(key)) {
                        func(result);
                    }
                });
        }
    }

    public function getTileImage (path :String, bounds :Rectangle, zations :Array,
        callback :Function) :void
    {
        getTileSetImage(path, zations, function(result :Bitmap) :void {
            var data :BitmapData =
                new BitmapData(bounds.width, bounds.height, true, 0x00000000);
            data.copyPixels(Bitmap(result).bitmapData, bounds, new Point(0, 0));
            callback(new Bitmap(data));
        });
    }

    /** Cache of images loaded from this data pack. */
    protected var _cache :Map = Maps.newMapOf(String);

    /** Any images we're in the process of resolving, map their key to a list of listeners*/
    protected var _pending :Map = Maps.newMapOf(String);
}
}