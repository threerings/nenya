package com.threerings.media.tile;

import java.awt.Graphics2D;

import com.threerings.media.image.Colorization;

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
