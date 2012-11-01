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

package com.threerings.chat;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;

import javax.swing.Icon;

import com.samskivert.swing.Label;
import com.samskivert.swing.util.SwingUtil;

import com.threerings.media.MediaPanel;
import com.threerings.media.animation.Animation;
import com.threerings.media.image.ColorUtil;

import static com.threerings.NenyaLog.log;

/**
 * Responsible for rendering a "unit" of chat on a {@link MediaPanel}.
 */
public class ChatGlyph extends Animation
{
    /** Can be used by the Overlay to mark our position in the history. */
    public int histIndex;

    /**
     * Construct a chat glyph.
     *
     * @param owner the subtitle overlay that ownz us.
     * @param bounds the bounds of the glyph
     * @param shape the shape to draw/outline.
     * @param icon the Icon to draw inside the bubble, or null for none
     * @param iconpos the virtual coordinate at which to draw the icon (can be null if no icon)
     * @param label the Label to draw inside the bubble.
     * @param labelpos the virtual coordinate at which to draw the label
     */
    public ChatGlyph (SubtitleChatOverlay owner, int type, long lifetime, Rectangle bounds,
        Shape shape, Icon icon, Point iconpos, Label label, Point labelpos, Color outline)
    {
        super(bounds);
        jiggleBounds();

        _owner = owner;
        _shape = shape;
        _type = type;
        _icon = icon;
        _ipos = iconpos;
        _label = label;
        _lpos = labelpos;
        _lifetime = lifetime;
        _outline = outline;

        if (Color.BLACK.equals(_outline)) {
            _background = Color.WHITE;
        } else {
            _background = ColorUtil.blend(Color.WHITE, _outline, .8f);
        }
    }

    /**
     * Sets whether or not this glyph is in dimmed mode.
     */
    public void setDim (boolean dim)
    {
        if (_dim != dim) {
            _dim = dim;
            invalidate();
        }
    }

    /**
     * Render the chat glyph with no thought to dirty rectangles or
     * restoring composites.
     */
    public void render (Graphics2D gfx)
    {
        Object oalias = SwingUtil.activateAntiAliasing(gfx);
        gfx.setColor(getBackground());
        gfx.fill(_shape);

        gfx.setColor(_outline);
        gfx.draw(_shape);
        SwingUtil.restoreAntiAliasing(gfx, oalias);

        if (_icon != null) {
            _icon.paintIcon(_owner.getTarget(), gfx, _ipos.x, _ipos.y);
        }

        gfx.setColor(Color.BLACK);
        _label.render(gfx, _lpos.x, _lpos.y);
    }

    /**
     * Get the internal chat type of this bubble.
     */
    public int getType ()
    {
        return _type;
    }

    /**
     * Returns the shape of this chat bubble.
     */
    public Shape getShape ()
    {
        return _shape;
    }

    @Override
    public void setLocation (int x, int y)
    {
        // we'll need these so that we can translate our complex shapes
        int dx = x - _bounds.x, dy = y - _bounds.y;
        super.setLocation(x, y);

        if (dx != 0 || dy != 0) {
            // update our icon position, if any
            if (_ipos != null) {
                _ipos.translate(dx, dy);
            }

            // update our label position
            _lpos.translate(dx, dy);

            if (_shape instanceof Area) {
                ((Area) _shape).transform(
                    AffineTransform.getTranslateInstance(dx, dy));

            } else if (_shape instanceof Polygon) {
                ((Polygon) _shape).translate(dx, dy);

            } else {
                log.warning("Unable to translate shape", "glyph", this, "shape", _shape + "]!");
            }
        }
    }

    /**
     * Called when the view has scrolled. The default implementation adjusts the glyph to remain
     * in the same position relative to the view rather than allowing it to scroll with the view.
     */
    public void viewDidScroll (int dx, int dy)
    {
        translate(dx, dy);
    }

    /**
     * Attempt to translate this glyph.
     */
    public void translate (int dx, int dy)
    {
        setLocation(_bounds.x + dx, _bounds.y + dy);
    }

    @Override
    public void tick (long tickStamp)
    {
        // set up our born stamp if we've got one
        if (_bornStamp == 0L) {
            _bornStamp = tickStamp;
            invalidate(); // make sure we're painted the first time
        }

        // if we're not yet ready to die, do nothing
        long deathTime = _bornStamp + _lifetime;
        if (tickStamp < deathTime) {
            /* TEMPORARILY disabled blinking until we sort it out
            if (_type == SubtitleChatOverlay.ATTENTION) {
                float alphaWas = _alpha;
                long val = tickStamp - _bornStamp;
                if ((val < 3000) && (val % 1000 < 500)) {
                    _alpha = 0f;
                } else {
                    _alpha = ALPHA;
                }
                if (_alpha != alphaWas) {
                    invalidate();
                }
            }
            */
            return;
        }

        long msecs = tickStamp - deathTime;
        if (msecs >= ANIM_TIME) {
            _alpha = 0f;
            // stick a fork in ourselves
            _finished = true;
            _owner.glyphExpired(this);

        } else {
            _alpha = ALPHA * (ANIM_TIME - msecs) / ANIM_TIME;
        }

        invalidate();
    }

    @Override
    public void fastForward (long timeDelta)
    {
        if (_bornStamp > 0L) {
            _bornStamp += timeDelta;
        }
    }

    @Override
    public void paint (Graphics2D gfx)
    {
        float alpha = _dim ? _alpha / 3 : _alpha;
        if (alpha == 0f) {
            return;
        }
        if (alpha != 1f) {
            Composite ocomp = gfx.getComposite();
            gfx.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            render(gfx);
            gfx.setComposite(ocomp);
        } else {
            render(gfx);
        }
    }

    protected Color getBackground ()
    {
        return _background;
    }

    /**
     * The damn repaint manager expects 1 more pixel than the shape gives, so we manually resize.
     */
    protected void jiggleBounds ()
    {
        _bounds.setSize(_bounds.width + 1, _bounds.height + 1);
    }

    /** The subtitle chat overlay that ownz us. */
    protected SubtitleChatOverlay _owner;

    /** The type of chat represented by this glyph. */
    protected int _type;

    /** The shape of this glyph. */
    protected Shape _shape;

    /** The visual icon, or null for none. */
    protected Icon _icon;

    /** The position at which we render our icon. */
    protected Point _ipos;

    /** The textual label. */
    protected Label _label;

    /** The position at which we render our text. */
    protected Point _lpos;

    /** The alpha level that we'll render at when fading out. */
    protected float _alpha = ALPHA;

    /** Whether we're in dimmed mode. */
    protected boolean _dim = false;

    /** The length of the fade animation. */
    protected static final long ANIM_TIME = 800L;

    /** The number of milliseconds to wait before this bubble will expire
     * and should begin to fade out. */
    protected long _lifetime;

    /** The time at which we came into being on the screen. */
    protected long _bornStamp;

    /** Our outline color. */
    protected Color _outline;

    /** Our background color. */
    protected Color _background;

    /** The initial alpha of all chat glyphs. */
    protected static final float ALPHA = .9f;
}
