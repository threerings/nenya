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

import java.awt.Graphics2D;

import com.threerings.media.image.Colorization;
import com.threerings.media.util.MultiFrameImage;

/**
 * A {@link MultiFrameImage} implementation that obtains its image frames
 * from a tileset but that only uses a subset of the tiles available.
 */
public class TileSubsetMultiFrameImage extends TileMultiFrameImage
{
    /**
     * Creates a tile MFI which will obtain its image frames from the
     * specified source tileset.
     */
    public TileSubsetMultiFrameImage (TileSet source, int startIdx, int numTiles)
    {
        super(source);
        if (startIdx + numTiles > source.getTileCount()) {
            throw new IllegalArgumentException("Invalid tile range specified.");
        }
        _tileCount = numTiles;
        _startIdx = startIdx;
    }

    /**
     * Creates a recoolored tile MFI which will obtain its image frames
     * from the specified source tileset.
     */
    public TileSubsetMultiFrameImage (TileSet source, Colorization[] zations,
        int startIdx, int numTiles)
    {
        this(source.clone(zations), startIdx, numTiles);
    }

    @Override
    public int getWidth (int index)
    {
        return super.getWidth(index + _startIdx);
    }

    @Override
    public int getHeight (int index)
    {
        return super.getHeight(index + _startIdx);
    }

    @Override
    public void paintFrame (Graphics2D g, int index, int x, int y)
    {
        super.paintFrame(g, index + _startIdx, x, y);
    }

    @Override
    public boolean hitTest (int index, int x, int y)
    {
        return super.hitTest(index + _startIdx, x, y);
    }

    /** Index of the tile with which we begin this subset. */
    protected int _startIdx;
}
