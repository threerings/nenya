//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
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

import com.threerings.util.Log;
import com.threerings.util.Set;

/**
 * The tile manager provides a simplified interface for retrieving and caching tiles. Tiles can be
 * loaded in two different ways. An application can load a tileset by hand, specifying the path to
 * the tileset image and all of the tileset metadata necessary for extracting the image tiles, or
 * it can provide a tileset repository which loads up tilesets using whatever repository mechanism
 * is implemented by the supplied repository. In the latter case, tilesets are loaded by a unique
 * identifier.
 *
 * <p> Loading tilesets by hand is intended for things like toolbar icons or games with a single
 * set of tiles (think Stratego, for example). Loading tilesets from a repository supports games
 * with vast numbers of tiles to which more tiles may be added on the fly (think the tiles for an
 * isometric-display graphical MUD).
 */
public class TileManager
{
    private static var log :Log = Log.getLog(TileManager);

    /**
     * Sets the tileset repository that will be used by the tile manager when tiles are requested
     * by tileset id.
     */
    public function setTileSetRepository (setrep :TileSetRepository) :void
    {
        _setrep = setrep;
    }

    /**
     * Returns the tileset repository currently in use.
     */
    public function getTileSetRepository () :TileSetRepository
    {
        return _setrep;
    }

    /**
     * Returns the tileset with the specified id. Tilesets are fetched from the tileset repository
     * supplied via {@link #setTileSetRepository}, and are subsequently cached.
     *
     * @param tileSetId the unique identifier for the desired tileset.
     *
     * @exception NoSuchTileSetError thrown if no tileset exists with the specified id or if
     * an underlying error occurs with the tileset repository's persistence mechanism.
     */
    public function getTileSet (tileSetId :int) :TileSet
    {
        // make sure we have a repository configured
        if (_setrep == null) {
            throw new NoSuchTileSetError(tileSetId);
        }

        try {
            return _setrep.getTileSet(tileSetId);
        } catch (pe :Error) {
            log.warning("Failure loading tileset", "id", tileSetId, pe);
            throw new NoSuchTileSetError(tileSetId);
        }

        // Unreachable.
        return null;
    }

    /**
     * Returns the tileset with the specified name.
     *
     * @throws NoSuchTileSetError if no tileset with the specified name is available via our
     * configured tile set repository.
     */
    public function getTileSetByName (name :String) :TileSet
    {
        // make sure we have a repository configured
        if (_setrep == null) {
            throw new NoSuchTileSetError(name);
        }

        try {
            return _setrep.getTileSetByName(name);
        } catch (pe :Error) {
            log.warning("Failure loading tileset", "name", name, "error", pe);
            throw new NoSuchTileSetError(name);
        }

        // Unreachable.
        return null;
    }

    /**
     * Returns the {@link Tile} object with the specified fully qualified tile id. The supplied
     * colorizer will be used to recolor the tile.
     *
     * @see TileUtil#getFQTileId
     */
    public function getTile (fqTileId :int, rizer :Colorizer = null) :Tile
    {
        return getTileBySet(TileUtil.getTileSetId(fqTileId),
            TileUtil.getTileIndex(fqTileId), rizer);
    }

    /**
     * Returns the {@link Tile} object from the specified tileset at the specified index.
     *
     * @param tileSetId the tileset id.
     * @param tileIndex the index of the tile to be retrieved.
     *
     * @return the tile object.
     */
    public function getTileBySet (tileSetId :int, tileIndex :int, rizer :Colorizer) :Tile
    {
        var set :TileSet = getTileSet(tileSetId);
        return set.getTile(tileIndex, rizer);
    }

    public function ensureLoaded (tileSets :Set, completeCallback :Function,
        progressCallback :Function) :void
    {
        _setrep.ensureLoaded(tileSets, completeCallback, progressCallback);
    }

    /** The tile set repository. */
    protected var _setrep :TileSetRepository;
}
}