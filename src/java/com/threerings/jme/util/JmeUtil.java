//
// $Id: SpatialVisitor.java 119 2007-01-24 00:22:12Z dhoover $

package com.threerings.jme.util;

import com.jme.math.Vector3f;
import com.jme.scene.Controller;

import com.samskivert.util.StringUtil;

import com.threerings.jme.Log;

/**
 * Some static classes and methods of general utility to applications using JME.
 */
public class JmeUtil
{
    /**
     * Represents the current position and direction in an animation composed of a fixed number of
     * discrete frames using one of the repeat types defined in {@link Controller}.
     */
    public static class FrameState
    {
        /** The index of the current frame. */
        public int idx;
        
        /** The current direction of animation. */
        public int dir = +1;
        
        /** The fractional progress towards the next frame. */
        public float accum;
        
        /**
         * Resets the state back to the beginning.
         */
        public void reset ()
        {
            set(0, +1, 0f);
        }
        
        /**
         * Sets the entire frame state.
         */
        public void set (int idx, int dir, float accum)
        {
            this.idx = idx;
            this.dir = dir;
            this.accum = accum;
        }
        
        /**
         * Updates the frame state after some amount of time has elapsed.
         */
        public void update (float elapsed, float frameRate, int frameCount, int repeatType)
        {
            float frames = elapsed * frameRate;
            for (accum += frames; accum >= 1f; accum -= 1f) {
                advance(frameCount, repeatType);
            }
        }
        
        /**
         * Advances the position by one frame.
         */
        public void advance (int frameCount, int repeatType)
        {
            if ((idx += dir) >= frameCount) {
                if (repeatType == Controller.RT_CLAMP) {
                    idx = frameCount - 1;
                    dir = 0;
                } else if (repeatType == Controller.RT_WRAP) {
                    idx = 0;
                } else { // repeatType == Controller.RT_CYCLE
                    idx = frameCount - 2;
                    dir = -1;
                }
            } else if (idx < 0) {
                idx = 1;
                dir = +1;
            }
        }
    }
    
    /**
     * Attempts to parse a string containing an axis: either "x", "y", or "z", or three
     * comma-delimited values representing an axis vector.  The value returned may be one of
     * JME's "constant" vectors (for example, {@link Vector3f#UNIT_X}), so don't modify it.
     */
    public static Vector3f parseAxis (String axis)
    {
        if (axis == null) {
            return null;
        }
        if (axis.equalsIgnoreCase("x")) {
            return Vector3f.UNIT_X;
        } else if (axis.equalsIgnoreCase("y")) {
            return Vector3f.UNIT_Y;
        } else if (axis.equalsIgnoreCase("z")) {
            return Vector3f.UNIT_Z;
        }
        float[] vals = StringUtil.parseFloatArray(axis);
        if (vals != null && vals.length == 3) {
            return new Vector3f(vals[0], vals[1], vals[2]).normalizeLocal();
        } else {
            Log.warning("Invalid axis [axis=" + axis + "].");
            return null;
        }
    }
    
    /**
     * Attempts to parse a string describing one of the repeat types defined in {@link Controller}:
     * "clamp", "cycle", or "wrap".  Will return the specified default if the type is
     * <code>null</code> or invalid.
     */
    public static int parseRepeatType (String type, int defaultType)
    {
        if ("clamp".equals(type)) {
            return Controller.RT_CLAMP;
        } else if ("cycle".equals(type)) {
            return Controller.RT_CYCLE;
        } else if ("wrap".equals(type)) {
            return Controller.RT_WRAP;
        } else if (type != null) {
            Log.warning("Invalid repeat type [type=" + type + "].");
        }
        return defaultType;
    }
}
