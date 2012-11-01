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

public class CompositeMirage implements Mirage
{
    public CompositeMirage (Mirage... mirages)
    {
        _mirages = mirages;
    }

    // documentation inherited from interface Mirage
    public long getEstimatedMemoryUsage ()
    {
        // Return the total memory of our component mirages.
        long mem = 0;
        for (Mirage m : _mirages) {
            mem += m.getEstimatedMemoryUsage();
        }

        return mem;
    }

    // documentation inherited from interface Mirage
    public int getHeight ()
    {
        // Return the maximal height of our component mirages.
        int height = 0;
        for (Mirage m : _mirages) {
            height = Math.max(height, m.getHeight());
        }

        return height;
    }

    // documentation inherited from interface Mirage
    public int getWidth ()
    {
        // Return the maximal width of our component mirages.
        int width = 0;
        for (Mirage m : _mirages) {
            width = Math.max(width, m.getWidth());
        }

        return width;
    }

    // documentation inheritd from interface Mirage
    public BufferedImage getSnapshot ()
    {
        BufferedImage img = new BufferedImage(getWidth(), getHeight(),
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D gfx = img.createGraphics();

        try {
            for (Mirage m : _mirages) {
                m.paint(gfx, 0, 0);
            }
        } finally {
            gfx.dispose();
        }

        return img;
    }

    // documentation inheritd from interface Mirage
    public boolean hitTest (int x, int y)
    {
        // If it hits any of our mirages, it hits us.
        for (Mirage m : _mirages) {
            if (m.hitTest(x, y)) {
                return true;
            }
        }

        return false;
    }

    // documentation inheritd from interface Mirage
    public void paint (Graphics2D gfx, int x, int y)
    {
        // Paint everyone.
        for (Mirage m : _mirages) {
            m.paint(gfx, x, y);
        }
    }

    /** All the component mirages we're made up of. */
    protected Mirage[] _mirages;
}
