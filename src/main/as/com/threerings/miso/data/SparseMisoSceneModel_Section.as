//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
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

package com.threerings.miso.data {

import flash.geom.Rectangle;

import com.threerings.io.ObjectOutputStream;
import com.threerings.io.ObjectInputStream;
import com.threerings.io.SimpleStreamableObject;
import com.threerings.io.TypedArray;
import com.threerings.media.tile.TileUtil;
import com.threerings.miso.util.ObjectSet;
import com.threerings.util.ArrayUtil;
import com.threerings.util.ClassUtil;
import com.threerings.util.Cloneable;
import com.threerings.util.Log;
import com.threerings.util.Set;
import com.threerings.util.StringUtil;

public class SparseMisoSceneModel_Section extends SimpleStreamableObject
    implements Cloneable
{
    /** The tile coordinate of our upper leftmost tile. */
    public var x :int;
    public var y :int;

    /** The width of this section in tiles. */
    public var width :int;

    /** The combined tile ids (tile set id and tile id) for our
     * section (in row major order). */
    public var baseTileIds :TypedArray;

    /** The combined tile ids (tile set id and tile id) of the
     * "uninteresting" tiles in the object layer. */
    public var objectTileIds :TypedArray = TypedArray.create(int);

    /** The x coordinate of the "uninteresting" tiles in the object
     * layer. */
    public var objectXs :TypedArray = TypedArray.createShort();

    /** The y coordinate of the "uninteresting" tiles in the object
     * layer. */
    public var objectYs :TypedArray = TypedArray.createShort();

    /** Information records for the "interesting" objects in the
     * object layer. */
    public var objectInfo :TypedArray = TypedArray.create(ObjectInfo);

    /**
     * Creates a new scene section with the specified dimensions.
     */
    public function SparseMisoSceneModel_Section (
        x :int = 0, y :int = 0, width :int = 0, height :int = 0)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        baseTileIds = TypedArray.create(int, ArrayUtil.create(width*height));
    }

    public function getBaseTileId (col :int, row :int) :int
    {
        if (col < x || col >= (x+width) || row < y || row >= (y+width)) {
            log.warning("Requested bogus tile +" + col + "+" + row +
                        " from " + this + ".");
            return -1;
        } else {
            return baseTileIds[(row-y)*width+(col-x)];
        }
    }

    public function setBaseTile (col :int, row :int, fqBaseTileId :int) :void
    {
        baseTileIds[(row-y)*width+(col-x)] = fqBaseTileId;
    }

    public function addObject (info :ObjectInfo) :Boolean
    {
        // sanity check: see if there is already an object of this
        // type at these coordinates
        var dupidx :int;
        if ((dupidx = ArrayUtil.indexOf(objectInfo, info)) != -1) {
            log.warning("Refusing to add duplicate object [ninfo=" + info +
			", oinfo=" + objectInfo[dupidx] + "].");
            return false;
        }
        if ((dupidx = indexOfUn(info)) != -1) {
            log.warning("Refusing to add duplicate object " +
			"[info=" + info + "].");
            return false;
        }

        if (info.isInteresting()) {
            objectInfo = TypedArray(objectInfo.concat(info));
        } else {
            objectTileIds = TypedArray(objectTileIds.concat(info.tileId));
            objectXs = TypedArray(objectXs.concat(info.x));
            objectYs = TypedArray(objectYs.concat(info.y));
        }
        return true;
    }

    public function removeObject (info :ObjectInfo) :Boolean
    {
        // look for it in the interesting info array
        var oidx :int = ArrayUtil.indexOf(objectInfo, info);
        if (oidx != -1) {
            objectInfo = TypedArray(ArrayUtil.splice(objectInfo, oidx, 1));
            return true;
        }

        // look for it in the uninteresting arrays
        oidx = indexOfUn(info);
        if (oidx != -1) {
            objectTileIds = TypedArray(ArrayUtil.splice(objectTileIds, oidx, 1));
            objectXs = TypedArray(ArrayUtil.splice(objectXs, oidx, 1));
            objectYs = TypedArray(ArrayUtil.splice(objectYs, oidx, 1));
            return true;
        }

        return false;
    }

    /**
     * Returns the index of the specified object in the uninteresting
     * arrays or -1 if it is not in this section as an uninteresting
     * object.
     */
    protected function indexOfUn (info :ObjectInfo) :int
    {
        for (var ii :int = 0; ii < objectTileIds.length; ii++) {
            if (objectTileIds[ii] == info.tileId &&
                objectXs[ii] == info.x && objectYs[ii] == info.y) {
                return ii;
            }
        }
        return -1;
    }

    public function getAllObjects (list :Array) :void
    {
        for each (var info :ObjectInfo in objectInfo) {
            list.add(info);
        }
        for (var ii :int= 0; ii < objectTileIds.length; ii++) {
            var x :int = objectXs[ii];
            var y :int = objectYs[ii];
            list.add(new ObjectInfo(objectTileIds[ii], x, y));
        }
    }

    public function getObjects (region :Rectangle, set :ObjectSet) :void
    {
        // first look for intersecting interesting objects
        for each (var info :ObjectInfo in objectInfo) {
            if (region.contains(info.x, info.y)) {
                set.insert(info);
            }
        }

        // now look for intersecting non-interesting objects
        for (var ii :int = 0; ii < objectTileIds.length; ii++) {
            var x :int = objectXs[ii];
            var y :int = objectYs[ii];
            if (region.contains(x, y)) {
                set.insert(new ObjectInfo(objectTileIds[ii], x, y));
            }
        }
    }

    /**
     * Returns true if this section contains no data beyond the default.
     * Used when saving a sparse scene: we omit blank sections.
     */
    public function isBlank () :Boolean
    {
        if ((objectTileIds.length != 0) || (objectInfo.length != 0)) {
            return false;
        }
        for each (var baseTileId :int in baseTileIds) {
            if (baseTileId != 0) {
                return false;
            }
        }

        return true;
    }

    public function clone () :Object
    {
        var section :SparseMisoSceneModel_Section =
            (ClassUtil.newInstance(this) as SparseMisoSceneModel_Section);
        section.x = x;
        section.y = y;
        section.width = width;
        section.baseTileIds = TypedArray(ArrayUtil.copyOf(baseTileIds));
        section.objectTileIds = TypedArray(ArrayUtil.copyOf(objectTileIds));
        section.objectXs = TypedArray(ArrayUtil.copyOf(objectXs));
        section.objectYs = TypedArray(ArrayUtil.copyOf(objectYs));
        section.objectInfo = TypedArray.create(ObjectInfo);
        for (var ii :int = 0; ii < objectInfo.length; ii++) {
            section.objectInfo[ii] = objectInfo[ii].clone();
        }
        return section;
    }

    override public function toString () :String
    {
        if (width == 0 || baseTileIds == null) {
            return "<no bounds>";
        } else {
            return width + "x" + (baseTileIds.length / width) + "+" + x + ":" + y + ":" +
                objectInfo.length;
        }
    }

    /**
     * Adds all tilesets we reference to the set.
     */
    public function getAllTilesets (tilesets :Set) :void
    {
        for each (var base :int in baseTileIds) {
            var baseSetId :int = TileUtil.getTileSetId(base);
            if (baseSetId != 0) {
                tilesets.add(baseSetId);
            }
        }
        for each (var obj :int in objectTileIds) {
            tilesets.add(TileUtil.getTileSetId(obj));
        }

        for each (var objInfo :ObjectInfo in objectInfo) {
            tilesets.add(TileUtil.getTileSetId(objInfo.tileId));
        }
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        x = ins.readShort();
        y = ins.readShort();
        width = ins.readInt();
        baseTileIds = ins.readField(TypedArray.getJavaType(int));
        objectTileIds = ins.readField(TypedArray.getJavaType(int));
        objectXs = ins.readField(TypedArray.getJavaShortType());
        objectYs = ins.readField(TypedArray.getJavaShortType());
        objectInfo = TypedArray(ins.readObject());
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeShort(x);
        out.writeShort(y);
        out.writeInt(width);
        out.writeField(baseTileIds);
        out.writeField(objectTileIds);
        out.writeField(objectXs);
        out.writeField(objectYs);
        out.writeField(objectInfo);
    }

    private static const log :Log = Log.getLog(SparseMisoSceneModel_Section);
}
}
