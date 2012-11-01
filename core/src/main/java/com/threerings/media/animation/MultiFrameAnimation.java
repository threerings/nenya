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
import java.awt.Rectangle;

import com.threerings.media.util.FrameSequencer;
import com.threerings.media.util.MultiFrameImage;

/**
 * Animates a sequence of image frames in place with a particular frame
 * rate.
 */
public class MultiFrameAnimation extends Animation
{
    /**
     * Creates a multi-frame animation with the specified source image
     * frames and the specified target frame rate (in frames per second).
     *
     * @param frames the source frames of the animation.
     * @param fps the target number of frames per second.
     * @param loop whether the animation should loop indefinitely or
     * finish after one shot.
     */
    public MultiFrameAnimation (
        MultiFrameImage frames, double fps, boolean loop)
    {
        this(frames, new FrameSequencer.ConstantRate(fps, loop));
    }

    /**
     * Creates a multi-frame animation with the specified source image
     * frames and the specified target frame rate (in frames per second).
     */
    public MultiFrameAnimation (MultiFrameImage frames, FrameSequencer seeker)
    {
        // we'll set up our bounds via setLocation() and in reset()
        super(new Rectangle());

        _frames = frames;
        _seeker = seeker;

        // reset ourselves to start things off
        reset();
    }

    @Override
    public Rectangle getBounds ()
    {
        // fill in the bounds with our current animation frame's bounds
        return _bounds;
    }

    @Override
    public void reset ()
    {
        super.reset();

        // set the frame number to -1 so that we don't ignore the
        // transition to frame zero on the first call to tick()
        _fidx = -1;

        // reset our frame sequencer
        setFrameIndex(_seeker.init(_frames));
        if (_seeker instanceof AnimationFrameSequencer) {
            ((AnimationFrameSequencer) _seeker).setAnimation(this);
        }
    }

    @Override
    public void tick (long tickStamp)
    {
        int fidx = _seeker.tick(tickStamp);
        if (fidx == -1) {
            _finished = true;

        } else if (fidx != _fidx) {
            // make a note of our current bounds
            Rectangle obounds = new Rectangle(_bounds);

            // update our frame index and bounds
            setFrameIndex(fidx);

            // invalidate our old and new bounds
            invalidateAfterChange(obounds);
        }
    }

    /**
     * Sets the frame index and updates our dimensions.
     */
    protected void setFrameIndex (int fidx)
    {
        _fidx = fidx;
        _bounds.width = _frames.getWidth(_fidx);
        _bounds.height = _frames.getHeight(_fidx);
    }

    @Override
    public void paint (Graphics2D gfx)
    {
        _frames.paintFrame(gfx, _fidx, _bounds.x, _bounds.y);
    }

    @Override
    public void fastForward (long timeDelta)
    {
        _seeker.fastForward(timeDelta);
    }

    protected MultiFrameImage _frames;
    protected FrameSequencer _seeker;
    protected int _fidx;
}
