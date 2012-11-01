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

import java.util.Map;

import java.io.IOException;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import com.samskivert.util.LRUHashMap;

import com.threerings.media.image.BufferedMirage;
import com.threerings.media.image.Colorization;
import com.threerings.media.image.ImageUtil;
import com.threerings.media.image.Mirage;

import static com.threerings.media.Log.log;

/**
 * An image provider that can be used by command line tools to load images and provide them to
 * tilesets when doing things like preprocessing tileset images.
 */
public abstract class SimpleCachingImageProvider implements ImageProvider
{
    // documentation inherited from interface
    public BufferedImage getTileSetImage (String path, Colorization[] zations)
    {
        BufferedImage image = _cache.get(path);
        if (image == null) {
            try {
                image = loadImage(path);
                _cache.put(path, image);
            } catch (IOException ioe) {
                log.warning("Failed to load image", "path", path, "ioe", ioe);
            }
        }

        if (zations == null || image == null) {
            return image;
        } else {
            return ImageUtil.recolorImage(image, zations);
        }
    }

    // documentation inherited from interface
    public Mirage getTileImage (String path, Rectangle bounds, Colorization[] zations)
    {
        // mostly fake it
        BufferedImage tsimg = getTileSetImage(path, zations);
        tsimg = tsimg.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);
        return new BufferedMirage(tsimg);
    }

    /**
     * Derived classes must implement this method to actually load the raw source images.
     */
    protected abstract BufferedImage loadImage (String path)
        throws IOException;

    protected Map<String, BufferedImage> _cache = new LRUHashMap<String, BufferedImage>(10);
}
