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

package com.threerings.miso.data {

import flash.geom.Rectangle;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.SimpleStreamableObject;
import com.threerings.io.ObjectOutputStream;

import com.threerings.util.ArrayIterator;
import com.threerings.util.ClassUtil;
import com.threerings.util.Integer;
import com.threerings.util.Iterator;
import com.threerings.util.Joiner;
import com.threerings.util.MathUtil;
import com.threerings.util.Set;
import com.threerings.util.Sets;
import com.threerings.util.StreamableHashMap;
import com.threerings.util.StringUtil;

import com.threerings.miso.util.ObjectSet;
import com.threerings.miso.data.MisoSceneModel;
import com.threerings.miso.data.SparseMisoSceneModel;
import com.threerings.miso.data.ObjectInfo;
import com.threerings.miso.data.SparseMisoSceneModel_ObjectVisitor;
import com.threerings.miso.data.SparseMisoSceneModel_Section;

/**
 * Contains miso scene data that is broken up into NxN tile sections.
 */
public class SparseMisoSceneModel extends MisoSceneModel
{
    public var swidth :int;
    public var sheight :int;
    /** The dimensions of a section of our scene. */

    /** The tileset to use when we have no tile data. */
    public var defTileSet :int = 0;

    override public function clone () :Object
    {
        var model :SparseMisoSceneModel = (ClassUtil.newInstance(this) as SparseMisoSceneModel);
        model._sections = new StreamableHashMap();
        for (var iter :Iterator = getSections(); iter.hasNext(); ) {
            var sect :SparseMisoSceneModel_Section = SparseMisoSceneModel_Section(iter.next());
            model.setSection(SparseMisoSceneModel_Section(sect.clone()));
        }
        return model;
    }

    override public function getBaseTileId (col :int, row :int) :int
    {
        var sec :SparseMisoSceneModel_Section = getSection(col, row, false);
        return (sec == null) ? -1 : sec.getBaseTileId(col, row);
    }

    override public function setBaseTile (fqBaseTileId :int, col :int, row :int) :Boolean
    {
        getSection(col, row, true).setBaseTile(col, row, fqBaseTileId);
        return true;
    }

    override public function setDefaultBaseTileSet (tileSetId :int) :void
    {
        defTileSet = tileSetId;
    }

    override public function getDefaultBaseTileSet () :int
    {
        return defTileSet;
    }

    override public function getObjects (region :Rectangle, set :ObjectSet) :void
    {
        var minx :int = MathUtil.floorDiv(region.x, swidth)*swidth;
        var maxx :int = MathUtil.floorDiv(region.x+region.width-1, swidth)*swidth;
        var miny :int = MathUtil.floorDiv(region.y, sheight)*sheight;
        var maxy :int = MathUtil.floorDiv(region.y+region.height-1, sheight)*sheight;
        for (var yy :int = miny; yy <= maxy; yy += sheight) {
            for (var xx :int = minx; xx <= maxx; xx += swidth) {
                var sec :SparseMisoSceneModel_Section = getSection(xx, yy, false);
                if (sec != null) {
                    sec.getObjects(region, set);
                }
            }
        }
    }

    override public function addObject (info :ObjectInfo) :Boolean
    {
        return getSection(info.x, info.y, true).addObject(info);
    }

    override public function updateObject (info :ObjectInfo) :void
    {
        // not efficient, but this is only done in editing situations
        removeObject(info);
        addObject(info);
    }

    override public function removeObject (info :ObjectInfo) :Boolean
    {
        var sec :SparseMisoSceneModel_Section = getSection(info.x, info.y, false);
        if (sec != null) {
            return sec.removeObject(info);
        } else {
            return false;
        }
    }

    /**
     * Adds all interesting {@link ObjectInfo} records in this scene to
     * the supplied list.
     */
    public function getInterestingObjects (list :Array) :void
    {
        for (var iter :Iterator = getSections(); iter.hasNext(); ) {
            var sect :SparseMisoSceneModel_Section = SparseMisoSceneModel_Section(iter.next());
            for each (var element :ObjectInfo in sect.objectInfo) {
                list.add(element);
            }
        }
    }

    /**
     * Don't call this method! This is only public so that the scene
     * writer can generate XML from the raw scene data.
     */
    public function getSections () :Iterator
    {
        return new ArrayIterator(_sections.values());
    }

    /**
     * Adds all {@link ObjectInfo} records in this scene to the supplied list.
     */
    public function getAllObjects (list :Array) :void
    {
        for (var iter :Iterator = getSections(); iter.hasNext(); ) {
            iter.next().getAllObjects(list);
        }
    }

    /**
     * Informs the supplied visitor of each object in this scene.
     *
     * @param interestingOnly if true, only the interesting objects will
     * be visited.
     */
    public function visitObjects (visitor :SparseMisoSceneModel_ObjectVisitor,
        interestingOnly :Boolean = false) :void
    {
        for (var iter :Iterator = getSections(); iter.hasNext(); ) {
            var sect :SparseMisoSceneModel_Section = SparseMisoSceneModel_Section(iter.next());
            for each (var oinfo :ObjectInfo in sect.objectInfo) {
                visitor.visit(oinfo);
            }
            if (!interestingOnly) {
                for (var oo :int = 0; oo < sect.objectTileIds.length; oo++) {
                    var info :ObjectInfo = new ObjectInfo(sect.objectTileIds[oo],
                        sect.objectXs[oo], sect.objectYs[oo]);
                    visitor.visit(info);
                }
            }
        }
    }

    /**
     * Don't call this method! This is only public so that the scene
     * parser can construct a scene from raw data. If only Java supported
     * class friendship.
     */
    public function setSection (section :SparseMisoSceneModel_Section) :void
    {
        _sections.put(key(section.x, section.y), section);
    }

    override public function getAllTilesets () :Set
    {
        var tilesets :Set = Sets.newSetOf(int);
        for each (var section :SparseMisoSceneModel_Section in _sections.values()) {
            section.getAllTilesets(tilesets);
        }

        tilesets.add(getDefaultBaseTileSet());

        return tilesets;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        swidth = ins.readShort();
        sheight = ins.readShort();
        defTileSet = ins.readInt();
        _sections = ins.readObject(StreamableHashMap);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeShort(swidth);
        out.writeShort(sheight);
        out.writeInt(defTileSet);
        out.writeObject(_sections);
    }

    override protected function toStringJoiner (j :Joiner) :void
    {
        super.toStringJoiner(j);
        j.add("sections", StringUtil.toString(_sections.values()));
    }

    /**
     * Returns the key for the specified section.
     */
    protected function key (x :int, y :int) :Integer
    {
        var sx :int = MathUtil.floorDiv(x, swidth);
        var sy :int = MathUtil.floorDiv(y, sheight);
        return Integer.valueOf((sx << 16) | (sy & 0xFFFF));
    }

    /** Returns the section for the specified tile coordinate. */
    protected function getSection (x :int, y :int, create :Boolean) :SparseMisoSceneModel_Section
    {
        var key :Integer = key(x, y);
        var sect :SparseMisoSceneModel_Section = _sections.get(key);
        if (sect == null && create) {
            var sx :int = MathUtil.floorDiv(x, swidth)*swidth;
            var sy :int = MathUtil.floorDiv(y, sheight)*sheight;
            _sections.put(key, sect = new SparseMisoSceneModel_Section(sx, sy, swidth, sheight));
        }
        return sect;
    }

    /** Contains our sections in row major order. */
    protected var _sections :StreamableHashMap = new StreamableHashMap();

}
}
