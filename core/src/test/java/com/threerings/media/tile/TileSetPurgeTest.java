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

package com.threerings.media.tile;

import java.awt.Rectangle;
import java.lang.ref.SoftReference;

import org.junit.Test;

import com.threerings.media.image.Colorization;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests that the active tile set lets go of the tiles the garbage collector clears.
 */
public class TileSetPurgeTest
{
    @Test
    public void collectedTileLeavesTheActiveSet ()
    {
        TileSet set = new BlankTileSet();
        Tile tile = set.getTile(0, NO_COLORS);
        assertTrue(isActive(tile.key));

        collect(TileSet._atiles.get(tile.key));
        set.getTile(1, NO_COLORS);
        assertFalse("a collected tile's entry is purged", isActive(tile.key));
    }

    @Test
    public void recreatedTileKeepsItsEntry ()
    {
        TileSet set = new BlankTileSet();
        Tile.Key key = set.getTile(0, NO_COLORS).key;
        SoftReference<Tile> stale = TileSet._atiles.get(key);
        stale.clear();

        Tile recreated = set.getTile(0, NO_COLORS);
        SoftReference<Tile> fresh = TileSet._atiles.get(key);
        stale.enqueue();
        set.getTile(1, NO_COLORS);
        assertSame("the recreated tile's entry survives the stale one's purge",
            fresh, TileSet._atiles.get(key));
        assertSame(recreated, fresh.get());
    }

    /** Clears a tile's reference and queues it, as the garbage collector does. */
    protected static void collect (SoftReference<Tile> ref)
    {
        ref.clear();
        ref.enqueue();
    }

    protected static boolean isActive (Tile.Key key)
    {
        synchronized (TileSet._atiles) {
            return TileSet._atiles.containsKey(key);
        }
    }

    /** A tileset of imageless tiles: it has no image provider, so its tiles load nothing. */
    protected static class BlankTileSet extends TileSet
    {
        @Override public int getTileCount ()
        {
            return TILE_COUNT;
        }

        @Override public Rectangle computeTileBounds (int tileIndex, Rectangle bounds)
        {
            bounds.setBounds(0, 0, 1, 1);
            return bounds;
        }
    }

    protected static final Colorization[] NO_COLORS = null;
    protected static final int TILE_COUNT = 2;
}
