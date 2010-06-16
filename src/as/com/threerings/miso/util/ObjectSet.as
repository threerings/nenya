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

package com.threerings.miso.util {

import com.threerings.miso.data.ObjectInfo;
import com.threerings.util.ArrayUtil;
import com.threerings.util.Log;

/**
 * Used to store an (arbitrarily) ordered, low-impact iteratable (doesn't
 * require object creation), set of {@link ObjectInfo} instances.
 */
public class ObjectSet
{
    public static const log :Log = Log.getLog(ObjectSet);

    /**
     * Inserts the supplied object into the set.
     *
     * @return true if it was inserted, false if the object was already in
     * the set.
     */
    public function insert (info :ObjectInfo) :Boolean
    {
        // bail if it's already in the set
        var ipos :int = indexOf(info);
        if (ipos >= 0) {
            // log a warning because the caller shouldn't be doing this
            log.warning("Requested to add an object to a set that already " +
                        "contains such an object [ninfo=" + info +
                        ", oinfo=" + _objs[ipos] + "].", new Error());
            return false;
        }

        // otherwise insert it
        ipos = -(ipos+1);
        _objs.splice(ipos, 0, info);
        return true;
    }

    /**
     * Returns true if the specified object is in the set, false if it is
     * not.
     */
    public function contains (info :ObjectInfo) :Boolean
    {
        return (indexOf(info) >= 0);
    }

    /**
     * Returns the number of objects in this set.
     */
    public function size () :int
    {
        return _objs.length;
    }

    /**
     * Returns the object with the specified index. The index must & be
     * between <code>0</code> and {@link #size}<code>-1</code>.
     */
    public function get (index :int) :ObjectInfo
    {
        return ObjectInfo(_objs[index]);
    }

    /**
     * Removes the object at the specified index.
     */
    public function removeAt (index :int) :void
    {
        ArrayUtil.removeFirst(_objs, index);
    }

    /**
     * Removes the specified object from the set.
     *
     * @return true if it was removed, false if it was not in the set.
     */
    public function remove (info :ObjectInfo) :Boolean
    {
        var opos :int = indexOf(info);
        if (opos >= 0) {
            removeAt(opos);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Clears out the contents of this set.
     */
    public function clear () :void
    {
        _objs.length = 0;
    }

    /**
     * Converts the contents of this object set to an array.
     */
    public function toArray () :Array
    {
        return ArrayUtil.copyOf(_objs);
    }

    public function toString () :String
    {
        var result :String = "[";
        for (var ii :int = 0; ii < _objs.length; ii++) {
            if (ii > 0) {
                result += ", ";
            }
            result += _objs[ii];
        }
        return result += "]";
    }

    /** We simply sort the objects in order of their hash code. We don't
     * care about their order, it exists only to support binary search. */
    protected function searchCompare (o1 :Object, o2 :Object) :int
    {
        var do1 :ObjectInfo = ObjectInfo(o1);
        var do2 :ObjectInfo = ObjectInfo(o2);
        if (do1.tileId == do2.tileId) {
            return ((do1.x << 16) + do1.y) - ((do2.x << 16) + do2.y);
        } else {
            return do1.tileId - do2.tileId;
        }
    };

    /**
     * Returns the index of the object or it's insertion index if it is
     * not in the set.
     */
    protected function indexOf (info :ObjectInfo) :int
    {
        return ArrayUtil.binarySearch(_objs, 0, _objs.length, info, searchCompare);
    }

    /** Our sorted array of objects. */
    protected var _objs :Array = new Array();
}
}
