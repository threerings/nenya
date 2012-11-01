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
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * The NinePatch class permits drawing a bitmap in nine sections. The four corners are unscaled;
 * the four edges are scaled in one axis, and the middle is scaled in both axes. Normally, the
 * middle is transparent so that the patch can provide a selection about a rectangle. Essentially,
 * it allows the creation of custom graphics that will scale the way that you define, when content
 * added within the image exceeds the normal bounds of the graphic.
 *
 * This is an AWT clone of the NinePatch functionality that exists in Google's Android graphic
 * system. See http://developer.android.com/guide/topics/graphics/2d-graphics.html#nine-patch
 */
public class NinePatch
{
    /**
     * Builds a NinePatch based on the given image.
     *
     * Figures out the NinePatch bounds by parsing the image: Opaque pixels in the top row
     * and left column show the stretchable center. Opaque pixels in the bottom row and right
     * column show the content area.
     */
    public NinePatch (BufferedImage img)
    {
        this(img.getSubimage(1, 1, img.getWidth() - 2, img.getHeight() - 2),
            getRectangle(img, true), getRectangle(img, false));
    }

    /**
     * Builds a NinePatch based on the given image.
     *
     * @param center specifies the stretchable center of the image.
     * @param content if non-null, specifies the usable content area of the image
     */
    public NinePatch (BufferedImage img, Rectangle center, Rectangle content)
    {
        _img = img;

        _left = center.x;
        _right = _img.getWidth() - (center.x + center.width);
        _top = center.y;
        _bottom = _img.getHeight() - (center.y + center.height);

        if (content != null) {
            _leftPad = content.x;
            _rightPad = _img.getWidth() - (content.x + content.width);
            _topPad = content.y;
            _bottomPad = _img.getHeight() - (content.y + content.height);
        } else  {
            _leftPad = _rightPad = _topPad = _bottomPad = 0;
        }
    }

    public void paint (Graphics2D gfx, Rectangle location)
    {
        int sx, sy, sw, sh;
        int dx, dy, dw, dh;

        int width = _img.getWidth();
        int height = _img.getHeight();

        // Top left
        sw = _left;
        sh = _top;
        sx = 0;
        sy = 0;

        dw = sw;
        dh = sh;
        dx = location.x;
        dy = location.y;

        gfx.drawImage(_img, dx, dy, dx + dw, dy + dh, sx, sy, sx + sw, sy + sh, null);

        // Top right
        sw = _right;
        sh = _top;
        sx = width - sw;
        sy = 0;

        dw = sw;
        dh = sh;
        dx = location.x + location.width - dw;
        dy = location.y;

        gfx.drawImage(_img, dx, dy, dx + dw, dy + dh, sx, sy, sx + sw, sy + sh, null);

        // Bottom left
        sw = _left;
        sh = _bottom;
        sx = 0;
        sy = height - sh;

        dw = sw;
        dh = sh;
        dx = location.x;
        dy = location.y + location.height - dh;

        gfx.drawImage(_img, dx, dy, dx + dw, dy + dh, sx, sy, sx + sw, sy + sh, null);

        // Bottom right
        sw = _right;
        sh = _bottom;
        sx = width - sw;
        sy = height - sh;

        dw = sw;
        dh = sh;
        dx = location.x + location.width - dw;
        dy = location.y + location.height - dh;

        gfx.drawImage(_img, dx, dy, dx + dw, dy + dh, sx, sy, sx + sw, sy + sh, null);

        // Top center
        sw = width - _left - _right;
        sh = _top;
        sx = _left;
        sy = 0;

        dw = location.width - _left - _right;
        dh = sh;
        dx = location.x + _left;
        dy = location.y;

        gfx.drawImage(_img, dx, dy, dx + dw, dy + dh, sx, sy, sx + sw, sy + sh, null);

        // Bottom center
        sw = width - _left - _right;
        sh = _bottom;
        sx = _left;
        sy = height - _bottom;

        dw = location.width - _left - _right;
        dh = sh;
        dx = location.x + _left;
        dy = location.y + location.height - dh;

        gfx.drawImage(_img, dx, dy, dx + dw, dy + dh, sx, sy, sx + sw, sy + sh, null);

        // Left center
        sw = _left;
        sh = height - _top - _bottom;
        sx = 0;
        sy = _top;

        dw = sw;
        dh = location.height - _top - _bottom;
        dx = location.x;
        dy = location.y + _top;

        gfx.drawImage(_img, dx, dy, dx + dw, dy + dh, sx, sy, sx + sw, sy + sh, null);

        // Right center
        sw = _right;
        sh = height - _top - _bottom;
        sx = width - _right;
        sy = _top;

        dw = sw;
        dh = location.height - _top - _bottom;
        dx = location.x + location.width - _right;
        dy = location.y + _top;

        gfx.drawImage(_img, dx, dy, dx + dw, dy + dh, sx, sy, sx + sw, sy + sh, null);

        // Dead center
        sw = width - _left - _right;
        sh = height - _top - _bottom;
        sx = _left;
        sy = _top;

        dw = location.width - _left - _right;
        dh = location.height - _top - _bottom;
        dx = location.x + _left;
        dy = location.y + _top;

        gfx.drawImage(_img, dx, dy, dx + dw, dy + dh, sx, sy, sx + sw, sy + sh, null);
    }

    /**
     * Returns a rectangle describing the bounds of this NinePatch when drawn such that it frames
     * the given content rectangle.
     */
    public Rectangle getBoundsSurrounding (Rectangle content)
    {
        Rectangle bounds = new Rectangle(content);

        bounds.width += _leftPad + _rightPad;
        bounds.height += _topPad + _bottomPad;
        bounds.x -= _leftPad;
        bounds.y -= _topPad;

        return bounds;
    }

    /**
     * Parses the image to find the bounds of the rectangle defined by pixels on the outside.
     */
    protected static Rectangle getRectangle (BufferedImage img, boolean stretch)
    {
        Rectangle rect = new Rectangle(0, 0, img.getWidth() - 2, img.getHeight() - 2);

        for (int xx = 1; xx < rect.width + 1; xx++) {
            if (ImageUtil.hitTest(img, xx, stretch ? 0 : img.getHeight() - 1)) {
                rect.x = xx - 1;
                rect.width -= rect.x;
                break;
            }
        }

        for (int xx = img.getWidth() - 1; xx >= rect.x + 1; xx--) {
            if (ImageUtil.hitTest(img, xx, stretch ? 0 : img.getHeight() - 1)) {
                rect.width = xx - rect.x;
                break;
            }
        }

        for (int yy = 1; yy < rect.height + 1; yy++) {
            if (ImageUtil.hitTest(img, stretch ? 0 : img.getWidth() - 1, yy)) {
                rect.y = yy - 1;
                rect.height -= rect.y;
                break;
            }
        }

        for (int yy = img.getHeight() - 1; yy >= rect.y + 1; yy--) {
            if (ImageUtil.hitTest(img, stretch ? 0 : img.getWidth() - 1, yy)) {
                rect.height = yy - rect.y;
                break;
            }
        }

        return rect;
    }

    final protected BufferedImage _img;

    /** The size of the non-stretchable regions on each side. */
    final protected int _top;
    final protected int _bottom;
    final protected int _left;
    final protected int _right;

    /** The amount of padding on each side until we're in the content area. */
    final protected int _topPad;
    final protected int _bottomPad;
    final protected int _leftPad;
    final protected int _rightPad;
}
