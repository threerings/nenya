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
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * A simple mirage that's just a solid rectangle of color.
 */
public class SolidMirage
    implements Mirage
{
    public SolidMirage (Color color, int width, int height)
    {
        _color = color;
        _width = width;
        _height = height;
    }

    public long getEstimatedMemoryUsage ()
    {
        return 0;
    }

    public int getHeight ()
    {
        return _height;
    }

    public BufferedImage getSnapshot ()
    {
        BufferedImage snap = new BufferedImage(_width, _height, BufferedImage.TYPE_INT_RGB);
        int rgb = _color.getRGB();

        // This isn't a particularly efficient way to go about this, but really, if you're asking
        // for a snapshot of our solid rectangle of color, it's okay
        for (int x = 0; x < _width; x++) {
            for (int y = 0; y < _height; y++) {
                snap.setRGB(x, y, rgb);
            }
        }

        return snap;
    }

    public int getWidth ()
    {
        return _width;
    }

    public boolean hitTest (int x, int y)
    {
        return x >= 0 && x < _width && y >= 0 && y < _height;
    }

    public void paint (Graphics2D gfx, int x, int y)
    {
        Color ocolor = gfx.getColor();
        gfx.setColor(_color);
        gfx.fillRect(x, y, _width, _height);
        gfx.setColor(ocolor);
    }

    protected final Color _color;
    protected final int _width, _height;
}
