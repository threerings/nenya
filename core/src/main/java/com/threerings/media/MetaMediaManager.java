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

import java.util.Arrays;
import java.util.Iterator;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import com.google.common.collect.Iterators;

import com.samskivert.util.IntListUtil;
import com.samskivert.util.StringUtil;

import com.samskivert.swing.Label;

import com.threerings.media.animation.Animation;
import com.threerings.media.animation.AnimationManager;
import com.threerings.media.sprite.Sprite;
import com.threerings.media.sprite.SpriteManager;
import com.threerings.media.timer.MediaTimer;

import static com.threerings.media.Log.log;

/**
 * Coordinates interaction between a sprite and animation manager and the media host that hosts and
 * renders them. This class is a little fiddly because {@link MediaPanel} has been around a long
 * time and is thoroughly out in the wild and now that we need to abstract out a bunch of its
 * functionality, we're constrained by all the extant usages and derivations.
 */
public class MetaMediaManager
    implements MediaConstants, Iterable<AbstractMedia>
{
    public MetaMediaManager (FrameManager framemgr, MediaHost host)
    {
        // keep these for later
        _framemgr = framemgr;
        _host = host;

        // initialize our managers
        _animmgr.init(host, _remgr);
        _spritemgr.init(host, _remgr);
    }

    /**
     * Returns the frame manager with which we are coordinating.
     */
    public FrameManager getFrameManager ()
    {
        return _framemgr;
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

    /**
     * Returns the region manager used to coordinate our dirty regions.
     */
    public RegionManager getRegionManager ()
    {
        return _remgr;
    }

    /**
     * Returns true if we are paused, false if we are running normally.
     */
    public boolean isPaused ()
    {
        return _paused;
    }

    /**
     * Pauses the sprites and animations that are currently active on this media panel. Also stops
     * listening to the frame tick while paused.
     */
    public void setPaused (boolean paused)
    {
        // sanity check
        if ((paused && (_pauseTime != 0)) || (!paused && (_pauseTime == 0))) {
            log.warning("Requested to pause when paused or vice-versa", "paused", paused);
            return;
        }

        _paused = paused;
        if (_paused) {
            // make a note of our pause time
            _pauseTime = _framemgr.getTimeStamp();

        } else {
            // let the animation and sprite managers know that we just warped into the future
            long delta = _framemgr.getTimeStamp() - _pauseTime;
            _animmgr.fastForward(delta);
            _spritemgr.fastForward(delta);

            // clear out our pause time
            _pauseTime = 0;
        }
    }

    /**
     * Returns a timestamp from the {@link MediaTimer} used to track time intervals for this media
     * panel. <em>Note:</em> this should only be called from the AWT thread.
     */
    public long getTimeStamp ()
    {
        return _framemgr.getTimeStamp();
    }

    /**
     * Adds a sprite to this panel.
     */
    public void addSprite (Sprite sprite)
    {
        _spritemgr.addSprite(sprite);
    }

    /**
     * @return true if the sprite is already added to this panel.
     */
    public boolean isManaged (Sprite sprite)
    {
        return _spritemgr.isManaged(sprite);
    }

    /**
     * Removes a sprite from this panel.
     */
    public void removeSprite (Sprite sprite)
    {
        _spritemgr.removeSprite(sprite);
    }

    /**
     * Removes all sprites from this panel.
     */
    public void clearSprites ()
    {
        _spritemgr.clearMedia();
    }

    /**
     * Adds an animation to this panel. Animations are automatically removed when they finish.
     */
    public void addAnimation (Animation anim)
    {
        _animmgr.registerAnimation(anim);
    }

    /**
     * @return true if the animation is already added to this panel.
     */
    public boolean isManaged (Animation anim)
    {
        return _animmgr.isManaged(anim);
    }

    /**
     * Aborts a currently running animation and removes it from this panel. Animations are normally
     * automatically removed when they finish.
     */
    public void abortAnimation (Animation anim)
    {
        _animmgr.unregisterAnimation(anim);
    }

    /**
     * Removes all animations from this panel.
     */
    public void clearAnimations ()
    {
        _animmgr.clearMedia();
    }

    /**
     * Called by the host to coordinate dirty region tracking. This should be supplied with the
     * number of dirty regions being painted on this tick and called just before painting them.
     */
    public void noteDirty (int regions)
    {
        _dirty[_tick] = regions;
    }

    /**
     * Our media front end should implement {@link FrameParticipant} and call this method in their
     * {@link FrameParticipant#tick} method. They must also first check {@link #isPaused} and not
     * call this method if we are paused. As they will probably want to have willTick() and
     * didTick() calldown methods, we cannot handle pausedness for them.
     */
    public void tick (long tickStamp)
    {
        // now tick our animations and sprites
        _animmgr.tick(tickStamp);
        _spritemgr.tick(tickStamp);

        // if performance debugging is enabled,
        if (FrameManager._perfDebug.getValue()) {
            if (_perfLabel == null) {
                _perfLabel = new Label("", Label.OUTLINE, Color.white, Color.black,
                                       new Font("Arial", Font.PLAIN, 10));
            }
            if (_perfRect == null) {
                _perfRect = new Rectangle(5, 5, 0, 0);
            }

            StringBuilder perf = new StringBuilder();
            perf.append("[FPS: ");
            perf.append(_framemgr.getPerfTicks()).append("/");
            perf.append(_framemgr.getPerfTries());
            perf.append(" PM:");
            StringUtil.toString(perf, _framemgr.getPerfMetrics());
//             perf.append(" MP:").append(_dirtyPerTick);
            perf.append("]");

            String perfStatus = perf.toString();
            if (!_perfStatus.equals(perfStatus)) {
                _perfStatus = perfStatus;
                _perfLabel.setText(perfStatus);

                Graphics2D gfx = _host.createGraphics();
                if (gfx != null) {
                    _perfLabel.layout(gfx);
                    gfx.dispose();

                    // make sure the region we dirty contains the old and the new text (which we
                    // ensure by never letting the rect shrink)
                    Dimension psize = _perfLabel.getSize();
                    _perfRect.width = Math.max(_perfRect.width, psize.width);
                    _perfRect.height = Math.max(_perfRect.height, psize.height);
                    _remgr.addDirtyRegion(new Rectangle(_perfRect));
                }
            }
        } else {
            _perfRect = null;
        }
    }

    /**
     * Our media front end should implement {@link FrameParticipant} and call this method in their
     * {@link FrameParticipant#needsPaint} method.
     */
    public boolean needsPaint ()
    {
        // compute our average dirty regions per tick
        if (_tick++ == 99) {
            _tick = 0;
            int dirty = IntListUtil.sum(_dirty);
            Arrays.fill(_dirty, 0);
            _dirtyPerTick = (float)dirty/100;
        }

        // regardless of whether or not we paint, we need to let our abstract media managers know
        // that we've gotten to the point of painting because they need to remain prepared to deal
        // with media changes that happen any time between the tick() and the paint() and thus need
        // to know when paint() happens
        _animmgr.willPaint();
        _spritemgr.willPaint();

        // if we have dirty regions, we need painting
        return _remgr.haveDirtyRegions();
    }

    /**
     * Renders the sprites and animations that intersect the supplied dirty region in the specified
     * layer.
     */
    public void paintMedia (Graphics2D gfx, int layer, Rectangle dirty)
    {
        if (layer == FRONT) {
            _spritemgr.paint(gfx, layer, dirty);
            _animmgr.paint(gfx, layer, dirty);
        } else {
            _animmgr.paint(gfx, layer, dirty);
            _spritemgr.paint(gfx, layer, dirty);
        }
    }

    /**
     * Renders our performance debugging information if enabled.
     */
    public void paintPerf (Graphics2D gfx)
    {
        if (_perfRect != null && FrameManager._perfDebug.getValue()) {
            gfx.setClip(null);
            _perfLabel.render(gfx, _perfRect.x, _perfRect.y);
        }
    }

    /**
     * If our host supports scrolling around in a virtual view, it should call this method when the
     * view origin changes.
     */
    public void viewLocationDidChange (int dx, int dy)
    {
        if (_perfRect != null) {
            Rectangle sdirty = new Rectangle(_perfRect);
            sdirty.translate(-dx, -dy);
            _remgr.addDirtyRegion(sdirty);
        }

        // let our sprites and animations know what's up
        _animmgr.viewLocationDidChange(dx, dy);
        _spritemgr.viewLocationDidChange(dx, dy);
    }

    public Iterator<AbstractMedia> iterator ()
    {
        return Iterators.concat(_spritemgr.enumerateSprites(), _animmgr.iterator());
    }

    /** The frame manager with whom we register. */
    protected FrameManager _framemgr;

    /** Our media host, so gracious and accomodating. */
    protected MediaHost _host;

    /** Used to accumulate and merge dirty regions on each tick. */
    protected RegionManager _remgr = new RegionManager();

    /** The animation manager in use by this panel. */
    protected AnimationManager _animmgr = new AnimationManager();

    /** The sprite manager in use by this panel. */
    protected SpriteManager _spritemgr = new SpriteManager();

    /** Whether we're currently paused. */
    protected boolean _paused;

    /** Used to track the clock time at which we were paused. */
    protected long _pauseTime;

    /** Used to keep metrics. */
    protected int[] _dirty = new int[200];

    /** Used to keep metrics. */
    protected int _tick;

    /** Used to keep metrics. */
    protected float _dirtyPerTick;

    // used to render performance metrics
    protected String _perfStatus = "";
    protected Label _perfLabel;
    protected Rectangle _perfRect;
}
