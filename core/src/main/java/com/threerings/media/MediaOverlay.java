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

import java.util.List;

import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JRootPane;

import com.threerings.media.animation.Animation;
import com.threerings.media.animation.AnimationManager;
import com.threerings.media.sprite.Sprite;
import com.threerings.media.sprite.SpriteManager;

/**
 * Provides an overlaid media "canvas" that allows for the rendering of sprites and animations on
 * top of everything else that takes place the view managed by a {@link FrameManager}. The media
 * overlay coordinates through the {@link ActiveRepaintManager} to repaint areas of the screen that
 * it has left dirty.
 */
public class MediaOverlay
    implements MediaHost
{
    /**
     * Returns a reference to the animation manager used by this media panel.
     */
    public AnimationManager getAnimationManager ()
    {
        return _metamgr.getAnimationManager();
    }

    /**
     * Returns a reference to the sprite manager used by this media overlay.
     */
    public SpriteManager getSpriteManager ()
    {
        return _metamgr.getSpriteManager();
    }

    /**
     * Adds a sprite to this overlay.
     */
    public void addSprite (Sprite sprite)
    {
        _metamgr.addSprite(sprite);
    }

    /**
     * @return true if the sprite is already added to this overlay.
     */
    public boolean isManaged (Sprite sprite)
    {
        return _metamgr.isManaged(sprite);
    }

    /**
     * Removes a sprite from this overlay.
     */
    public void removeSprite (Sprite sprite)
    {
        _metamgr.removeSprite(sprite);
    }

    /**
     * Removes all sprites from this overlay.
     */
    public void clearSprites ()
    {
        _metamgr.clearSprites();
    }

    /**
     * Adds an animation to this overlay. Animations are automatically removed when they finish.
     */
    public void addAnimation (Animation anim)
    {
        _metamgr.addAnimation(anim);
    }

    /**
     * @return true if the animation is already added to this overlay.
     */
    public boolean isManaged (Animation anim)
    {
        return _metamgr.isManaged(anim);
    }

    /**
     * Aborts a currently running animation and removes it from this overlay. Animations are
     * normally automatically removed when they finish.
     */
    public void abortAnimation (Animation anim)
    {
        _metamgr.abortAnimation(anim);
    }

    /**
     * Removes all animations from this overlay.
     */
    public void clearAnimations ()
    {
        _metamgr.clearAnimations();
    }

    /**
     * Adds a dirty region to this overlay.
     */
    public void addDirtyRegion (Rectangle rect)
    {
        // Only add dirty regions where rect intersects our actual media as the set region will
        // propagate out to the repaint manager.
        for (AbstractMedia media : _metamgr) {
            Rectangle intersection = media.getBounds().intersection(rect);
            if (!intersection.isEmpty()) {
                _metamgr.getRegionManager().addDirtyRegion(intersection);
            }
        }
    }

    /**
     * Called by the {@link FrameManager} to propagate our dirty regions to the active repaint
     * manager so that it can repaint the underlying components just prior to our painting our
     * media. This will be followed by a call to {@link #paint} after the components have been
     * repainted.
     */
    public void propagateDirtyRegions (ActiveRepaintManager repmgr, JRootPane root)
    {
        if (_metamgr.needsPaint()) {
            // tell the repaint manager about our raw (unmerged) dirty regions so that it can dirty
            // only components that are actually impacted
            List<Rectangle> dlist = _metamgr.getRegionManager().peekDirtyRegions();
            for (int ii = 0, ll = dlist.size(); ii < ll; ii++) {
                Rectangle dirty = dlist.get(ii);
                repmgr.addDirtyRegion(root, dirty.x - root.getX(), dirty.y - root.getY(),
                                      dirty.width, dirty.height);
            }
        }
    }

    /**
     * Called by the {@link FrameManager} after everything is done painting, allowing us to paint
     * gloriously overtop of everything in the frame.
     *
     * @return true if we painted something, false otherwise.
     */
    public boolean paint (Graphics2D gfx)
    {
        if (!_metamgr.getRegionManager().haveDirtyRegions()) {
            return false;
        }

        Rectangle[] dirty = _metamgr.getRegionManager().getDirtyRegions();
        for (Rectangle element : dirty) {
            gfx.setClip(element);
            _metamgr.paintMedia(gfx, MediaConstants.BACK, element);
            _metamgr.paintMedia(gfx, MediaConstants.FRONT, element);
        }
        return true;
    }

    // from interface MediaHost
    public Graphics2D createGraphics ()
    {
        return _metamgr.getFrameManager().createGraphics();
    }

    /**
     * Called by the frame manager on every tick.
     */
    public void tick (long tickStamp)
    {
        if (!_metamgr.isPaused()) {
            // tick our meta manager which will tick our sprites and animations
            _metamgr.tick(tickStamp);
        }
    }

    /**
     * Creates a media overlay. Only the {@link FrameManager} will construct an instance.
     */
    protected MediaOverlay (FrameManager fmgr)
    {
        _framemgr = fmgr;
        _metamgr = new MetaMediaManager(fmgr, this);
    }

    /** The frame manager with whom we cooperate. */
    protected FrameManager _framemgr;

    /** Handles the heavy lifting involving media. */
    protected MetaMediaManager _metamgr;

    /** A temporary list of dirty rectangles maintained during the painting process. */
    protected Rectangle[] _dirty;
}
