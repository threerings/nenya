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

import java.awt.Graphics2D;

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
    protected MediaOverlay (FrameManager fmgr)
    {
        _framemgr = fmgr;
        _remgr = new RegionManager();
        _animmgr = new AnimationManager(this);
        _spritemgr = new SpriteManager(this);
    }

    /**
     * Returns a reference to the animation manager used by this media panel.
     */
    public AnimationManager getAnimationManager ()
    {
        return _animmgr;
    }

    /**
     * Returns a reference to the sprite manager used by this media panel.
     */
    public SpriteManager getSpriteManager ()
    {
        return _spritemgr;
    }

    // from interface MediaHost
    public Graphics2D createGraphics ()
    {
        return _framemgr.createGraphics();
    }

    // from interface MediaHost
    public RegionManager getRegionManager ()
    {
        return _remgr;
    }

    /** The frame manager with whom we cooperate. */
    protected FrameManager _framemgr;

    /** The animation manager in use by this overlay. */
    protected AnimationManager _animmgr;

    /** The sprite manager in use by this overlay. */
    protected SpriteManager _spritemgr;

    /** Used to accumulate and merge dirty regions on each tick. */
    protected RegionManager _remgr;
}
