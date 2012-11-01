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

import java.io.IOException;
import java.io.OutputStream;

import java.awt.Rectangle;

import com.threerings.resource.FastImageIO;

import com.threerings.media.image.Colorization;
import com.threerings.media.tile.util.TileSetTrimmer;

/**
 * Contains the necessary information to create a set of trimmed tiles from a base image and the
 * associated trim metrics.
 */
public class TrimmedTileSet extends TileSet
{
    @Override
    public int getTileCount ()
    {
        return _obounds.length;
    }

    @Override
    public Rectangle computeTileBounds (int tileIndex, Rectangle bounds)
    {
        bounds.setBounds(_obounds[tileIndex]);
        return bounds;
    }

    @Override
    protected Tile createTile ()
    {
        return new TrimmedTile();
    }

    @Override
    protected void initTile (Tile tile, int tileIndex, Colorization[] zations)
    {
        super.initTile(tile, tileIndex, zations);
        ((TrimmedTile)tile).setTrimmedBounds(_tbounds[tileIndex]);
    }

    /**
     * Convenience function to trim the tile set and save it using FastImageIO.
     */
    public static TrimmedTileSet trimTileSet (TileSet source, OutputStream destImage)
        throws IOException
    {
        return trimTileSet(source, destImage, FastImageIO.FILE_SUFFIX);
    }

    /**
     * Creates a trimmed tileset from the supplied source tileset. The image path must be set by
     * hand to the appropriate path based on where the image data that is written to the
     * <code>destImage</code> parameter is actually stored on the file system. The image format
     * indicateds how the resulting image should be saved.  If null, we save using FastImageIO
     * See {@link TileSetTrimmer#trimTileSet} for further information.
     */
    public static TrimmedTileSet trimTileSet (TileSet source, OutputStream destImage,
                                              String imgFormat)
        throws IOException
    {
        final TrimmedTileSet tset = new TrimmedTileSet();
        tset.setName(source.getName());
        int tcount = source.getTileCount();
        tset._tbounds = new Rectangle[tcount];
        tset._obounds = new Rectangle[tcount];

        // grab the dimensions of the original tiles
        for (int ii = 0; ii < tcount; ii++) {
            tset._tbounds[ii] = source.computeTileBounds(ii, new Rectangle());
        }

        // create the trimmed tileset image
        TileSetTrimmer.TrimMetricsReceiver tmr = new TileSetTrimmer.TrimMetricsReceiver() {
            public void trimmedTile (int tileIndex, int imageX, int imageY,
                                     int trimX, int trimY, int trimWidth, int trimHeight) {
                tset._tbounds[tileIndex].x = trimX;
                tset._tbounds[tileIndex].y = trimY;
                tset._obounds[tileIndex] = new Rectangle(imageX, imageY, trimWidth, trimHeight);
            }
        };
        TileSetTrimmer.trimTileSet(source, destImage, tmr, imgFormat);

        return tset;
    }

    /** The width and height of the trimmed tile, and the x and y offset of the trimmed image
     * within our tileset image. */
    protected Rectangle[] _obounds;

    /** The width and height of the untrimmed image and the x and y offset within the untrimmed
     * image at which the trimmed image should be rendered. */
    protected Rectangle[] _tbounds;

    /** Increase this value when object's serialized state is impacted by a class change
     * (modification of fields, inheritance). */
    private static final long serialVersionUID = 1;
}
