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

package com.threerings.media.image;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.samskivert.util.LRUHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Tests how the {@link ImageManager} cache accounts for colorized copies of an image.
 */
public class ImageManagerCacheTest
{
    /**
     * A colorized copy shares its source's pixels, so it must cost the cache only its palette.
     */
    @Test
    public void colorizedCopyCostsOnlyItsPalette ()
    {
        ImageManager imgr = new SizedImageManager(BUDGET_KB);
        CountingProvider images = new CountingProvider();
        ImageManager.ImageKey key = imgr.getImageKey(images, "a");

        BufferedImage source = imgr.getImage(key, null);
        for (int ii = 0; ii < COLORS; ii++) {
            BufferedImage copy = imgr.getImage(key, colorize(ii));
            assertSame("a copy shares its source's pixels",
                source.getRaster().getDataBuffer(), copy.getRaster().getDataBuffer());
        }

        ImageManager.CacheRecord record = imgr._ccache.get(key);
        assertEquals("one sheet of pixels plus a palette per color",
            SHEET_BYTES + COLORS * PALETTE_BYTES, record.getEstimatedMemoryUsage());
    }

    /**
     * Colorizing one image many times must not push other images out of a cache that has room
     * for all of them.
     */
    @Test
    public void colorizingDoesNotEvictOtherImages ()
    {
        ImageManager imgr = new SizedImageManager(BUDGET_KB);
        CountingProvider images = new CountingProvider();
        ImageManager.ImageKey a = imgr.getImageKey(images, "a");
        ImageManager.ImageKey b = imgr.getImageKey(images, "b");

        imgr.getImage(b, null);
        imgr.getImage(a, null);
        for (int ii = 0; ii < COLORS; ii++) {
            imgr.getImage(a, colorize(ii));
        }
        imgr.getImage(b, null);

        assertEquals("b is loaded once and then served from the cache", 1, images.loads("b"));
    }

    /**
     * Evicting a record with colorized copies must take back exactly what adding it charged,
     * or the cache's running total drifts with every eviction.
     */
    @Test
    public void evictionKeepsTheTotalInStep ()
        throws Exception
    {
        // room for two images and their copies, so most of these are evicted along the way
        ImageManager imgr = new SizedImageManager(BUDGET_KB);
        CountingProvider images = new CountingProvider();
        for (int round = 0; round < ROUNDS; round++) {
            ImageManager.ImageKey key = imgr.getImageKey(images, "img" + round);
            imgr.getImage(key, null);
            for (int ii = 0; ii < COLORS; ii++) {
                imgr.getImage(key, colorize(ii));
            }
        }

        long held = 0;
        for (ImageManager.CacheRecord record : imgr._ccache.values()) {
            held += record.getEstimatedMemoryUsage();
        }
        assertEquals("the running total is what the cache still holds", held, cacheTotal(imgr));
    }

    /** Reads the LRU cache's running total, which it does not expose. */
    protected static int cacheTotal (ImageManager imgr)
        throws Exception
    {
        Field size = LRUHashMap.class.getDeclaredField("_size");
        size.setAccessible(true);
        return size.getInt(imgr._ccache);
    }

    /** Returns a distinct colorization for each index. */
    protected static Colorization[] colorize (int index)
    {
        return new Colorization[] {
            new Colorization(index + 1, Color.BLUE, new float[] { 1f, 1f, 1f },
                new float[] { 0.1f * index, 0f, 0f })
        };
    }

    /** An image manager with a set cache budget and no resource manager. */
    protected static class SizedImageManager extends ImageManager
    {
        public SizedImageManager (int budgetKb)
        {
            super(null, (OptimalImageCreator)null);
            _ccache.setMaxSize(budgetKb * BYTES_PER_KB);
        }
    }

    /** Hands out a fresh indexed image for any path, counting the loads of each. */
    protected static class CountingProvider implements ImageDataProvider
    {
        public BufferedImage loadImage (String path)
        {
            _loads.merge(path, 1, Integer::sum);
            return new BufferedImage(SIDE, SIDE, BufferedImage.TYPE_BYTE_INDEXED);
        }

        public String getIdent ()
        {
            return "test";
        }

        public int loads (String path)
        {
            return _loads.getOrDefault(path, 0);
        }

        protected Map<String, Integer> _loads = new HashMap<String, Integer>();
    }

    /** The width and height of each test image, whose pixels are one byte each. */
    protected static final int SIDE = 100;
    protected static final int SHEET_BYTES = SIDE * SIDE;

    /** A TYPE_BYTE_INDEXED palette: 256 entries of 4 bytes. */
    protected static final int PALETTE_BYTES = 256 * 4;

    protected static final int COLORS = 10;
    protected static final int ROUNDS = 6;
    protected static final int BYTES_PER_KB = 1024;

    /** Room for two sheets and their copies' palettes, but not for ten full copies. */
    protected static final int BUDGET_KB = 40;
}
