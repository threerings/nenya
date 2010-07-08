//
// $Id: MisoSceneModel.java 872 2010-01-13 18:28:28Z ray $
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

import flash.geom.Rectangle;

import com.threerings.miso.util.ObjectSet;
import com.threerings.io.SimpleStreamableObject;
import com.threerings.miso.data.MisoSceneModel;
import com.threerings.io.ObjectInputStream;
import com.threerings.miso.data.ObjectInfo;
import com.threerings.util.ClassUtil;
import com.threerings.util.Cloneable;
import com.threerings.util.Set;
import com.threerings.io.ObjectOutputStream;

/**
 * Contains basic information for a miso scene model that is shared among
 * the specialized model implementations.
 */
public /*abstract*/ class MisoSceneModel extends SimpleStreamableObject
    implements Cloneable
{
    public function clone () :Object
    {
        return (ClassUtil.newInstance(this) as MisoSceneModel);
    }

    /**
     * Returns the fully qualified tile id of the base tile at the
     * specified coordinates. <code>-1</code> will be returned if there is
     * no tile at the specified coordinate.
     */
    public function getBaseTileId (x :int, y :int) :int
    {
        throw new Error("abstract");
    }

    public function setBaseTile (x :int, y :int, tileId :int) :Boolean
    {
        throw new Error("abstract");
    }

    public function setDefaultBaseTileSet (tileSetId :int) :void
    {
        // nothing doing
    }

    /**
     * Scene models can return a default tileset to be used when no base
     * tile data exists for a particular tile.
     */
    public function getDefaultBaseTileSet () :int
    {
        return 0;
    }

    /**
     * Populates the supplied object set with info on all objects whose
     * origin falls in the requested region.
     */
    public function getObjects (region :Rectangle, set :ObjectSet) :void
    {
        throw new Error("abstract");
    }

    public function addObject (info :ObjectInfo) :Boolean
    {
        throw new Error("abstract");
    }

    public function updateObject (info :ObjectInfo) :void
    {
        throw new Error("abstract");
    }

    public function removeObject (info :ObjectInfo) :Boolean
    {
        throw new Error("abstract");
    }

    public function getAllTilesets () :Set
    {
        throw new Error("abstract");
    }
}
}
