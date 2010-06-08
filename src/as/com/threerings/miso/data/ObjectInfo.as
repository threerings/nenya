//
// $Id: ObjectInfo.java 872 2010-01-13 18:28:28Z ray $
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

package com.threerings.miso.data {

import com.threerings.util.Hashable;
import com.threerings.util.ClassUtil;
import com.threerings.util.Cloneable;
import com.threerings.util.StringUtil;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.miso.data.ObjectInfo;
import com.threerings.media.tile.TileUtil;

/**
 * Contains information about an object in a Miso scene.
 */
public class ObjectInfo extends SimpleStreamableObject
    implements Cloneable, Hashable
{
    /** The fully qualified object tile id. */
    public var tileId :int;

    /** The x and y tile coordinates of the object. */
    public var x :int;
    public var y :int;

    /** Don't access this directly unless you are serializing this
     * instance. Use {@link #getPriority} instead. */
    public var priority :int = 0;

    /** The action associated with this object or null if it has no
     * action. */
    public var action :String;

    /** A "spot" associated with this object (specified as an offset from
     * the fine coordinates of the object's origin tile). */
    public var sx :int;
    public var sy :int;

    /** The orientation of the "spot" associated with this object. */
    public var sorient :int;

    /** Up to two colorization assignments for this object. */
    public var zations :int;

    public function ObjectInfo (tileId :int = 0, x :int = 0, y :int = 0)
    {
        this.tileId = tileId;
        this.x = x;
        this.y = y;
    }

    public function hashCode () :int
    {
        return x ^ y ^ tileId;
    }

    public function clone () :Object
    {
        var info :ObjectInfo = (ClassUtil.newInstance(this) as ObjectInfo);
        info.tileId = tileId;
        info.x = x;
        info.y = y;
        info.priority = priority;
        info.action = action;
        info.sx = sx;
        info.sy = sy;
        info.sorient = sorient;
        info.zations = zations;
        return info;
    }

    public function equals (other :Object) :Boolean
    {
        if (other is ObjectInfo) {
            var ooi :ObjectInfo = ObjectInfo(other);
            return (x == ooi.x && y == ooi.y && tileId == ooi.tileId);
        } else {
            return false;
        }
    }

    /**
     * Returns the render priority of this object tile.
     */
    public function getPriority () :int
    {
        return priority;
    }

    /**
     * Returns the primary colorization assignment.
     */
    public function getPrimaryZation () :int
    {
        return (zations & 0xFF);
    }

    /**
     * Returns the secondary colorization assignment.
     */
    public function getSecondaryZation () :int
    {
        return ((zations >> 16) & 0xFF);
    }

    /**
     * Returns the tertiary colorization assignment.
     */
    public function getTertiaryZation () :int
    {
        return ((zations >> 24) & 0xFF);
    }

    /**
     * Returns the quaternary colorization assignment.
     */
    public function getQuaternaryZation () :int
    {
        return ((zations >> 8) & 0xFF);
    }

    /**
     * Sets the primary and secondary colorization assignments.
     */
    public function setZations (primary :int, secondary :int, tertiary :int, quaternary :int) :void
    {
        zations = (primary | (secondary << 16) | (tertiary << 24) | (quaternary << 8));
    }

    /**
     * Returns true if this object info contains non-default data for
     * anything other than the tile id and coordinates.
     */
    public function isInteresting () :Boolean
    {
        return (!StringUtil.isBlank(action) || priority != 0 ||
                sx != 0 || sy != 0 || zations != 0);
    }

    /** Enhances our {@link SimpleStreamableObject#toString} output. */
    public function tileIdToString () :String
    {
        return (TileUtil.getTileSetId(tileId) + ":" +
                TileUtil.getTileIndex(tileId));
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        tileId = ins.readInt();
        x = ins.readInt();
        y = ins.readInt();
        priority = ins.readByte();
        action = ins.readField(String);
        sx = ins.readByte();
        sy = ins.readByte();
        sorient = ins.readByte();
        zations = ins.readInt();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(tileId);
        out.writeInt(x);
        out.writeInt(y);
        out.writeByte(priority);
        out.writeField(action);
        out.writeByte(sx);
        out.writeByte(sy);
        out.writeByte(sorient);
        out.writeInt(zations);
    }

}
}
