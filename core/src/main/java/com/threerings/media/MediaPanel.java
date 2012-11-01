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

package com.threerings.media;

import java.util.ArrayList;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.MouseInputAdapter;

import com.google.common.collect.Lists;

import com.samskivert.swing.Controller;
import com.samskivert.swing.event.AncestorAdapter;
import com.samskivert.swing.event.CommandEvent;

import com.threerings.media.animation.Animation;
import com.threerings.media.animation.AnimationManager;
import com.threerings.media.sprite.Sprite;
import com.threerings.media.sprite.SpriteManager;
import com.threerings.media.sprite.action.ActionSprite;
import com.threerings.media.sprite.action.ArmingSprite;
import com.threerings.media.sprite.action.CommandSprite;
import com.threerings.media.sprite.action.DisableableSprite;
import com.threerings.media.sprite.action.HoverSprite;
import com.threerings.media.timer.MediaTimer;

import static com.threerings.media.Log.log;

/**
 * Provides a useful extensible framework for rendering animated displays that use sprites and
 * animations. Sprites and animations can be added to this panel and they will automatically be
 * ticked and rendered (see {@link #addSprite} and {@link #addAnimation}).
 *
 * <p> To facilitate optimized sprite and animation rendering, the panel provides a dirty region
 * manager which is used to only repaint dirtied regions on each frame. Derived classes can add
 * dirty regions to the scene and/or augment the dirty regions created by moving sprites and
 * changing animations.
 *
 * <p> Sprite and animation rendering is done in two layers: front and back. Callbacks are provided
 * to render behind the back layer ({@link #paintBehind}), in between front and back ({@link
 * #paintBetween}) and in front of the front layer ({@link #paintInFront}).
 *
 * <p> The animated panel automatically registers with the {@link FrameManager} to participate in
 * the frame tick. It only does so while it is a visible part of the UI hierarchy, so animations
 * and sprites are paused while the animated panel is hidden.
 */
public class MediaPanel extends JComponent
    implements FrameParticipant, MediaConstants, MediaHost
{
    public interface Obscurer
    {
        /**
         * Returns the region obscured by the obscurer, in screen coords.
         */
        public Rectangle getObscured (boolean changedOnly);
    }

    /**
     * Constructs a media panel.
     */
    public MediaPanel (FrameManager framemgr)
    {
        setOpaque(true); // our repaints shouldn't cause other jcomponents to

        // create our meta manager
        _metamgr = new MetaMediaManager(framemgr, this);
        _animmgr = _metamgr.getAnimationManager();
        _spritemgr = _metamgr.getSpriteManager();
        _remgr = _metamgr.getRegionManager();

        // participate in the frame when we're visible
        addAncestorListener(new AncestorAdapter() {
            @Override
            public void ancestorAdded (AncestorEvent event) {
                _metamgr.getFrameManager().registerFrameParticipant(MediaPanel.this);
            }
            @Override
            public void ancestorRemoved (AncestorEvent event) {
                _metamgr.getFrameManager().removeFrameParticipant(MediaPanel.this);
            }
        });
    }

    /**
     * Get the bounds of the viewport, in media coordinates. For the base MediaPanel, this will
     * always be (0, 0, width, height).
     */
    public Rectangle getViewBounds ()
    {
        return new Rectangle(getWidth(), getHeight());
    }

    /**
     * Returns a reference to the animation manager used by this media panel.
     */
    public AnimationManager getAnimationManager ()
    {
        return _metamgr.getAnimationManager();
    }

    /**
     * Returns a reference to the sprite manager used by this media panel.
     */
    public SpriteManager getSpriteManager ()
    {
        return _metamgr.getSpriteManager();
    }

    /**
     * Returns a reference to the region manager used by this media panel.
     */
    public RegionManager getRegionManager ()
    {
        return _metamgr.getRegionManager();
    }

    /**
     * Pauses the sprites and animations that are currently active on this media panel. Also stops
     * listening to the frame tick while paused.
     */
    public void setPaused (boolean paused)
    {
        _metamgr.setPaused(paused);
    }

    /**
     * Returns a timestamp from the {@link MediaTimer} used to track time intervals for this media
     * panel. <em>Note:</em> this should only be called from the AWT thread.
     */
    public long getTimeStamp ()
    {
        return _metamgr.getTimeStamp();
    }

    /**
     * Adds a sprite to this panel.
     */
    public void addSprite (Sprite sprite)
    {
        _metamgr.addSprite(sprite);

        if (((sprite instanceof ActionSprite) ||
             (sprite instanceof HoverSprite)) && (_actionSpriteCount++ == 0)) {
            if (_actionHandler == null) {
                _actionHandler = createActionSpriteHandler();
            }
            addMouseListener(_actionHandler);
            addMouseMotionListener(_actionHandler);
        }
    }

    /**
     * @return true if the sprite is already added to this panel.
     */
    public boolean isManaged (Sprite sprite)
    {
        return _metamgr.isManaged(sprite);
    }

    /**
     * Removes a sprite from this panel.
     */
    public void removeSprite (Sprite sprite)
    {
        _metamgr.removeSprite(sprite);

        if (((sprite instanceof ActionSprite) ||
             (sprite instanceof HoverSprite)) && (--_actionSpriteCount == 0)) {
            removeMouseListener(_actionHandler);
            removeMouseMotionListener(_actionHandler);
        }
    }

    /**
     * Removes all sprites from this panel.
     */
    public void clearSprites ()
    {
        _metamgr.clearSprites();

        if (_actionHandler != null) {
            removeMouseListener(_actionHandler);
            removeMouseMotionListener(_actionHandler);
            _actionSpriteCount = 0;
        }
    }

    /**
     * Adds an animation to this panel. Animations are automatically removed when they finish.
     */
    public void addAnimation (Animation anim)
    {
        _metamgr.addAnimation(anim);
    }

    /**
     * @return true if the animation is already added to this panel.
     */
    public boolean isManaged (Animation anim)
    {
        return _metamgr.isManaged(anim);
    }

    /**
     * Aborts a currently running animation and removes it from this panel. Animations are
     * normally automatically removed when they finish.
     */
    public void abortAnimation (Animation anim)
    {
        _metamgr.abortAnimation(anim);
    }

    /**
     * Removes all animations from this panel.
     */
    public void clearAnimations ()
    {
        _metamgr.clearAnimations();
    }

    // from interface MediaHost
    public Graphics2D createGraphics ()
    {
        return (Graphics2D)getGraphics();
    }

    // from interface FrameParticipant
    public void tick (long tickStamp)
    {
        if (_metamgr.isPaused()) {
            return;
        }

        // let derived classes do their business
        willTick(tickStamp);

        // tick our meta manager which will tick our sprites and animations
        _metamgr.tick(tickStamp);

        // let derived classes do their business
        didTick(tickStamp);

        // make a note that the next paint will correspond to a call to tick()
        _tickPaintPending = true;
    }

    // from interface FrameParticipant
    public boolean needsPaint ()
    {
        boolean needsPaint = _metamgr.needsPaint();

        // if we have no dirty regions, clear our pending tick indicator because we're not going to
        // get painted
        if (!needsPaint) {
            _tickPaintPending = false;
        }

        return needsPaint;
    }

    // from interface FrameParticipant
    public Component getComponent ()
    {
        return this;
    }

    @Override
    public void setOpaque (boolean opaque)
    {
        if (!opaque) {
            log.warning("Media panels shouldn't be setOpaque(false).", new Exception());
        }
        super.setOpaque(true);
    }

    @Override
    public void repaint (long tm, int x, int y, int width, int height)
    {
        if (width > 0 && height > 0) {
            dirtyScreenRect(new Rectangle(x, y, width, height));
        }
    }

    @Override
    public void paint (Graphics g)
    {
        Graphics2D gfx = (Graphics2D)g;

        // no use in painting if we're not showing or if we've not yet been validated
        if (!isValid() || !isShowing()) {
            return;
        }

        // if this isn't a tick paint, then we need to mark the clipping rectangle as dirty
        if (!_tickPaintPending) {
            Shape clip = g.getClip();
            if (clip == null) {
                // mark the whole component as dirty
                repaint();
            } else {
                dirtyScreenRect(clip.getBounds());
            }

            // we used to bail out here and not render until our next tick, but it turns out that
            // we need to render here because Swing may have repainted our parent over us and
            // expect that we're going to paint ourselves on top of whatever it just painted, so we
            // go ahead and paint now to avoid flashing

        } else {
            _tickPaintPending = false;
        }

        addObscurerDirtyRegions(true);

        // if we have no invalid rects, there's no need to repaint
        if (!_metamgr.getRegionManager().haveDirtyRegions()) {
            return;
        }

        // get our dirty rectangles and delegate the main painting to a method that can be more
        // easily overridden
        Rectangle[] dirty = _metamgr.getRegionManager().getDirtyRegions();

        _metamgr.noteDirty(dirty.length);
        try {
            paint(gfx, dirty);
        } catch (Throwable t) {
            log.warning(this + " choked in paint(" + dirty + ").", t);
        }

        // render our performance debugging if it's enabled
        _metamgr.paintPerf(gfx);
    }

    /**
     * Adds an element that could be obscuring the panel and thus requires extra redrawing.
     */
    public void addObscurer (Obscurer obscurer) {
        if (_obscurerList == null) {
            _obscurerList = Lists.newArrayList();
        }
        _obscurerList.add(obscurer);
    }

    /**
     * Removes an obscuring element.
     */
    public void removeObscurer (Obscurer obscurer) {
        if (_obscurerList != null) {
            _obscurerList.remove(obscurer);
        }
    }

    /**
     * Add dirty regions for all our obscurers.
     */
    protected void addObscurerDirtyRegions (boolean changedOnly)
    {
        if (_obscurerList != null) {
            for (Obscurer obscurer : _obscurerList) {
                Rectangle obscured = obscurer.getObscured(changedOnly);
                if (obscured != null) {
                    Point pt = new Point(obscured.x, obscured.y);
                    SwingUtilities.convertPointFromScreen(pt, this);
                    addObscurerDirtyRegion(
                        new Rectangle(pt.x, pt.y, obscured.width, obscured.height));
                }
            }
        }
    }

    /**
     * Adds the particular region as dirty.
     */
    protected void addObscurerDirtyRegion (Rectangle region)
    {
        dirtyScreenRect(region);
    }

    /**
     * Derived classes can override this method and perform computation prior to the ticking of the
     * sprite and animation managers.
     */
    protected void willTick (long tickStamp)
    {
    }

    /**
     * Derived classes can override this method and perform computation subsequent to the ticking
     * of the sprite and animation managers.
     */
    protected void didTick (long tickStamp)
    {
    }

    /**
     * Performs the actual painting of the media panel. Derived methods can override this method if
     * they wish to perform pre- and/or post-paint activities or if they wish to provide their own
     * painting mechanism entirely.
     */
    protected void paint (Graphics2D gfx, Rectangle[] dirty)
    {
        int dcount = dirty.length;

        for (int ii = 0; ii < dcount; ii++) {
            Rectangle clip = dirty[ii];
            // sanity-check the dirty rectangle
            if (clip == null) {
                log.warning("Found null dirty rect painting media panel?!", new Exception());
                continue;
            }

            // constrain this dirty region to the bounds of the component
            constrainToBounds(clip);

            // ignore rectangles that were reduced to nothingness
            if (clip.width == 0 || clip.height == 0) {
                continue;
            }

            // clip to this dirty region
            clipToDirtyRegion(gfx, clip);

            // paint the region
            paintDirtyRect(gfx, clip);
        }
    }

    /**
     * Paints all the layers of the specified dirty region.
     */
    protected void paintDirtyRect (Graphics2D gfx, Rectangle rect)
    {
        // paint the behind the scenes stuff
        paintBehind(gfx, rect);

        // paint back sprites and animations
        paintBits(gfx, AnimationManager.BACK, rect);

        // paint the between the scenes stuff
        paintBetween(gfx, rect);

        // paint front sprites and animations
        paintBits(gfx, AnimationManager.FRONT, rect);

        // paint anything in front
        paintInFront(gfx, rect);
    }

    /**
     * Called by the main rendering code to constrain this dirty rectangle to the bounds of the
     * media panel. If a derived class is using dirty rectangles that live in some sort of virtual
     * coordinate system, they'll want to override this method and constraint the rectangles
     * properly.
     */
    protected void constrainToBounds (Rectangle dirty)
    {
        SwingUtilities.computeIntersection(0, 0, getWidth(), getHeight(), dirty);
    }

    /**
     * This is called to clip the rendering output to the supplied dirty region. This should use
     * {@link Graphics#setClip} because the clipping region will need to be replaced as we iterate
     * through our dirty regions. By default, a region is assumed to represent screen coordinates,
     * but if a derived class wishes to maintain dirty regions in non-screen coordinates, it can
     * override this method to properly clip to the dirty region.
     */
    protected void clipToDirtyRegion (Graphics2D gfx, Rectangle dirty)
    {
//         Log.info("MP: Clipping to [clip=" + StringUtil.toString(dirty) + "].");
        gfx.setClip(dirty);
    }

    /**
     * Called to mark the specified rectangle (in screen coordinates) as dirty. The rectangle will
     * be bent, folded and mutilated, so be sure you're not passing a rectangle into this method
     * that is being used elsewhere.
     *
     * <p> If derived classes wish to convert from screen coordinates to some virtual coordinate
     * system to be used by their repaint manager, this is the place to do it.
     */
    protected void dirtyScreenRect (Rectangle rect)
    {
        _metamgr.getRegionManager().addDirtyRegion(rect);
    }

    /**
     * Paints behind all sprites and animations. The supplied invalid rectangle should be redrawn
     * in the supplied graphics context.  Sub-classes should override this method to do the actual
     * rendering for their display.
     */
    protected void paintBehind (Graphics2D gfx, Rectangle dirtyRect)
    {
    }

    /**
     * Paints between the front and back layer of sprites and animations.  The supplied invalid
     * rectangle should be redrawn in the supplied graphics context. Sub-classes should override
     * this method to do the actual rendering for their display.
     */
    protected void paintBetween (Graphics2D gfx, Rectangle dirtyRect)
    {
    }

    /**
     * Paints in front of all sprites and animations. The supplied invalid rectangle should be
     * redrawn in the supplied graphics context.  Sub-classes should override this method to do the
     * actual rendering for their display.
     */
    protected void paintInFront (Graphics2D gfx, Rectangle dirtyRect)
    {
    }

    /**
     * Renders the sprites and animations that intersect the supplied dirty region in the specified
     * layer. Derived classes can override this method if they need to do custom sprite or
     * animation rendering (if they need to do special sprite z-order handling, for example).  The
     * clipping region will already be set appropriately.
     */
    protected void paintBits (Graphics2D gfx, int layer, Rectangle dirty)
    {
        _metamgr.paintMedia(gfx, layer, dirty);
    }

    /**
     * Creates the mouse listener that will handle action sprites and their variants.
     */
    protected ActionSpriteHandler createActionSpriteHandler ()
    {
        return new ActionSpriteHandler();
    }

    /** Handles ActionSprite/HoverSprite/ArmingSprite manipulation. */
    protected class ActionSpriteHandler extends MouseInputAdapter
    {
        @Override
        public void mousePressed (MouseEvent me) {
            if (_activeSprite == null) {
                // see if we can find one
                Sprite s = getHit(me);
                if (s instanceof ActionSprite) {
                    _activeSprite = s;
                }
            }

            if (_activeSprite instanceof ArmingSprite) {
                ((ArmingSprite) _activeSprite).setArmed(true);
            }
        }

        @Override
        public void mouseReleased (MouseEvent me) {
            if (_activeSprite instanceof ArmingSprite) {
                ((ArmingSprite)_activeSprite).setArmed(false);
            }

            if ((_activeSprite instanceof ActionSprite) &&
                    _activeSprite.hitTest(me.getX(), me.getY())) {
                ActionEvent event;
                if (_activeSprite instanceof CommandSprite) {
                    CommandSprite cs = (CommandSprite) _activeSprite;
                    event = new CommandEvent(
                        MediaPanel.this, cs.getActionCommand(),
                        cs.getCommandArgument(), me.getWhen(),
                        me.getModifiers());

                } else {
                    ActionSprite as = (ActionSprite) _activeSprite;
                    event = new ActionEvent(
                        MediaPanel.this, ActionEvent.ACTION_PERFORMED,
                        as.getActionCommand(), me.getWhen(), me.getModifiers());
                }
                Controller.postAction(event);
            }

            if (!(_activeSprite instanceof HoverSprite)) {
                _activeSprite = null;
            }

            mouseMoved(me);
        }

        @Override
        public void mouseDragged (MouseEvent me) {
            if (_activeSprite instanceof ArmingSprite) {
                ((ArmingSprite) _activeSprite).setArmed(
                    _activeSprite.hitTest(me.getX(), me.getY()));
            }
        }

        @Override
        public void mouseMoved (MouseEvent me) {
            Sprite s = getHit(me);
            if (_activeSprite == s) {
                return;
            }
            if (_activeSprite instanceof HoverSprite) {
                ((HoverSprite) _activeSprite).setHovered(false);
            }
            _activeSprite = s;
            if (_activeSprite instanceof HoverSprite) {
                ((HoverSprite) _activeSprite).setHovered(true);
            }
        }

        /**
         * Utility method, get the highest non-disabled action/hover sprite.
         */
        protected Sprite getHit (MouseEvent me) {
            ArrayList<Sprite> list = Lists.newArrayList();
            getSpriteManager().getHitSprites(list, me.getX(), me.getY());
            for (int ii = 0, nn = list.size(); ii < nn; ii++) {
                Object o = list.get(ii);
                if ((o instanceof HoverSprite || o instanceof ActionSprite) &&
                        (!(o instanceof DisableableSprite) ||
                         ((DisableableSprite) o).isEnabled())) {
                    return (Sprite) o;
                }
            }
            return null;
        }

        /** The active hover sprite, or action sprite. */
        protected Sprite _activeSprite;
    }

    /** Handles most of our heavy lifting. */
    protected MetaMediaManager _metamgr;

    /** Legacy reference to avoid breaking children in the wild. */
    protected AnimationManager _animmgr;

    /** Legacy reference to avoid breaking children in the wild. */
    protected SpriteManager _spritemgr;

    /** Legacy reference to avoid breaking children in the wild. */
    protected RegionManager _remgr;

    /** Used to correlate tick()s with paint()s. */
    protected boolean _tickPaintPending = false;

    /** The action sprite handler, or null for none. */
    protected ActionSpriteHandler _actionHandler;

    /** The number of action/hover sprites being managed. */
    protected int _actionSpriteCount;

    /** Anyone registered as someone who might obscure the media panel (and thus require extra
     * redrawing. */
    protected ArrayList<Obscurer> _obscurerList;
}
