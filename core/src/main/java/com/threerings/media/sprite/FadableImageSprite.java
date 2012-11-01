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

package com.threerings.media.sprite;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;

import com.threerings.media.image.Mirage;
import com.threerings.media.util.MultiFrameImage;
import com.threerings.media.util.Path;

public class FadableImageSprite extends OrientableImageSprite
{
    /**
     * Creates a new fadable image sprite.
     */
    public FadableImageSprite ()
    {}

    /**
     * Creates a new fadable image sprite.
     *
     * @param image the image to render
     */
    public FadableImageSprite (Mirage image)
    {
        super(image);
    }

    /**
     * Creates a new fadable image sprite.
     *
     * @param frames the frames to render
     */
    public FadableImageSprite (MultiFrameImage frames)
    {
        super(frames);
    }

    /**
     * Fades this sprite in over the specified duration after waiting for the specified delay.
     */
    public void fadeIn (long delay, long duration)
    {
        setAlpha(0.0f);

        _fadeStamp = 0;
        _fadeDelay = delay;
        _fadeInDuration = duration;
    }

    /**
     * Fades this sprite out over the specified duration after waiting for the specified delay.
     */
    public void fadeOut (long delay, long duration)
    {
        setAlpha(1.0f);

        _fadeStamp = 0;
        _fadeDelay = delay;
        _fadeOutDuration = duration;
    }

    /**
     * Puts this sprite on the specified path and fades it in over the specified duration.
     *
     * @param path the path to move along
     * @param fadePortion the portion of time to spend fading in, from 0.0f (no time) to 1.0f (the
     * entire time)
     */
    public void moveAndFadeIn (Path path, long pathDuration, float fadePortion)
    {
        move(path);

        setAlpha(0.0f);

        _fadeInDuration = (long)(pathDuration * fadePortion);
    }

    /**
     * Puts this sprite on the specified path and fades it out over the specified duration.
     *
     * @param path the path to move along
     * @param pathDuration the duration of the path
     * @param fadePortion the portion of time to spend fading out, from 0.0f (no time) to 1.0f
     * (the entire time)
     */
    public void moveAndFadeOut (Path path, long pathDuration, float fadePortion)
    {
        move(path);

        setAlpha(1.0f);

        _fadeStamp = 0;
        _pathDuration = pathDuration;
        _fadeOutDuration = (long)(pathDuration * fadePortion);
        _fadeDelay = _pathDuration - _fadeOutDuration;
    }

    /**
     * Puts this sprite on the specified path, fading it in over the specified duration at the
     * beginning and fading it out at the end.
     *
     * @param path the path to move along
     * @param pathDuration the duration of the path
     * @param fadePortion the portion of time to spend fading in/out, from 0.0f (no time) to 1.0f
     * (the entire time)
     */
    public void moveAndFadeInAndOut (Path path, long pathDuration, float fadePortion)
    {
        move(path);

        setAlpha(0.0f);

        _pathDuration = pathDuration;
        _fadeInDuration = _fadeOutDuration = (long)(pathDuration * fadePortion);
    }

    @Override
    public void tick (long tickStamp)
    {
        super.tick(tickStamp);

        if (_fadeInDuration != -1) {
            if (_path != null && (tickStamp - _pathStamp) <= _fadeInDuration) {
                // fading in while moving
                float alpha = (float)(tickStamp - _pathStamp) / _fadeInDuration;
                if (alpha >= 1.0f) {
                    completeFadeIn();
                } else {
                    setAlpha(alpha);
                }

            } else {
                // fading in while stationary
                if (_fadeStamp == 0) {
                    // store the time at which fade started
                    _fadeStamp = tickStamp;
                }
                if (tickStamp > _fadeStamp + _fadeDelay) {
                    // initial delay has passed
                    float alpha = (float)(tickStamp - _fadeStamp - _fadeDelay) / _fadeInDuration;
                    if (alpha >= 1.0f) {
                        completeFadeIn();
                    } else {
                        setAlpha(alpha);
                    }
                }
            }

        } else if (_fadeOutDuration != -1) {
            if (_fadeStamp == 0) {
                // store the time at which fade started
                _fadeStamp = tickStamp;
            }

            if (tickStamp > _fadeStamp + _fadeDelay) {
                // initial delay has passed
                float alpha = 1f - (float)(tickStamp - _fadeStamp - _fadeDelay) / _fadeOutDuration;
                if (alpha <= 0.0f) {
                    completeFadeOut();
                } else {
                    setAlpha(alpha);
                }
            }
        }
    }

    @Override
    public void pathCompleted (long timestamp)
    {
        if (_fadeInDuration != -1) {
            completeFadeIn();
        } else if (_fadeOutDuration != -1) {
            completeFadeOut();
        }
        super.pathCompleted(timestamp);
    }

    /** Completes the process of fading in. */
    protected void completeFadeIn ()
    {
        setAlpha(1.0f);
        _fadeInDuration = -1;
    }

    /** Completes the process of fading out. */
    protected void completeFadeOut ()
    {
        setAlpha(0.0f);
        _fadeOutDuration = -1;
    }

    @Override
    public void paint (Graphics2D gfx)
    {
        if (_alphaComposite.getAlpha() < 1.0f) {
            Composite ocomp = gfx.getComposite();
            gfx.setComposite(_alphaComposite);
            super.paint(gfx);
            gfx.setComposite(ocomp);

        } else {
            super.paint(gfx);
        }
    }

    /**
     * Sets the alpha value of this sprite.
     */
    public void setAlpha (float alpha)
    {
        if (alpha < 0.0f) {
            alpha = 0.0f;

        } else if (alpha > 1.0f) {
            alpha = 1.0f;
        }
        if (alpha != _alphaComposite.getAlpha()) {
            _alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
            if (_mgr != null) {
                _mgr.getRegionManager().invalidateRegion(_bounds);
            }
        }
    }

    /**
     * Returns the alpha value of this sprite.
     */
    public float getAlpha ()
    {
        return _alphaComposite.getAlpha();
    }

    /** The alpha composite. */
    protected AlphaComposite _alphaComposite =
        AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);

    /** If fading in, the fade-in duration (otherwise -1). */
    protected long _fadeInDuration = -1;

    /** If fading in without moving, the fade-in delay. */
    protected long _fadeDelay;

    /** The time at which fading started. */
    protected long _fadeStamp = -1;

    /** If fading out, the fade-out duration (otherwise -1). */
    protected long _fadeOutDuration = -1;

    /** If fading out, the path duration. */
    protected long _pathDuration;
}
