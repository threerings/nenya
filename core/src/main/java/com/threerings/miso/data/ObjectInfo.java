//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// https://github.com/threerings/nenya
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

package com.threerings.miso.data;

import com.samskivert.util.StringUtil;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.media.tile.TileUtil;

/**
 * Contains information about an object in a Miso scene.
 */
public class ObjectInfo extends SimpleStreamableObject
    implements Cloneable
{
    /** The fully qualified object tile id. */
    public int tileId;

    /** The x and y tile coordinates of the object. */
    public int x, y;

    /** Don't access this directly unless you are serializing this
     * instance. Use {@link #getPriority} instead. */
    public byte priority = 0;

    /** The action associated with this object or null if it has no
     * action. */
    public String action;

    /** A "spot" associated with this object (specified as an offset from
     * the fine coordinates of the object's origin tile). */
    public byte sx, sy;

    /** The orientation of the "spot" associated with this object. */
    public byte sorient;

    /** Up to two colorization assignments for this object. */
    public int zations;

    /**
     * Convenience constructor.
     */
    public ObjectInfo (int tileId, int x, int y)
    {
        this.tileId = tileId;
        this.x = x;
        this.y = y;
    }

    /**
     * Creates an object info that is a copy of the supplied info.
     */
    public ObjectInfo (ObjectInfo other)
    {
        this.tileId = other.tileId;
        this.x = other.x;
        this.y = other.y;
        this.priority = other.priority;
        this.action = other.action;
        this.sx = other.sx;
        this.sy = other.sy;
        this.sorient = other.sorient;
        this.zations = other.zations;
    }

    /**
     * Zero argument constructor needed for unserialization.
     */
    public ObjectInfo ()
    {
    }

    /**
     * Returns the render priority of this object tile.
     */
    public int getPriority ()
    {
        return priority;
    }

    /**
     * Returns the primary colorization assignment.
     */
    public int getPrimaryZation ()
    {
        return (zations & 0xFF);
    }

    /**
     * Returns the secondary colorization assignment.
     */
    public int getSecondaryZation ()
    {
        return ((zations >> 16) & 0xFF);
    }

    /**
     * Returns the tertiary colorization assignment.
     */
    public int getTertiaryZation ()
    {
        return ((zations >> 24) & 0xFF);
    }

    /**
     * Returns the quaternary colorization assignment.
     */
    public int getQuaternaryZation ()
    {
        return ((zations >> 8) & 0xFF);
    }

    /**
     * Sets the primary and secondary colorization assignments.
     */
    public void setZations (byte primary, byte secondary, byte tertiary, byte quaternary)
    {
        zations = (primary | (secondary << 16) | (tertiary << 24) | (quaternary << 8));
    }

    /**
     * Returns true if this object info contains non-default data for
     * anything other than the tile id and coordinates.
     */
    public boolean isInteresting ()
    {
        return (!StringUtil.isBlank(action) || priority != 0 ||
                sx != 0 || sy != 0 || zations != 0);
    }

    @Override
    public boolean equals (Object other)
    {
        if (other instanceof ObjectInfo) {
            ObjectInfo ooi = (ObjectInfo)other;
            return (x == ooi.x && y == ooi.y && tileId == ooi.tileId);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode ()
    {
        return x ^ y ^ tileId;
    }

    @Override
    public ObjectInfo clone ()
    {
        try {
            return (ObjectInfo) super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new AssertionError(cnse);
        }
    }

    /** Enhances our {@link SimpleStreamableObject#toString} output. */
    public String tileIdToString ()
    {
        return (TileUtil.getTileSetId(tileId) + ":" +
                TileUtil.getTileIndex(tileId));
    }
}
