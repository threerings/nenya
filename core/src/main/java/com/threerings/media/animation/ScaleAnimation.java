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

package com.threerings.media.animation;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import com.threerings.media.image.Mirage;
import com.threerings.media.util.LinearTimeFunction;
import com.threerings.media.util.TimeFunction;

/**
 * Animates an image changing size about its center point.
 */
public class ScaleAnimation extends Animation
{
    /**
     * Creates a scale animation with the supplied image. If the image's size would ever be 0 or
     * less, it is not drawn.
     *
     * @param image The image to paint.
     *
     * @param center The screen coordinates of the pixel upon which the image's center should
     * always be rendered.
     *
     * @param startScale The amount to scale the image when it is rendered at time 0.
     *
     * @param endScale The amount to scale the image at the final frame of animation.
     *
     * @param duration The time in milliseconds the anim takes to complete.
     */
    public ScaleAnimation (
        Mirage image, Point center, float startScale, float endScale, int duration)
    {
        super(getBounds(Math.max(startScale, endScale), center, image));

        // Save inputted variables
        _image = image;
        _bufferedImage = _image.getSnapshot();
        _center = new Point(center);
        _startScale = startScale;
        _endScale = endScale;

        // Hack the LinearTimeFunction to use fixed point rationals
        //
        // FIXME: This class doesn't seem to be saving me a lot of work, since I have to repackage
        // the outputs into floats anyway. Find some way to make the LinearTimeFunction do more of
        // this work for us, or write a new class that does. Maybe IntLinearTimeFunction and
        // FloatLinearTimeFunction classes would be useful.
        _scaleFunc = new LinearTimeFunction(0, 10000, duration);
    }

    /**
     * Java wants the first call in a constructor to be super() if it exists at all, so we have to
     * trick it with this function.
     *
     * Oh, and this function computes how big the bounding box needs to be to bound the inputted
     * image scaled to the inputted size centered around the inputted center point.
     */
    public static Rectangle getBounds (float scale, Point center, Mirage image)
    {
        Point size = getSize(scale, image);
        Point corner = getCorner(center, size);
        return new Rectangle(corner.x, corner.y, size.x, size.y);
    }

    @Override
    public Rectangle getBounds ()
    {
        return getBounds(_scale, _center, _image);
    }

    /** Computes the width and height to which an image should be scaled. */
    public static Point getSize (float scale, Mirage image)
    {
        int width  = Math.max(0, Math.round(image.getWidth()  * scale));
        int height = Math.max(0, Math.round(image.getHeight() * scale));
        return new Point(width, height);
    }

    /**
     * Computes the upper left corner where the image should be drawn, given the center and
     * dimensions to which the image should be scaled.
     */
    public static Point getCorner (Point center, Point size)
    {
        return new Point(center.x - size.x/2, center.y - size.y/2);
    }

    @Override
    public void tick (long tickStamp)
    {
        // Compute the new scaling value
        float weight = _scaleFunc.getValue(tickStamp) / 10000.0f;
        float scale = ((1.0f - weight) * _startScale) +
                      ((       weight) *   _endScale);

        // Update the animation if the scaling changes
        if (_scale != scale) {
            _scale = scale;
            invalidate();
        }

        // Check if the animation completed
        if (weight >= 1.0f) {
            _finished = true;
        }
    }

    @Override
    public void fastForward (long timeDelta)
    {
        _scaleFunc.fastForward(timeDelta);
    }

    @Override
    public void paint (Graphics2D gfx)
    {
        // Compute the bounding box to render this image
        Rectangle bounds = getBounds();

        // Paint nothing if the image was scaled to nothing
        if (bounds.width <= 0 || bounds.height <= 0) {
            return;
        }

        // Smooth out the image scaling
        //
        // FIXME: Should this be turned off when the painting is done?
        gfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                             RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Paint the image scaled to this location
        gfx.drawImage(_bufferedImage,
                      bounds.x,
                      bounds.y,
                      bounds.x + bounds.width,
                      bounds.y + bounds.height,
                      0,
                      0,
                      _bufferedImage.getWidth(),
                      _bufferedImage.getHeight(),
                      null);
    }

    /** The image to scale. */
    protected Mirage _image;

    /** The image converted to a format Graphics2D likes, and cached. */
    protected BufferedImage _bufferedImage;

    /** The center pixel to render the image around. */
    protected Point _center;

    /** The amount to scale the image at the start of the animation. */
    protected float _startScale;

    /** The amount to scale the image at the end of the animation. */
    protected float _endScale;

    /** The current amount of scaling to render. */
    protected float _scale;

    /** Computes the image scaling to use at the specified time. */
    protected TimeFunction _scaleFunc;
}
