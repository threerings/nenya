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

package com.threerings.openal;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

/**
 * Represents the OpenAL listener object.
 */
public class Listener
{
    /**
     * Sets the position of the listener.
     */
    public void setPosition (float x, float y, float z)
    {
        if (_px != x || _py != y || _pz != z) {
            AL10.alListener3f(AL10.AL_POSITION, _px = x, _py = y, _pz = z);
        }
    }

    /**
     * Returns the x component of the listener's position.
     */
    public float getPositionX ()
    {
        return _px;
    }

    /**
     * Returns the y component of the listener's position.
     */
    public float getPositionY ()
    {
        return _py;
    }

    /**
     * Returns the z component of the listener's position.
     */
    public float getPositionZ ()
    {
        return _pz;
    }

    /**
     * Sets the velocity of the listener.
     */
    public void setVelocity (float x, float y, float z)
    {
        if (_vx != x || _vy != y || _vz != z) {
            AL10.alListener3f(AL10.AL_VELOCITY, _vx = x, _vy = y, _vz = z);
        }
    }

    /**
     * Sets the gain of the listener.
     */
    public void setGain (float gain)
    {
        if (_gain != gain) {
            AL10.alListenerf(AL10.AL_GAIN, _gain = gain);
        }
    }

    /**
     * Sets the orientation of the listener in terms of an "at" (direction) and "up" vector.
     */
    public void setOrientation (float ax, float ay, float az, float ux, float uy, float uz)
    {
        if (_ax != ax || _ay != ay || _az != az || _ux != ux || _uy != uy || _uz != uz) {
            _vbuf.put(_ax = ax).put(_ay = ay).put(_az = az);
            _vbuf.put(_ux = ux).put(_uy = uy).put(_uz = uz).rewind();
            AL10.alListener(AL10.AL_ORIENTATION, _vbuf);
        }
    }

    /**
     * The listener is only to be created by the {@link SoundManager}.
     */
    protected Listener ()
    {
    }

    /** The position of the listener. */
    protected float _px, _py, _pz;

    /** The velocity of the listener. */
    protected float _vx, _vy, _vz;

    /** The gain of the listener. */
    protected float _gain = 1f;

    /** The orientation of the listener (initialized to the OpenAL defaults). */
    protected float _ax, _ay, _az = -1f, _ux, _uy = 1f, _uz;

    /** A buffer for floating point values. */
    protected FloatBuffer _vbuf = BufferUtils.createFloatBuffer(6);
}
