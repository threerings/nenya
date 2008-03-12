//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2007 Three Rings Design, Inc., All Rights Reserved
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

package com.threerings.media.timer;

import com.samskivert.Log;
import com.samskivert.swing.RuntimeAdjust;
import com.samskivert.util.Interval;
import com.samskivert.util.RunQueue;
import com.threerings.media.MediaPrefs;

/**
 * Calibrates timing values from a subclass' implementation of current against those returned by
 * System.currentTimeMillis. If the subclass timer is moving 1.25 times faster or .75 times slower
 * than currentTimeMillis, hold it to the values from currentTimeMillis.
 */
public abstract class CalibratingTimer
    implements MediaTimer
{
    /**
     * Initializes this timer. Must be called before the timer is used.
     * 
     * @param milliDivider - value by which current() must be divided to get milliseconds
     * @param microDivider - value by which current() must be divided to get microseconds
     */
    protected void init (long milliDivider, long microDivider)
    {
        _milliDivider = milliDivider;
        _microDivider = microDivider;
        reset();
        Log.info("Using " + getClass() + " timer [mfreq=" + _milliDivider + ", ufreq="
            + _microDivider + ", start=" + _startStamp + "].");
    }

    // documentation inherited from interface
    public long getElapsedMicros ()
    {
        return elapsed() / _microDivider;
    }

    // documentation inherited from interface
    public long getElapsedMillis ()
    {
        return elapsed() / _milliDivider;
    }

    // documentation inherited from interface
    public void reset ()
    {
        if (_calibrateInterval != null) {
            _calibrateInterval.cancel();
            _calibrateInterval = null;
        }
        _startStamp = _priorCurrent = current();
        _driftMilliStamp = System.currentTimeMillis();
        _driftTimerStamp = current();
        _calibrateInterval = new Interval(RunQueue.AWT) {
            @Override public void expired () {
                calibrate();
            }
        };
        _calibrateInterval.schedule(5000, true);
    }
    
    /**
     * Returns the greatest drift ratio we've had to compensate for.
     */
    public double getMaxDriftRatio ()
    {
    	return _maxDriftRatio;
    }
    
    /**
     * Clears out our remembered max drift.
     */
    public void clearMaxDriftRatio ()
    {
    	_maxDriftRatio = 1.0;
    }

    /** Returns the difference between _startStamp and current() */
    protected long elapsed ()
    {
        long current = current();
        if (_driftRatio != 1.0) {
            long elapsed = current - _priorCurrent;
            _startStamp += (elapsed - (elapsed * _driftRatio));
            _priorCurrent = current;
        }
        return current - _startStamp;
    }

    /** Calculates the drift factor from the time elapsed from the last calibrate call. */
    protected void calibrate ()
    {
        long current = current();
        double elapsedTimer = (current - _driftTimerStamp);
        double elapsedMillis = System.currentTimeMillis() - _driftMilliStamp;
        double drift = elapsedMillis / (elapsedTimer / _milliDivider);
        if (_debugCalibrate.getValue()) {
            Log.warning("Calibrating [timer=" + elapsedTimer + ", millis=" + elapsedMillis
                + ", drift=" + drift + ", timerstamp=" + _driftTimerStamp + ", millistamp="
                + _driftMilliStamp + ", current=" + current + "]");
        }
        if (drift > MAX_ALLOWED_DRIFT_RATIO || drift < MIN_ALLOWED_DRIFT_RATIO) {
            Log.warning("Calibrating [drift=" + drift + "]");
            _driftRatio = drift;
            if (Math.abs(drift - 1.0) > Math.abs(_maxDriftRatio - 1.0)) {
            	_maxDriftRatio = drift;
            }
        } else if (_driftRatio != 1.0) {
            // If we're within bounds now but we weren't before, reset _driftFactor and log it
            _driftRatio = 1.0;
            Log.warning("Calibrating [drift=" + drift + "]");
        }
        _driftMilliStamp = System.currentTimeMillis();
        _driftTimerStamp = current();
    }
    
    /** Return the current value for this timer. */
    public abstract long current ();
    
    /** The largest drift ratio we'll allow without correcting. */
    protected static final double MAX_ALLOWED_DRIFT_RATIO = 1.1;
    
    /** The smallest drift ratio we'll allow without correcting. */
    protected static final double MIN_ALLOWED_DRIFT_RATIO = 0.9;

    /** current() value when the timer was started. */
    protected long _startStamp;
    
    /** current() value when elapsed was called last. */
    protected long _priorCurrent;

    /** Amount by which current() should be divided to get milliseconds. */
    protected long _milliDivider;

    /** Amount by which current() should be divided to get microseconds. */
    protected long _microDivider;

    /** currentTimeMillis() value from the last time we called calibrate. */
    protected long _driftMilliStamp = System.currentTimeMillis();

    /** current() value from the last time we called calibrate. */
    protected long _driftTimerStamp;

    /** Ratio of currentTimeMillis to timer millis. */
    protected double _driftRatio = 1.0;
    
    /** The largest drift we've had to adjust for. */
    protected double _maxDriftRatio = 1.0;
    
    /** Interval that fires every five seconds to run the calibration. */
    protected Interval _calibrateInterval;

    /** A debug hook that toggles dumping of calibration values. */
    protected static RuntimeAdjust.BooleanAdjust _debugCalibrate = new RuntimeAdjust.BooleanAdjust(
        "Toggles calibrations statistics", "narya.media.timer",
        MediaPrefs.config, false);
}
