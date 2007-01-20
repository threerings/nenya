//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2006 Three Rings Design, Inc., All Rights Reserved
// http://www.threerings.net/code/nenya/
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

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.JRootPane;

import com.threerings.media.animation.AnimationManager;
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
     * Returns a reference to the sprite manager used by this media panel.
     */
    public SpriteManager getSpriteManager ()
    {
        return _metamgr.getSpriteManager();
    }

    /**
     * Called by the {@link FrameManager} to propagate our dirty regions to the active repaint
     * manager so that it can repaint the underlying components just prior to our painting our
     * media. This will be followe by a call to {@link paint} after the components have been
     * repainted.
     */
    public void propagateDirtyRegions (ActiveRepaintManager repmgr, JRootPane root)
    {
        // this will clear out our region manager, so we need to keep these around for our
        // subsequent call to paint
        _dirty = _metamgr.getRegionManager().getDirtyRegions();
        for (int ii = 0; ii < _dirty.length; ii++) {
            Rectangle dirty = _dirty[ii];
            repmgr.addDirtyRegion(root, dirty.x, dirty.y, dirty.width, dirty.height);
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
        if (_metamgr.needsPaint()) {
            for (int ii = 0; ii < _dirty.length; ii++) {
                gfx.setClip(_dirty[ii]);
                _metamgr.paintMedia(gfx, MediaConstants.BACK, _dirty[ii]);
                _metamgr.paintMedia(gfx, MediaConstants.FRONT, _dirty[ii]);
            }
            return true;
        }
        return false;
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
