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
        _calibrationInterval = 5 * 1000 * _milliDivider;// Calibrate every 5 seconds
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
        _startStamp = _priorCurrent = current();
        _driftMilliStamp = System.currentTimeMillis();
        _driftTimerStamp = current();
    }

    /** Returns the difference between _startStamp and current() */
    protected long elapsed ()
    {
        long current = current();
        if ((current - _driftTimerStamp) > _calibrationInterval) {
            calibrate();
        }
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
        long elapsedTimer = (current() - _driftTimerStamp);
        double elapsedMillis = System.currentTimeMillis() - _driftMilliStamp;
        double drift = elapsedMillis / (elapsedTimer / _milliDivider);
        if (drift > 1.25 || drift < 0.75) {
            Log.warning("Calibrating [drift=" + drift + "]");
            _driftRatio = drift;
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

    /** Amount of timer ticks that elapse between calibrations. */
    protected long _calibrationInterval;

}
