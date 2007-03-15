package com.threerings.flash {

import flash.utils.getTimer;

/**
 * Tracks multiple sine waves with different periods and amplitudes, and
 * returns a
 */
public class Siner
{
    /**
     * @param args: amplitude1, period1, amplitude2, period2...
     * Periods are specified in seconds.
     *
     * If constructed with more than one amplitude, the amplitudes are
     * additive.
     * 
     * The Siner will start in the reset() state.
     */
    public function Siner (... args)
    {
        if (args.length == 0 || (args.length % 2 != 0)) {
            throw new ArgumentError();
        }
        while (args.length > 0) {
            if (!(args[0] is Number) || !(args[1] is Number)) {
                throw new ArgumentError();
            }
            var amp :Number = args.shift();
            var per :Number = args.shift();

            _incs.push(TWO_PI / (per * 1000));
            _amps.push(amp);
        }
        reset();
    }

    /**
     * Reset to 0, with the amplitude about to increase.
     */
    public function reset () :void
    {
        for (var ii :int = _amps.length - 1; ii >= 0; ii--) {
            _values[ii] = 3 * Math.PI / 2;
        }
        _stamp = getTimer();
    }

    /**
     * Randomize the value.
     */
    public function randomize () :void
    {
        for (var ii :int = _amps.length - 1; ii >= 0; ii--) {
            _values[ii] = Math.random() * TWO_PI;
        }
    }

    /**
     * Access the instantaneous value, which can range from
     * [ -totalAmplitude, totalAmplitude ].
     *
     * Note: timestamps are only kept relative to the last access of the value,
     * and floating point math is used, so things could get a little "off" after
     * a while, and the frequency with which you sample the value will impact
     * the error. You cope.
     */
    public function get value () :Number
    {
        var now :Number = getTimer();
        var elapsed :Number = now - _stamp;
        _stamp = now;

        var accum :Number = 0;
        for (var ii :int = _values.length - 1; ii >= 0; ii--) {
            var val :Number = (_values[ii] + (_incs[ii] * elapsed)) % TWO_PI;
            _values[ii] = val;
            accum += _amps[ii] * Math.sin(val);
        }
        return accum;
    }

    protected var _values :Array = [];
    protected var _incs :Array = [];
    protected var _amps :Array = [];

    protected var _stamp :Number;

    protected static const TWO_PI :Number = 2 * Math.PI;
}

}
