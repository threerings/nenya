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

import com.threerings.util.Set;

public interface TileSetRepository {

    /**
     * Returns an iterator over the identifiers of all {@link TileSet}
     * objects available.
     */
    function enumerateTileSetIds () :Array;

    /**
     * Returns an iterator over all {@link TileSet} objects available.
     */
    function enumerateTileSets () :Array;

    /**
     * Returns the {@link TileSet} with the specified tile set
     * identifier. The repository is responsible for configuring the tile
     * set with an image provider.
     *
     * @exception NoSuchTileSetException thrown if no tileset exists with
     * the specified identifier.
     * @exception PersistenceException thrown if an error occurs
     * communicating with the underlying persistence mechanism.
     */
    function getTileSet (tileSetId :int) :TileSet;

    /**
     * Returns the unique identifier of the {@link TileSet} with the
     * specified tile set name.
     *
     * @exception NoSuchTileSetException thrown if no tileset exists with
     * the specified name.
     * @exception PersistenceException thrown if an error occurs
     * communicating with the underlying persistence mechanism.
     */
    function getTileSetId (setName :String) :int;

    /**
     * Returns the {@link TileSet} with the specified tile set name. The
     * repository is responsible for configuring the tile set with an
     * image provider.
     *
     * @exception NoSuchTileSetException thrown if no tileset exists with
     * the specified name.
     * @exception PersistenceException thrown if an error occurs
     * communicating with the underlying persistence mechanism.
     */
    function getTileSetByName (setName :String) :TileSet;

    /**
     * Ensures we are ready with quick-access to all the specified tilesets.
     */
    function ensureLoaded (tileSets :Set, completeCallback :Function,
        progressCallback :Function) :void;
}
}