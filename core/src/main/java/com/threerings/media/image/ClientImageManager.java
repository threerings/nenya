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

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import com.samskivert.swing.RuntimeAdjust;

import com.threerings.resource.ResourceManager;

import com.threerings.media.MediaPrefs;

/**
 * Provides a single point of access for image retrieval and caching - just like the ImageManager
 * but adds a tie in to the RuntimeAdjust system to control caching and mirage creation.
 */
public class ClientImageManager extends ImageManager
{
    /**
     * Sets the size of the image cache. This must be called before the ImageManager is created.
     */
    public static void setCacheSize (int cacheKilobytes)
    {
        _runCacheSize = cacheKilobytes;
    }

    /**
     * Sets if images should be recreated in the graphics context's preferred format before
     * rendering. This must be called before the ImageManager is created.
     */
    public static void setPrepareImages (boolean prepareImages)
    {
        _runPrepareImages = prepareImages;
    }

    public ClientImageManager (ResourceManager rmgr, OptimalImageCreator icreator)
    {
        super(rmgr, icreator);
    }

    public ClientImageManager (ResourceManager rmgr, Component context)
    {
        super(rmgr, context);
    }

    @Override
    public int getCacheSize ()
    {
        return _runCacheSize;
    }

    @Override
    public Mirage getMirage (ImageKey key, Rectangle bounds, Colorization[] zations)
    {
        // We need to do something more complicated than the BaseImageManager because our
        //  runtime adjustments may affect how we create our mirages.

        BufferedImage src = null;

        float percentageOfDataBuffer = 1;
        if (bounds == null) {
            // if they specified no bounds, we need to load up the raw image and determine its
            // bounds so that we can pass those along to the created mirage
            src = getImage(key, zations);
            bounds = new Rectangle(0, 0, src.getWidth(), src.getHeight());

        } else if (!_runPrepareImages) {
            src = getImage(key, zations);
            percentageOfDataBuffer =
                (bounds.width * bounds.height)/(float)(src.getHeight() * src.getWidth());
            src = src.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        if (_runBlank.getValue()) {
            return new BlankMirage(bounds.width, bounds.height);
        } else if (_runPrepareImages) {
            return new CachedVolatileMirage(this, key, bounds, zations);
        } else {
            return new BufferedMirage(src, percentageOfDataBuffer);
        }
    }

    /** Register our image cache size with the runtime adjustments framework. */
    protected static RuntimeAdjust.IntAdjust _cacheSize = new RuntimeAdjust.IntAdjust(
        "Size (in kb of memory used) of the image manager LRU cache [requires restart]",
        "narya.media.image.cache_size", MediaPrefs.config, DEFAULT_CACHE_SIZE);

    /**
     * Cache size to be used in this run. Adjusted by setCacheSize without affecting the stored
     * value.
     */
    protected static int _runCacheSize = _cacheSize.getValue();

    /** Controls whether or not we prepare images or use raw versions. */
    protected static RuntimeAdjust.BooleanAdjust _prepareImages = new RuntimeAdjust.BooleanAdjust(
        "Cause image manager to optimize all images for display.",
        "narya.media.image.prep_images", MediaPrefs.config, true);

    /**
     * If images should be prepared for the graphics context in this run.
     */
    protected static boolean _runPrepareImages = _prepareImages.getValue();

    /** A debug toggle for running entirely without rendering images. */
    protected static RuntimeAdjust.BooleanAdjust _runBlank = new RuntimeAdjust.BooleanAdjust(
        "Cause image manager to return blank images.",
        "narya.media.image.run_blank", MediaPrefs.config, false);
}
