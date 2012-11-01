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

package com.threerings.media.util;

/**
 * Used to vary a value over time where time is provided at discrete
 * increments (on the frame tick) and the value is computed appropriately.
 */
public abstract class TimeFunction
{
    /**
     * Every time function varies a value from some starting value to some
     * ending value over some duration. The way in which it varies
     * (linearly, for example) is up to the derived class.
     *
     * <p><em>Note:</em> it is assumed that we will operate with
     * relatively short durations such that integer arithmetic may be used
     * rather than long arithmetic.
     */
    public TimeFunction (int start, int end, int duration)
    {
        _start = start;
        _end = end;
        _duration = duration;
    }

    /**
     * Configures this function with a starting time. This method need not
     * be called, and instead the first vall to {@link #getValue} will be
     * used to obtain a starting time stamp.
     */
    public void init (long tickStamp)
    {
        _startStamp = tickStamp;
    }

    /**
     * Called to fast forward our time stamps if we are ever paused and
     * need to resume where we left off.
     */
    public void fastForward (long timeDelta)
    {
        _startStamp += timeDelta;
    }

    /**
     * Returns the current value given the supplied time stamp. The value
     * will be bounded to the originally supplied starting and ending
     * values at times 0 (and below) and {@link #_duration} (and above)
     * respectively.
     */
    public int getValue (long tickStamp)
    {
        if (_startStamp == 0L) {
            _startStamp = tickStamp;
        }

        int dt = (int)(tickStamp - _startStamp);
        if (dt <= 0) {
            return _start;
        } else if (dt >= _duration) {
            return _end;
        } else {
            return computeValue(dt);
        }
    }

    /**
     * This must be implemented by our derived class to compute our value
     * given the specified elapsed time (in millis).
     */
    protected abstract int computeValue (int dt);

    /** Our starting and ending values. */
    protected int _start, _end;

    /** The number of milliseconds over which we vary our value. */
    protected int _duration;

    /** The timestamp at which we began varying our value. */
    protected long _startStamp;
}
