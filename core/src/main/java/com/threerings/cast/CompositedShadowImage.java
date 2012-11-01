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
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import com.threerings.media.image.ImageManager;
import com.threerings.media.image.VolatileMirage;

import com.threerings.cast.CompositedActionFrames.ComponentFrames;

/**
 * Used to composite the special shadow action frames for a particular
 * orientation of a {@link CompositedActionFrames}.
 */
public class CompositedShadowImage extends CompositedMultiFrameImage
{
    public CompositedShadowImage (ImageManager imgr, ComponentFrames[] sources,
                                  String action, int orient, float shadowAlpha)
    {
        super(imgr, sources, action, orient);

        // create the appropriate alpha composite for rendering the shadow
        _shadowAlpha = AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER, shadowAlpha);
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
    protected CompositedMirage createCompositedMirage (int index) {
        // Always use a CompositedVolatileMirage for ShadowImage since we need to draw into it.
        return new CompositedVolatileMirage(index);
    }

    @Override
    public void paintFrame (Graphics2D g, int index, int x, int y) {
        Composite ocomp = g.getComposite();
        g.setComposite(_shadowAlpha);
        _images[index].paint(g, x + getX(index), y + getY(index));
        g.setComposite(ocomp);
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

    /** The alpha value at which we render our shadow. */
    protected AlphaComposite _shadowAlpha;
}
