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

package com.threerings.cast;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import com.threerings.media.image.ImageManager;
import com.threerings.media.image.VolatileMirage;

import com.threerings.cast.CompositedActionFrames.ComponentFrames;

/**
 * Used to composite action frames with mask frames.
 */
public class CompositedMaskedImage extends CompositedMultiFrameImage
{
    public CompositedMaskedImage (
        ImageManager imgr, ComponentFrames[] sources, String action,
        int orient)
    {
        super(imgr, sources, action, orient);
    }

    @Override
    public int getWidth (int index) {
        return _sources[0].frames.getFrames(_orient).getWidth(index);
    }

    @Override
    public int getHeight (int index) {
        return _sources[0].frames.getFrames(_orient).getHeight(index);
    }

    @Override
    public int getXOrigin (int index) {
        return _sources[0].frames.getXOrigin(_orient, index);
    }

    @Override
    public int getYOrigin (int index) {
        return _sources[0].frames.getYOrigin(_orient, index);
    }

    @Override
    public void paintFrame (Graphics2D g, int index, int x, int y) {
        _images[index].paint(g, x + getX(index), y + getY(index));
    }

    @Override
    public boolean hitTest (int index, int x, int y) {
        return _images[index].hitTest(x + getX(index), y + getY(index));
    }

    @Override
    public void getTrimmedBounds (int index, Rectangle bounds) {
        bounds.setBounds(getX(index), getY(index), _images[index].getWidth(),
            _images[index].getHeight());
    }

    @Override
    protected CompositedMirage createCompositedMirage (int index)
    {
        return new MaskedMirage(index);
    }

    /**
     * Combines the image in the first source with the masks in the rest. */
    protected class MaskedMirage extends CompositedVolatileMirage
    {
        public MaskedMirage (int index)
        {
            super(index);
        }

        @Override
        protected Rectangle combineBounds (Rectangle bounds, Rectangle tbounds)
        {
            if (bounds.width == 0 && bounds.height == 0) {
                bounds.setBounds(tbounds);
            } else {
                bounds = bounds.intersection(tbounds);
            }
            return bounds;
        }

        @Override
        protected void refreshVolatileImage ()
        {
            Graphics2D g = (Graphics2D)_image.getGraphics();
            try {
                TrimmedMultiFrameImage source = _sources[0].frames.getFrames(_orient);
                source.paintFrame(g, _index, -_bounds.x, -_bounds.y);
                g.setComposite(AlphaComposite.DstIn);
                for (int ii = 1; ii < _sources.length; ii++) {
                    TrimmedMultiFrameImage mask = _sources[ii].frames.getFrames(_orient);
                    mask.paintFrame(g, _index, -_bounds.x, -_bounds.y);
                }

            } finally {
                // clean up after ourselves
                if (g != null) {
                    g.dispose();
                }
            }
        }
    }

    /**
     * @return the x offset into the source image for the image at index
     */
    protected int getX (int index) {
        return ((VolatileMirage)_images[index]).getX();
    }

    /**
     * @return the y offset into the source image for the image at index
     */
    protected int getY (int index) {
        return ((VolatileMirage)_images[index]).getY();
    }
}
