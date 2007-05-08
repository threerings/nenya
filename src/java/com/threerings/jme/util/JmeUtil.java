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

package com.threerings.jme.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.nio.IntBuffer;

import org.lwjgl.opengl.ARBShaderObjects;

import com.jme.math.Matrix4f;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Controller;
import com.jme.scene.state.GLSLShaderObjectsState;
import com.jme.system.DisplaySystem;
import com.jme.util.geom.BufferUtils;

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
     * Sets a matrix to the transform defined by the given translation,
     * rotation, and scale values.
     */
    public static Matrix4f setTransform (
        Vector3f translation, Quaternion rotation, Vector3f scale, Matrix4f result)
    {
        result.setRotationQuaternion(rotation);
        result.setTranslation(translation);

        result.m00 *= scale.x;
        result.m01 *= scale.y;
        result.m02 *= scale.z;

        result.m10 *= scale.x;
        result.m11 *= scale.y;
        result.m12 *= scale.z;

        result.m20 *= scale.x;
        result.m21 *= scale.y;
        result.m22 *= scale.z;

        return result;
    }

    /**
     * Attempts to parse a string containing an axis: either "x", "y", or "z", or three
     * comma-delimited values representing an axis vector.  The value returned may be one of
     * JME's "constant" vectors (for example, {@link Vector3f#UNIT_X}), so don't modify it.
     */
    public static Vector3f parseAxis (String axis)
    {
        if ("x".equalsIgnoreCase(axis)) {
            return Vector3f.UNIT_X;
        } else if ("y".equalsIgnoreCase(axis)) {
            return Vector3f.UNIT_Y;
        } else if ("z".equalsIgnoreCase(axis)) {
            return Vector3f.UNIT_Z;
        } else {
            Vector3f vector = parseVector3f(axis);
            if (vector != null) {
                vector.normalizeLocal();
            }
            return vector;
        }
    }

    /**
     * Attempts to parse a string containing three comma-delimited values and return a
     * {@link Vector3f}.
     */
    public static Vector3f parseVector3f (String vector)
    {
        if (vector != null) {
            float[] vals = StringUtil.parseFloatArray(vector);
            if (vals != null && vals.length == 3) {
                return new Vector3f(vals[0], vals[1], vals[2]);
            } else {
                Log.warning("Invalid vector [vector=" + vector + "].");
            }
        }
        return null;
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

    /**
     * Loads the specified shaders (prepending the supplied preprocessor definitions)
     * and returns a GLSL shader state.  One of the supplied streams may be <code>null</code>
     * in order to use the fixed-function pipeline for that part.  The method returns
     * <code>null</code> if the shaders fail to compile (JME will log an error).
     *
     * @param defs a number of preprocessor definitions to be #defined in both shaders
     * (e.g., "ENABLE_FOG", "NUM_LIGHTS 2").
     */
    public static GLSLShaderObjectsState loadShaders (
        InputStream vert, InputStream frag, String... defs)
        throws IOException
    {
        GLSLShaderObjectsState sstate =
            DisplaySystem.getDisplaySystem().getRenderer().createGLSLShaderObjectsState();
        sstate.load(
            (vert == null) ? null : readSource(vert, defs),
            (frag == null) ? null : readSource(frag, defs));

        // check its link status
        IntBuffer linked = BufferUtils.createIntBuffer(1);
        ARBShaderObjects.glGetObjectParameterARB(sstate.getProgramID(),
            ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB, linked);
        return (linked.get() == 0) ? null : sstate;
    }

    /**
     * Loads an entire source file as a string, prepending the supplied preprocessor definitions.
     */
    protected static String readSource (InputStream in, String[] defs)
        throws IOException
    {
        StringBuffer buf = new StringBuffer();
        String ln = System.getProperty("line.separator");
        for (String def : defs) {
            buf.append("#define ").append(def).append(ln);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        while ((line = reader.readLine()) != null) {
            buf.append(line).append(ln);
        }
        return buf.toString();
    }
}
