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

import java.awt.Component;

public abstract class FrameInterval
    implements FrameParticipant
{
    /**
     * Constructor - registers the interval as a frame participant
     */
    public FrameInterval (FrameManager mgr)
    {
        _mgr = mgr;
    }

    // documentation inhertied from FrameParticipant
    public Component getComponent ()
    {
        return null;
    }

    // documentation inherited from FrameParticipant
    public boolean needsPaint ()
    {
        return false;
    }

    // documentation inherited from FrameParticipant
    public void tick (long tickStamp)
    {
        if (_nextTime == -1) {
            // First time through
            _nextTime = tickStamp + _initDelay;
        } else if (tickStamp >= _nextTime) {

            // If we're repeating, set the next time to run, otherwise, reset
            if (_repeatDelay != 0L) {
                _nextTime += _repeatDelay;
            } else {
                _nextTime = -1;
                cancel();
            }
            expired();
        }
    }

    /**
     *
     * The main method where your interval should do its work.
     *
     */
    public abstract void expired ();

    /**
     * Schedule the interval to execute once, after the specified delay.
     * Supersedes any previous schedule that this Interval may have had.
     */
    public final void schedule (long delay)
    {
        schedule(delay, 0L);
    }

    /**
     * Schedule the interval to execute repeatedly, with the same delay.
     * Supersedes any previous schedule that this Interval may have had.
     */
    public final void schedule (long delay, boolean repeat)
    {
        schedule(delay, repeat ? delay : 0L);
    }

    /**
     * Schedule the interval to execute repeatedly with the specified
     * initial delay and repeat delay.
     * Supersedes any previous schedule that this Interval may have had.
     */
    public final void schedule (long initialDelay, long repeatDelay)
    {
        if (!_mgr.isRegisteredFrameParticipant(this)) {
            _mgr.registerFrameParticipant(this);
        }

        _repeatDelay = repeatDelay;
        _initDelay = initialDelay;
        _nextTime = -1;
    }

    /**
     * Cancel the current schedule, and ensure that any expirations that
     * are queued up but have not yet run do not run.
     */
    public final void cancel ()
    {
        _mgr.removeFrameParticipant(this);
    }

    /** Time of the next expiration. */
    protected long _nextTime;

    /** Time between expirations. */
    protected long _repeatDelay;

    /** Time between expirations. */
    protected long _initDelay;

    /** The context whose FrameManager we are using. */
    protected FrameManager _mgr;
}
