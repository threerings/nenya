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

package com.threerings.media.tile.util;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RasterFormatException;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.threerings.resource.FastImageIO;

import com.threerings.media.image.ImageUtil;
import com.threerings.media.tile.TileSet;

/**
 * Contains routines for trimming the images from an existing tileset
 * which means that each tile is converted to an image that contains the
 * smallest rectangular region of the original image that contains all
 * non-transparent pixels. These trimmed images are then written out to a
 * single image, packed together.
 */
public class TileSetTrimmer
{
    /**
     * Used to communicate the metrics of the trimmed tiles back to the
     * caller so that they can do what they like with them.
     */
    public static interface TrimMetricsReceiver
    {
        /**
         * Called for each trimmed tile.
         *
         * @param tileIndex the index of the tile in the original tileset.
         * @param imageX the x offset into the newly created tileset image
         * of the trimmed image data.
         * @param imageY the y offset into the newly created tileset image
         * of the trimmed image data.
         * @param trimX the x offset into the untrimmed tile image where
         * the trimmed data begins.
         * @param trimY the y offset into the untrimmed tile image where
         * the trimmed data begins.
         * @param trimWidth the width of the trimmed tile image.
         * @param trimHeight the height of the trimmed tile image.
         */
        public void trimmedTile (int tileIndex, int imageX, int imageY,
                                 int trimX, int trimY,
                                 int trimWidth, int trimHeight);
    }

    /**
     * Used to pack the trimmed tiles into the final image.
     */
    public static interface Packer
    {
        /** Add trimmed bounds for a tile. */
        void addTile (int tileIndex, int width, int height);

        /** Do the deed and return dimensions of the packaged layout. */
        Dimension pack ();

        /** Get the packed bounds of the given tile. */
        Rectangle getPosition (int tileIndex);
    }

    /**
     * Convenience function to trim the tile set using FastImageIO to save the result.
     */
    public static void trimTileSet (
        TileSet source, OutputStream destImage, TrimMetricsReceiver tmr, Packer packer)
        throws IOException
    {
        trimTileSet(source, destImage, tmr, FastImageIO.FILE_SUFFIX, packer);
    }

    /**
     * Generates a trimmed tileset image from the supplied source
     * tileset. The source tileset must be configured with an image
     * provider so that the tile images can be obtained. The tile images
     * will be trimmed and a new tileset image generated and written to
     * the <code>destImage</code> output stream argument.
     *
     * @param source the source tileset.
     * @param destImage an output stream to which the new trimmed image
     * will be written.
     * @param tmr a callback object that will be used to inform the caller
     * of the trimmed tile metrics.
     * @param imgFormat the format in which to write the image file - or if null, use FastImageIO.
     */
    public static void trimTileSet (
        TileSet source, OutputStream destImage, TrimMetricsReceiver tmr, String imgFormat,
        Packer packer)
        throws IOException
    {
        int tcount = source.getTileCount();
        BufferedImage[] timgs = new BufferedImage[tcount];

        // these will contain the bounds of the trimmed image in the
        // coordinate system defined by the untrimmed image
        Rectangle[] tbounds = new Rectangle[tcount];

        // compute some tile metrics
        for (int ii = 0; ii < tcount; ii++) {
            // extract the image from the original tileset
            try {
                timgs[ii] = source.getRawTileImage(ii);
            } catch (RasterFormatException rfe) {
                throw new IOException("Failed to get tile image " +
                    "[tidx=" + ii + ", tset=" + source + ", rfe=" + rfe + "].");
            }

            // figure out how tightly we can trim it
            tbounds[ii] = new Rectangle();
            ImageUtil.computeTrimmedBounds(timgs[ii], tbounds[ii]);

            packer.addTile(ii, tbounds[ii].width, tbounds[ii].height);
        }

        Dimension bounds = packer.pack();

        for (int ii = 0; ii < tcount; ii++) {
            // let our caller know what we did
            Rectangle rect = packer.getPosition(ii);
            tmr.trimmedTile(ii, rect.x, rect.y, tbounds[ii].x, tbounds[ii].y,
                            tbounds[ii].width, tbounds[ii].height);
        }

        // create the new tileset image
        BufferedImage image = null;
        try {
            image = ImageUtil.createCompatibleImage(source.getRawTileSetImage(),
                bounds.width, bounds.height);

            // Empty it out
            Graphics2D graphics = (Graphics2D)image.getGraphics();
            graphics.setComposite(AlphaComposite.Clear);
            graphics.fillRect(0, 0, bounds.width, bounds.height);

        } catch (RasterFormatException rfe) {
            throw new IOException("Failed to create trimmed tileset image " +
                "[bounds=" + bounds + ", tset=" + source + ", rfe=" + rfe + "].");
        }

        // copy the tile data
        WritableRaster drast = image.getRaster();
        for (int ii = 0; ii < tcount; ii++) {
            Rectangle pos = packer.getPosition(ii);
            Rectangle tb = tbounds[ii];
            Raster srast = timgs[ii].getRaster().createChild(
                tb.x, tb.y, tb.width, tb.height, 0, 0, null);
            drast.setRect(pos.x, pos.y, srast);
        }

        if (destImage != null) {
            // write out trimmed image
            if (imgFormat == null || FastImageIO.FILE_SUFFIX.equals(imgFormat)) {
                FastImageIO.write(image, destImage);
            } else {
                ImageIO.write(image, imgFormat, destImage);
            }
        }
    }

    /** Includes some boilerplate to handle bits of the packing process. */
    public static abstract class BasePacker implements Packer
    {
        public void addTile (int tileIndex, int width, int height) {
            if (_packed) {
                throw new IllegalStateException("Cannot add tile after packing.");
            }
            _tiles.put(tileIndex, new Rectangle(0, 0, width, height));
        }

        final public Dimension pack () {
            if (_packed) {
                throw new IllegalStateException("Cannot pack repeatedly.");
            }
            if (_tiles.isEmpty()) {
                throw new IllegalStateException("Cannot pack empty tileset");
            }

            _packed = true;
            return doPack();
        }

        protected abstract Dimension doPack ();

        public Rectangle getPosition (int tileIndex) {
            if (!_packed) {
                throw new IllegalStateException("Cannot get tile position before packing.");
            }
            return _tiles.get(tileIndex);
        }

        protected Map<Integer, Rectangle> _tiles = Maps.newHashMap();
        protected boolean _packed;
    };

    /**
     * Packs the tiles together in a wide strip in tile order.
     */
    public static class StripPacker extends RowPacker
    {
        public StripPacker () {
            super(Integer.MAX_VALUE, false);
        }
    };

    /**
     * Packs into rows of the given maximum width, optionally shuffling tiles around to help
     * things pack a little less wastefully if the tiles vary in height.
     */
    public static class RowPacker extends BasePacker
    {
        public final int maxWidth;
        public final boolean sort;

        public RowPacker (int maxWidth, boolean sort) {
            this.maxWidth = maxWidth;
            this.sort = sort;
        }

        @Override protected Dimension doPack () {
            Dimension dim = new Dimension(0, 0);
            int x = 0;
            int y = 0;

            Collection<Rectangle> rects = _tiles.values();
            if (sort) {
                rects = Lists.newArrayList(_tiles.values());
                Collections.sort((List<Rectangle>)rects, new Comparator<Rectangle>() {
                    @Override public int compare (Rectangle a, Rectangle b) {
                        return ComparisonChain.start().
                            compare(b.height, a.height).
                            compare(b.width, a.width).
                            result();
                    }
                });
            }

            for (Rectangle rect : rects) {
                if (x + rect.width > maxWidth) {
                    x = 0;
                    y = dim.height;
                }

                rect.x = x;
                x += rect.width;
                rect.y = y;

                dim.height = Math.max(dim.height, y + rect.height);
                dim.width = Math.max(dim.width, x);
            }

            return dim;
        }
    }
}
