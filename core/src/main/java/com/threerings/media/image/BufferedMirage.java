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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * A simple mirage implementation that uses a buffered image.
 */
public class BufferedMirage implements Mirage
{
    public BufferedMirage (BufferedImage image)
    {
        this(image, 1);
    }

    /**
     * @param percentageOfDataBuffer - the percentage of image's data buffer used by image for use
     * in getEstimatedMemory. ie if this image is a subimage of another image, and they share a
     * data buffer, this is the percentage of the size of this image compared to the source.
     */
    public BufferedMirage (BufferedImage image, float percentageOfDataBuffer)
    {
        _image = image;
        _percentageOfDataBuffer = percentageOfDataBuffer;
    }

    // documentation inherited from interface
    public void paint (Graphics2D gfx, int x, int y)
    {
        gfx.drawImage(_image, x, y, null);
    }

    // documentation inherited from interface
    public int getWidth ()
    {
        return _image.getWidth();
    }

    // documentation inherited from interface
    public int getHeight ()
    {
        return _image.getHeight();
    }

    // documentation inherited from interface
    public boolean hitTest (int x, int y)
    {
        return ImageUtil.hitTest(_image, x, y);
    }

    // documentation inherited from interface
    public long getEstimatedMemoryUsage ()
    {
        return (long)(ImageUtil.getEstimatedMemoryUsage(_image.getRaster()) *
                _percentageOfDataBuffer);
    }

    // documentation inherited from interface
    public BufferedImage getSnapshot ()
    {
        return _image;
    }

    protected float _percentageOfDataBuffer;

    protected BufferedImage _image;
}
