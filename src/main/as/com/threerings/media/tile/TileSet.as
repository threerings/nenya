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
import flash.display.DisplayObject;
import flash.geom.Rectangle;

import com.threerings.display.ImageUtil;
import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;
import com.threerings.media.image.Colorization;
import com.threerings.util.Cloneable;
import com.threerings.util.Hashable;
import com.threerings.util.Log;
import com.threerings.util.StringUtil;
import com.threerings.util.Throttle;
import com.threerings.util.maps.HashMap;
import com.threerings.util.maps.WeakValueMap;

public /* abstract */ class TileSet
    implements Streamable, Hashable, Cloneable
{
    private static var log :Log = Log.getLog(TileSet);

    public function readObject (oin :ObjectInputStream) :void
    {
        _imagePath = oin.readUTF();
        _name = oin.readUTF();
    }

    public function writeObject (oout :ObjectOutputStream) :void
    {
        oout.writeUTF(_imagePath);
        oout.writeUTF(_name);
    }

    /**
     * Configures this tileset with an image provider that it can use to load its tileset image.
     * This will be called automatically when the tileset is fetched via the {@link TileManager}.
     */
    public function setImageProvider (improv :ImageProvider) :void
    {
        _improv = improv;

        if (_improv != null) {
            for each (var arr :Array in _pending) {
                initTile(arr[0], arr[1], arr[2]);
            }
        }
        _pending = [];

        loaded();
    }

    /**
     * Returns the tileset name.
     */
    public function getName () :String
    {
        return (_name == null) ? _imagePath : _name;
    }

    /**
     * Specifies the tileset name.
     */
    public function setName (name :String) :void
    {
        _name = name;
    }

    public function hashCode () :int
    {
        return StringUtil.hashCode(getName());
    }

    public function equals (other :Object) :Boolean
    {
        return other === this;
    }

    /**
     * Sets the path to the image that will be used by this tileset. This must be called before the
     * first call to {@link #getTile}.
     */
    public function setImagePath (imagePath :String) :void
    {
        _imagePath = imagePath;
    }

    /**
     * Returns the path to the composite image used by this tileset.
     */
    public function getImagePath () :String
    {
        return _imagePath;
    }

    /**
     * Returns the number of tiles in the tileset.
     */
    public function getTileCount () :int
    {
        throw new Error("abstract");
    }

    /**
     * Computes and fills in the bounds for the specified tile based on the mechanism used by the
     * derived class to do such things. The width and height of the bounds should be the size of
     * the tile image and the x and y offset should be the offset in the tileset image for the
     * image data of the specified tile.
     *
     * @param tileIndex the index of the tile whose bounds are to be computed.
     * @param bounds the rectangle object into which to fill the bounds.
     *
     * @return the rectangle passed into the bounds parameter.
     */
    public function computeTileBounds (tileIndex :int) :Rectangle
    {
        throw new Error("abstract");
    }

    /**
     * Creates a {@link Tile} object from this tileset corresponding to the specified tile id and
     * returns that tile. A null tile will never be returned, but one with an error image may be
     * returned if a problem occurs loading the underlying tileset image.
     *
     * @param tileIndex the index of the tile in the tileset. Tile indexes start with zero as the
     * upper left tile and increase by one as the tiles move left to right and top to bottom over
     * the source image.
     * @param zations colorizations to be applied to the tile image prior to returning it. These
     * may be null for uncolorized images.
     *
     * @return the tile object.
     */
    public function getTile (tileIndex :int, zations :* = null) :Tile
    {
        // Default to using our default tileset colorizations.
        if (zations == null) {
            zations = _zations;
        } else if (zations is Colorizer) {
            zations = getTileColorizations(tileIndex, zations);
        }

        var tile :Tile = null;

        // first look in the active set; if it's in use by anyone or in the cache, it will be in
        // the active set
        var key :Tile_Key = new Tile_Key(this, tileIndex, zations);
        tile = _atiles.get(key);

        // if it's not in the active set, it's not in memory; so load it
        if (tile == null) {
            tile = createTile();
            tile.key = key;
            initTile(tile, tileIndex, zations);
            _atiles.put(tile.key, tile);
        }

        // periodically report our image cache performance
        reportCachePerformance();

        return tile;
    }

    /**
     * Returns a prepared version of the image that would be used by the tile at the specified
     * index. Because tilesets are often used simply to provide access to a collection of uniform
     * images, this method is provided to bypass the creation of a {@link Tile} object when all
     * that is desired is access to the underlying image.
     */
    public function getTileImage (tileIndex :int, zations :Array, callback :Function)
        :void
    {
        // If they weren't specified, get the default zations for this tile.
        if (zations == null) {
            zations = getTileColorizations(tileIndex, null);
        }

        var bounds :Rectangle = computeTileBounds(tileIndex);
        var image :DisplayObject = null;
        if (checkTileIndex(tileIndex)) {
            if (_improv == null) {
                log.warning("Aiya! Tile set missing image provider [path=" + _imagePath + "].");
                callback(new Bitmap(ImageUtil.createErrorBitmap()));

            } else {
                _improv.getTileImage(_imagePath, bounds, zations,
                    function(result :DisplayObject) :void {
                        if (result == null) {
                            result = new Bitmap(ImageUtil.createErrorBitmap());
                        }
                        callback(result);
                    });
            }
        } else {
            callback(ImageUtil.createErrorImage(bounds.width, bounds.height));
        }
    }

    /**
     * Returns colorizations for the specified tile image. The default is to return any
     * colorizations associated with the tileset via a call to {@link #clone(Colorization[])},
     * however derived classes may have dynamic colorization policies that look up colorization
     * assignments from the supplied colorizer.
     */
    protected function getTileColorizations (tileIndex :int, rizer :Colorizer) :Array
    {
        return _zations;
    }

    /**
     * Used to ensure that the specified tile index is valid.
     */
    protected function checkTileIndex (tileIndex :int) :Boolean
    {
        var tcount :int = getTileCount();
        if (tileIndex >= 0 && tileIndex < tcount) {
            return true;
        } else {
            log.warning("Requested invalid tile [tset=" + this + ", index=" + tileIndex + "].",
  new Error());
            return false;
        }
    }

    /**
     * Creates a blank tile of the appropriate type for this tileset.
     *
     * @return a blank tile ready to be populated with its image and metadata.
     */
    protected function createTile () :Tile
    {
        return new Tile();
    }

    /**
     * Initializes the supplied tile. Derived classes can override this method to add
     * in their own tile information, but should be sure to call <code>super.initTile()</code>.
     *
     * @param tile the tile to initialize.
     * @param tileIndex the index of the tile.
     * @param zations the colorizations to be used when generating the tile image.
     */
    protected function initTile (tile :Tile, tileIndex :int, zations :Array) :void
    {
        if (_improv != null) {
            getTileImage(tileIndex, zations, function(result :DisplayObject) :void {
                tile.setImage(result);
            });
        } else {
            _pending.push([tile, tileIndex, zations]);
        }
    }

    /**
     * Reports statistics detailing the image manager cache performance and the current size of the
     * cached images.
     */
    protected function reportCachePerformance () :void
    {
        if (_improv == null ||
            _cacheStatThrottle.throttleOp()) {
            return;
        }

        // compute our estimated memory usage
        var amem :int = 0;
        var asize :int = 0;
        // first total up the active tiles
        for each (var tile :Tile in _atiles.values()) {
            if (tile != null) {
                asize++;
            }
        }
        log.info("Tile caches [seen=" + _atiles.size() + ", asize=" + asize + "].");
    }

    public function toString () :String
    {
        return "[" + toStringBuf(new String()) + "]";
    }

    /**
     * Derived classes can override this, calling <code>super.toString(buf)</code> and then
     * appending additional information to the buffer.
     */
    protected function toStringBuf (buf :String) :String
    {
        buf = buf.concat("name=", _name);
        buf = buf.concat(", path=", _imagePath);
        buf = buf.concat(", tileCount=", getTileCount());
        return buf;
    }

    public function isLoaded () :Boolean
    {
        return _isLoaded;
    }

    public function loaded () :void
    {
        _isLoaded = true;

        // Notify them all and clear our list.
        for each (var func :Function in _notifyOnLoad) {
            func(this);
        }
        _notifyOnLoad = [];
    }

    public function notifyOnLoad (func :Function) :void
    {
        if (isLoaded()) {
            func(this);
        } else {
            _notifyOnLoad.push(func);
        }
    }

    protected function populateFromXml (xml :XML) :void
    {
        _name = xml.@name;
        _imagePath = xml.imagePath;
    }

    public function clone () :Object
    {
        var newSet :TileSet = createClone();
        populateClone(newSet);
        return newSet;
    }

    public function populateClone (clone :TileSet) :void
    {
        clone._isLoaded = _isLoaded;
        clone._imagePath = _imagePath;
        clone._name = _name;
        clone._improv = _improv;
    }

    public function createClone () :TileSet
    {
        return new TileSet();
    }

    public function cloneWithZations (zations :Array) :TileSet
    {
        var newSet :TileSet = TileSet(clone());
        newSet._zations = zations;
        return newSet;
    }

    /** Whether all the media for this tileset is loaded and ready. */
    protected var _isLoaded :Boolean;

    /** The path to the file containing the tile images. */
    protected var _imagePath :String;

    /** The tileset name. */
    protected var _name :String;

    /** Colorizations to be applied to tiles created from this tileset. */
    protected var _zations :Array; /* of */ Colorization;

    /** The entity from which we obtain our tile image. */
    protected var _improv :ImageProvider;

    /** Everyone who cares when we're loaded. */
    protected var _notifyOnLoad :Array = [];

    /** Tiles awaiting an image provider. */
    protected var _pending :Array = [];

    /** A map containing weak references to all "active" tiles. */
    protected static var _atiles :WeakValueMap = new WeakValueMap(new HashMap());

    /** Throttle our cache status logging to once every 300 seconds. */
    protected static var _cacheStatThrottle :Throttle = new Throttle(1, 300000);
}
}