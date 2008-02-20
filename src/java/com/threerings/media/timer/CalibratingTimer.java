package com.threerings.media.timer;

import com.samskivert.Log;

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
        _startStamp = current();
        _driftMilliStamp = System.currentTimeMillis();
        _driftTimerStamp = current();
        _callsSinceCalibration = 0;
    }

    /** Returns the difference between _startStamp and current() */
    protected long elapsed ()
    {
        if (_callsSinceCalibration++ > 1000) {
            calibrate();
        }
        return (long)(((current() - _startStamp)) / _driftFactor);
    }

    /** Calculates the drift factor from the time elapsed from the last calibrate call. */
    protected void calibrate ()
    {
        long elapsedNanos = (current() - _driftTimerStamp);
        double elapsedMillis = System.currentTimeMillis() - _driftMilliStamp;
        double drift = (elapsedNanos / 1000000L) / elapsedMillis;
        if (drift > 1.25 || drift < 0.75) {
            if (_driftFactor == 1.0) {
                Log.warning("Calibrating [drift=" + drift + "]");
            }
            _driftFactor = drift;
        } else if (_driftFactor != 1.0) {
            // If we're within bounds now but we weren't before, reset _driftFactor and log it
            _driftFactor = 1.0;
            Log.warning("Calibrating [drift=" + drift + "]");
        }
        _driftMilliStamp = System.currentTimeMillis();
        _driftTimerStamp = current();
        _callsSinceCalibration = 0;
    }

    /** Return the current value for this timer. */
    public abstract long current ();

    /** current() value when the timer was started. */
    protected long _startStamp;

    /** Amount by which current() should be divided to get milliseconds. */
    protected long _milliDivider;

    /** Amount by which current() should be divided to get microseconds. */
    protected long _microDivider;

    /** currentTimeMillis() value from the last time we called calibrate. */
    protected long _driftMilliStamp = System.currentTimeMillis();

    /** current() value from the last time we called calibrate. */
    protected long _driftTimerStamp;

    /** Factor by which the timer values are drifting from currentTimeMillis. */
    protected double _driftFactor = 1.0;

    /** Number of calls to elapsed since we've called calibrate. */
    protected int _callsSinceCalibration = 0;

}
