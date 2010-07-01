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

import com.threerings.util.DataPack;
import com.threerings.util.MultiLoader;

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
        // TODO - DO SOMETHING WITH ZATIONS
        getDisplayObjects(path, callback);
    }

    public function getTileImage (path :String, bounds :Rectangle, zations :Array,
        callback :Function) :void
    {
        getTileSetImage(path, zations, function(result :DisplayObject) :void {
            // TODO - DO SOMETHING TO SUB_REGION THIS
            if (result is Bitmap) {
                var data :BitmapData =
                    new BitmapData(bounds.width, bounds.height, true, 0x00000000);
                data.copyPixels(Bitmap(result).bitmapData, bounds, new Point(0, 0));
                callback(new Bitmap(data));
            } else {
                callback(result);
            }
        });
    }
}
}