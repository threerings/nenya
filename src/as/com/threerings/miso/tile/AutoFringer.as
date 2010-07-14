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

package com.threerings.miso.tile {

import flash.geom.Point;
import flash.geom.Rectangle;

import flash.display.Bitmap;
import flash.display.BitmapData;

import com.threerings.display.ImageUtil;
import com.threerings.media.tile.NoSuchTileSetError;
import com.threerings.media.tile.BaseTile;
import com.threerings.media.tile.Tile;
import com.threerings.media.tile.TileSet;
import com.threerings.media.tile.TileManager;
import com.threerings.media.tile.TileUtil;
import com.threerings.util.ArrayUtil;
import com.threerings.util.Integer;
import com.threerings.util.Log;
import com.threerings.util.Maps;
import com.threerings.util.Map;
import com.threerings.util.WeakReference;
import com.threerings.miso.data.MisoSceneModel;

public class AutoFringer
{
    private var log :Log = Log.getLog(AutoFringer);

    public function AutoFringer (fringeConf :FringeConfiguration, tMgr :TileManager)
    {
        _fringeConf = fringeConf;
        _tMgr = tMgr;


        // Construct the BITS_TO_INDEX array.
        var initIdx :int;
        // first clear everything to -1 (meaning there is no tile defined)
        for (initIdx = 0; initIdx < INIT_CT; initIdx++) {
            BITS_TO_INDEX[initIdx] = -1;
        }

        // then fill in with the defined tiles.
        for (initIdx = 0; initIdx < FRINGETILES.length; initIdx++) {
            BITS_TO_INDEX[FRINGETILES[initIdx]] = initIdx;
        }

    }

    /**
     * Returns the fringe configuration used by this fringer.
     */
    public function getFringeConf () :FringeConfiguration
    {
        return _fringeConf;
    }

    /**
     * Compute and return the fringe tile to be inserted at the specified location.
     */
    public function getFringeTile (scene :MisoSceneModel, col :int, row :int, fringes :Map,
        masks :Map) :BaseTile
    {
        // get the tileset id of the base tile we are considering
        var underset :int = adjustTileSetId(scene.getBaseTileId(col, row) >> 16);

        // start with a clean temporary fringer map
        _fringers.clear();
        var passable :Boolean = true;

        // walk through our influence tiles
        for (var y :int = row - 1, maxy :int = row + 2; y < maxy; y++) {
            for (var x :int = col - 1, maxx :int = col + 2; x < maxx; x++) {
                // we sensibly do not consider ourselves
                if ((x == col) && (y == row)) {
                    continue;
                }

                // determine the tileset for this tile
                var btid :int= scene.getBaseTileId(x, y);
                var baseset :int= adjustTileSetId((btid <= 0) ?
                    scene.getDefaultBaseTileSet() : (btid >> 16));

                // determine if it fringes on our tile
                var pri :int = _fringeConf.fringesOn(baseset, underset);
                if (pri == -1) {
                    continue;
                }

                var fringer :FringerRec = FringerRec(_fringers.get(baseset));
                if (fringer == null) {
                    fringer = new FringerRec(baseset, pri);
                    _fringers.put(baseset, fringer);
                }

                // now turn on the appropriate fringebits
                fringer.bits |= FLAGMATRIX[y - row + 1][x - col + 1];

                // See if a tile that fringes on us kills our passability,
                // but don't count the default base tile against us, as
                // we allow users to splash in the water.
                if (passable && (btid > 0)) {
                    try {
                        var bt :BaseTile = BaseTile(_tMgr.getTile(btid));
                        passable = bt.isPassable();
                    } catch (nstse :NoSuchTileSetError) {
                        log.warning("Autofringer couldn't find a base set while attempting to " +
                            "figure passability", nstse);
                    }
                }
            }
        }

        // if nothing fringed, we're done
        var numfringers :int = _fringers.size();
        if (numfringers == 0) {
            return null;
        }

        // otherwise compose a FringeTile from the specified fringes
        var frecs :Array = new Array(numfringers);
        for (var ii :int = 0, pp :int = 0; ii < 16; ii++) {
            var rec :FringerRec = FringerRec(_fringers.get(ii));
            if (rec != null) {
                frecs[pp++] = rec;
            }
        }

        return composeFringeTile(frecs, fringes, TileUtil.getTileHash(col, row), passable, masks);
    }

    /**
     * Compose a FringeTile out of the various fringe images needed.
     */
    protected function composeFringeTile (fringers :Array,
        fringes :Map, hashValue :int, passable :Boolean, masks :Map) :FringeTile
    {
        // sort the array so that higher priority fringers get drawn first
        ArrayUtil.sort(fringers);

        // Generate an identifier for the fringe tile being created as an array of the keys of its
        // component tiles in the order they'll be drawn in the fringe tile.
        var fringeIds :Array = [];
        for each (var fringer :FringerRec in fringers) {
            var indexes :Array = getFringeIndexes(fringer.bits);
            var tsr :FringeTileSetRecord = _fringeConf.getFringe(fringer.baseset, hashValue);
            var fringeset :int = tsr.fringe_tsid;
            for each (var index :int in indexes) {
                // Add a key for this tile as an int containing its base tile, the fringe set it's
                // working with and the index used in that set.
                fringeIds.push((fringer.baseset << 20) + (fringeset << 8) + index);
            }
        }
        var frTile :FringeTile = new FringeTile(fringeIds, passable);

        // If the fringes map contains something with the same fringe identifier, this will pull
        // it out and we can use it instead.
        var result :WeakReference = fringes.get(frTile);
        if (result != null) {
            var fringe :FringeTile = result.get();
            if (fringe != null) {
                return fringe;
            }
        }

        // There's no fringe with the same identifier, so we need to create the tile.
        var img :Bitmap = null;
        getTileImageHelper1(img, hashValue, masks, fringers, 0, function (result :Bitmap) :void {
                frTile.setImage(result);
            });
        fringes.put(frTile, new WeakReference(frTile));
        return frTile;
    }

    protected function getTileImageHelper1 (img :Bitmap, hashValue :int, masks :Map,
        fringers :Array, fringerIdx :int, callback :Function) :void
    {
        var fringer :FringerRec = fringers[fringerIdx];
        var indexes :Array = getFringeIndexes(fringer.bits);
        var tsr :FringeTileSetRecord = _fringeConf.getFringe(fringer.baseset, hashValue);
        getTileImageHelper0(img, tsr, fringer.baseset, hashValue, masks, indexes, 0,
            function (result :Bitmap) :void {
                if (fringerIdx == fringers.size() - 1) {
                    callback(result);
                } else {
                    getTileImageHelper1(img, hashValue, masks, fringers, fringerIdx + 1, callback);
                }
            });
    }

    protected function getTileImageHelper0 (img :Bitmap, tsr :FringeTileSetRecord,
        baseset :int, hashValue :int, masks :Map, indexes :Array,
        indexIdx :int, callback :Function) :void
    {
        try {
            getTileImage(img, tsr, baseset, indexes[indexIdx], hashValue, masks,
                function (result :Bitmap) :void {
                    if (indexIdx == indexes.size() - 1) {
                        callback(result);
                    } else {
                        getTileImageHelper0(img, tsr, baseset, hashValue, masks, indexes,
                            indexIdx + 1, callback);
                    }
                });
        } catch (nstse :NoSuchTileSetError) {
            log.warning("Autofringer couldn't find a needed tileset", nstse);
            callback(null);
        }
    }

    /**
     * Retrieve or compose an image for the specified fringe.
     */
    protected function getTileImage (img :Bitmap, tsr :FringeTileSetRecord ,
        baseset :int, index :int, hashValue :int, masks :Map, callback :Function) :void
    {
        var fringeset :int = tsr.fringe_tsid;
        var fset :TileSet = _tMgr.getTileSet(fringeset);
        if (!tsr.mask) {
            // oh good, this is easy
            var stamp :Tile = fset.getTile(index);
            callback(stampTileImage(stamp, img, stamp.getWidth(), stamp.getHeight()));
            return;
        }

        // otherwise, it's a mask..
        var maskkey :int = (baseset << 20) + (fringeset << 8) + index;
        var mask :Bitmap = masks.get(maskkey);
        if (mask == null) {
            _tMgr.getTileSet(fringeset).getTileImage(index, null, function (fsrc :Bitmap) :void {
                _tMgr.getTileSet(baseset).getTileImage(0, null, function (bsrc :Bitmap) :void {
                    mask = composeMaskedImage(fsrc, bsrc);
                    masks.put(maskkey, mask);
                    callback(stampTileImage(mask, img, mask.width, mask.height));
                });
            });

        } else {
            callback(stampTileImage(mask, img, mask.width, mask.height));
        }
    }



    /**
     * Create an image using the alpha channel from the first and the RGB values from the second.
     */
    public static function composeMaskedImage (mask :Bitmap, base :Bitmap) :Bitmap
    {
        var data :BitmapData = new BitmapData(base.width, base.height);
        data.copyPixels(base.bitmapData, new Rectangle(0, 0, base.width, base.height),
            new Point(0, 0), mask.bitmapData, new Point(0, 0), false);
        return new Bitmap(data);
    }

    /** Helper function for {@link #getTileImage}. */
    protected function stampTileImage (stamp :Object, ftimg :Bitmap, width :int,
        height :int) :Bitmap
    {
        // create the target image if necessary
        if (ftimg == null) {
            ftimg = new Bitmap(new BitmapData(width, height, true, 0x00000000));
        }
        var img :Bitmap;
        if (stamp is Tile) {
            img = Bitmap((Tile(stamp)).getImage());
        } else {
            img = Bitmap(stamp);
        }

        ftimg.bitmapData.draw(img);

        return ftimg;
    }

    /**
     * Get the fringe index specified by the fringebits. If no index is available, try breaking
     * down the bits into contiguous regions of bits and look for indexes for those.
     */
    protected function getFringeIndexes (bits :int) :Array
    {
        var index :int = BITS_TO_INDEX[bits];
        if (index != -1) {
            return [index];
        }

        // otherwise, split the bits into contiguous components

        // look for a zero and start our first split
        var start :int = 0;
        while ((((1 << start) & bits) != 0) && (start < NUM_FRINGEBITS)) {
            start++;
        }

        if (start == NUM_FRINGEBITS) {
            // we never found an empty fringebit, and since index (above)
            // was already -1, we have no fringe tile for these bits.. sad.
            return new Array(0);
        }

        var indexes :Array = [];
        var weebits :int = 0;
        for (var ii :int = (start + 1) % NUM_FRINGEBITS; ii != start;
             ii = (ii + 1) % NUM_FRINGEBITS) {

            if (((1 << ii) & bits) != 0) {
                weebits |= (1 << ii);
            } else if (weebits != 0) {
                index = BITS_TO_INDEX[weebits];
                if (index != -1) {
                    indexes.add(index);
                }
                weebits = 0;
            }
        }
        if (weebits != 0) {
            index = BITS_TO_INDEX[weebits];
            if (index != -1) {
                indexes.add(index);
            }
        }

        return indexes;
    }

    /**
     * Allow subclasses to apply arbitrary modifications to tileset ids for whatever nefarious
     * purposes they may have.
     */
    protected function adjustTileSetId (tileSetId :int) :int
    {
        // by default, nothing.
        return tileSetId;
    }

    // fringe bits
    // see docs/miso/fringebits.png
    //
    protected static const NORTH     :int = 1 << 0;
    protected static const NORTHEAST :int = 1 << 1;
    protected static const EAST      :int = 1 << 2;
    protected static const SOUTHEAST :int = 1 << 3;
    protected static const SOUTH     :int = 1 << 4;
    protected static const SOUTHWEST :int = 1 << 5;
    protected static const WEST      :int = 1 << 6;
    protected static const NORTHWEST :int = 1 << 7;

    protected static const NUM_FRINGEBITS :int = 8;

    // A matrix mapping adjacent tiles to which fringe bits they affect.
    // (x and y are offset by +1, since we can't have -1 as an array index)
    // again, see docs/miso/fringebits.png
    //
    protected static const FLAGMATRIX :Array= [
        [ NORTHEAST, (NORTHEAST | EAST | SOUTHEAST), SOUTHEAST ],
        [ (NORTHWEST | NORTH | NORTHEAST), 0, (SOUTHEAST | SOUTH | SOUTHWEST) ],
        [ NORTHWEST, (NORTHWEST | WEST | SOUTHWEST), SOUTHWEST ]
    ];

    /**
     * The fringe tiles we use. These are the 17 possible tiles made up of continuous fringebits
     * sections. Huh? see docs/miso/fringebits.png
     */
    protected static const FRINGETILES :Array = [
        SOUTHEAST,
        SOUTHWEST | SOUTH | SOUTHEAST,
        SOUTHWEST,
        NORTHEAST | EAST | SOUTHEAST,
        NORTHWEST | WEST | SOUTHWEST,
        NORTHEAST,
        NORTHWEST | NORTH | NORTHEAST,
        NORTHWEST,

        SOUTHWEST | WEST | NORTHWEST | NORTH | NORTHEAST,
        NORTHWEST | NORTH | NORTHEAST | EAST | SOUTHEAST,
        NORTHWEST | WEST | SOUTHWEST | SOUTH | SOUTHEAST,
        SOUTHWEST | SOUTH | SOUTHEAST | EAST | NORTHEAST,

        NORTHEAST | NORTH | NORTHWEST | WEST | SOUTHWEST | SOUTH | SOUTHEAST,
        SOUTHEAST | EAST | NORTHEAST | NORTH | NORTHWEST | WEST | SOUTHWEST,
        SOUTHWEST | SOUTH | SOUTHEAST | EAST | NORTHEAST | NORTH | NORTHWEST,
        NORTHWEST | WEST | SOUTHWEST | SOUTH | SOUTHEAST | EAST | NORTHEAST,

        // all the directions!
        NORTH | NORTHEAST | EAST | SOUTHEAST | SOUTH | SOUTHWEST | WEST | NORTHWEST
    ];

    // A reverse map of the above array, for quickly looking up which tile
    // we want.
    protected const INIT_CT :int = (1 << NUM_FRINGEBITS);

    protected const BITS_TO_INDEX :Array = new Array(INIT_CT);

    protected var _tMgr :TileManager;
    protected var _fringeConf :FringeConfiguration;
    protected var _fringers :Map = Maps.newMapOf(int);
}
}

import com.threerings.util.Comparable;

/**
 * A record for holding information about a particular fringe as we're computing what it will
 * look like.
 */
class FringerRec
    implements Comparable
{
    public var baseset :int;
    public var priority :int;
    public var bits :int;

    public function FringerRec (base :int, pri :int)
    {
        baseset = base;
        priority = pri;
    }

    public function compareTo (o :Object) :int
    {
        return priority - FringerRec(o).priority;
    }

    public function toString () :String
    {
        return "[base=" + baseset + ", pri=" + priority + ", bits="
            + bits.toString(16) + "]";
    }
}
