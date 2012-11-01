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

import java.util.ArrayList;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;

import com.google.common.collect.Lists;

/**
 * Represents an OpenAL source object.
 */
public class Source
{
    /**
     * Creates a new source for the specified sound manager.
     */
    public Source (SoundManager soundmgr)
    {
        _soundmgr = soundmgr;
        IntBuffer idbuf = BufferUtils.createIntBuffer(1);
        AL10.alGenSources(idbuf);
        _id = idbuf.get(0);
    }

    /**
     * Returns this source's OpenAL identifier.
     */
    public final int getId ()
    {
        return _id;
    }

    /**
     * Sets the position of the source.
     */
    public void setPosition (float x, float y, float z)
    {
        if (_px != x || _py != y || _pz != z) {
            AL10.alSource3f(_id, AL10.AL_POSITION, _px = x, _py = y, _pz = z);
        }
    }

    /**
     * Sets the velocity of the source.
     */
    public void setVelocity (float x, float y, float z)
    {
        if (_vx != x || _vy != y || _vz != z) {
            AL10.alSource3f(_id, AL10.AL_VELOCITY, _vx = x, _vy = y, _vz = z);
        }
    }

    /**
     * Sets the gain of the source.
     */
    public void setGain (float gain)
    {
        if (_gain != gain) {
            AL10.alSourcef(_id, AL10.AL_GAIN, _gain = gain);
        }
    }

    /**
     * Sets whether or not the position, velocity, etc., of the source are relative to the
     * listener.
     */
    public void setSourceRelative (boolean relative)
    {
        if (_sourceRelative != relative) {
            _sourceRelative = relative;
            AL10.alSourcei(_id, AL10.AL_SOURCE_RELATIVE, relative ? AL10.AL_TRUE : AL10.AL_FALSE);
        }
    }

    /**
     * Sets whether or not the source is looping.
     */
    public void setLooping (boolean looping)
    {
        if (_looping != looping) {
            _looping = looping;
            AL10.alSourcei(_id, AL10.AL_LOOPING, looping ? AL10.AL_TRUE : AL10.AL_FALSE);
        }
    }

    /**
     * Sets the minimum gain.
     */
    public void setMinGain (float gain)
    {
        if (_minGain != gain) {
            AL10.alSourcef(_id, AL10.AL_MIN_GAIN, _minGain = gain);
        }
    }

    /**
     * Sets the maximum gain.
     */
    public void setMaxGain (float gain)
    {
        if (_maxGain != gain) {
            AL10.alSourcef(_id, AL10.AL_MAX_GAIN, _maxGain = gain);
        }
    }

    /**
     * Sets the reference distance for attenuation.
     */
    public void setReferenceDistance (float distance)
    {
        if (_referenceDistance != distance) {
            AL10.alSourcef(_id, AL10.AL_REFERENCE_DISTANCE, _referenceDistance = distance);
        }
    }

    /**
     * Sets the rolloff factor for attenuation.
     */
    public void setRolloffFactor (float rolloff)
    {
        if (_rolloffFactor != rolloff) {
            AL10.alSourcef(_id, AL10.AL_ROLLOFF_FACTOR, _rolloffFactor = rolloff);
        }
    }

    /**
     * Sets the maximum distance for attenuation.
     */
    public void setMaxDistance (float distance)
    {
        if (_maxDistance != distance) {
            AL10.alSourcef(_id, AL10.AL_MAX_DISTANCE, _maxDistance = distance);
        }
    }

    /**
     * Sets the pitch multiplier.
     */
    public void setPitch (float pitch)
    {
        if (_pitch != pitch) {
            AL10.alSourcef(_id, AL10.AL_PITCH, _pitch = pitch);
        }
    }

    /**
     * Sets the direction of the source.
     */
    public void setDirection (float x, float y, float z)
    {
        if (_dx != x || _dy != y || _dz != z) {
            AL10.alSource3f(_id, AL10.AL_DIRECTION, _dx = x, _dy = y, _dz = z);
        }
    }

    /**
     * Sets the inside angle of the sound cone.
     */
    public void setConeInnerAngle (float angle)
    {
        if (_coneInnerAngle != angle) {
            AL10.alSourcef(_id, AL10.AL_CONE_INNER_ANGLE, _coneInnerAngle = angle);
        }
    }

    /**
     * Sets the outside angle of the sound cone.
     */
    public void setConeOuterAngle (float angle)
    {
        if (_coneOuterAngle != angle) {
            AL10.alSourcef(_id, AL10.AL_CONE_OUTER_ANGLE, _coneOuterAngle = angle);
        }
    }

    /**
     * Sets the gain outside of the sound cone.
     */
    public void setConeOuterGain (float gain)
    {
        if (_coneOuterGain != gain) {
            AL10.alSourcef(_id, AL10.AL_CONE_OUTER_GAIN, _coneOuterGain = gain);
        }
    }

    /**
     * Sets the source buffer.  Equivalent to unqueueing all buffers, then queuing the provided
     * buffer.  Cannot be called when the source is playing or paused.
     *
     * @param buffer the buffer to set, or <code>null</code> to clear.
     */
    public void setBuffer (Buffer buffer)
    {
        _queue.clear();
        if (buffer != null) {
            _queue.add(buffer);
        }
        AL10.alSourcei(_id, AL10.AL_BUFFER, buffer == null ? AL10.AL_NONE : buffer.getId());
    }

    /**
     * Enqueues the specified buffers.
     */
    public void queueBuffers (Buffer... buffers)
    {
        IntBuffer idbuf = BufferUtils.createIntBuffer(buffers.length);
        for (int ii = 0; ii < buffers.length; ii++) {
            Buffer buffer = buffers[ii];
            _queue.add(buffer);
            idbuf.put(ii, buffer.getId());
        }
        AL10.alSourceQueueBuffers(_id, idbuf);
    }

    /**
     * Removes the specified buffers from the queue.
     */
    public void unqueueBuffers (Buffer... buffers)
    {
        IntBuffer idbuf = BufferUtils.createIntBuffer(buffers.length);
        for (int ii = 0; ii < buffers.length; ii++) {
            Buffer buffer = buffers[ii];
            _queue.remove(buffer);
            idbuf.put(ii, buffer.getId());
        }
        AL10.alSourceUnqueueBuffers(_id, idbuf);
    }

    /**
     * Determines whether the source is playing.
     */
    public boolean isPlaying ()
    {
        return getSourceState() == AL10.AL_PLAYING;
    }

    /**
     * Determines whether the source is paused.
     */
    public boolean isPaused ()
    {
        return getSourceState() == AL10.AL_PAUSED;
    }

    /**
     * Determines whether the source is stopped.
     */
    public boolean isStopped ()
    {
        return getSourceState() == AL10.AL_STOPPED;
    }

    /**
     * Returns the state of the source: {@link AL10#AL_INITIAL}, {@link AL10#AL_PLAYING},
     * {@link AL10#AL_PAUSED}, or {@link AL10#AL_STOPPED}.
     */
    public int getSourceState ()
    {
        return AL10.alGetSourcei(_id, AL10.AL_SOURCE_STATE);
    }

    /**
     * Returns the number of buffers that have been processed.
     */
    public int getBuffersProcessed ()
    {
        return AL10.alGetSourcei(_id, AL10.AL_BUFFERS_PROCESSED);
    }

    /**
     * Returns the position offset of the source within the queued buffers, in seconds.
     */
    public float getSecOffset ()
    {
        return AL10.alGetSourcef(_id, AL11.AL_SEC_OFFSET);
    }

    /**
     * Returns the position offset of the source within the queued buffers, in samples.
     */
    public int getSampleOffset ()
    {
        return AL10.alGetSourcei(_id, AL11.AL_SAMPLE_OFFSET);
    }

    /**
     * Returns the position offset of the source within the queued buffers, in bytes.
     */
    public int getByteOffset ()
    {
        return AL10.alGetSourcei(_id, AL11.AL_BYTE_OFFSET);
    }

    /**
     * Starts playing the source.
     */
    public void play ()
    {
        AL10.alSourcePlay(_id);
    }

    /**
     * Pauses the source.
     */
    public void pause ()
    {
        AL10.alSourcePause(_id);
    }

    /**
     * Stops the source.
     */
    public void stop ()
    {
        AL10.alSourceStop(_id);
    }

    /**
     * Rewinds the source.
     */
    public void rewind ()
    {
        AL10.alSourceRewind(_id);
    }

    /**
     * Deletes this source, rendering it unusable.
     */
    public void delete ()
    {
        IntBuffer idbuf = BufferUtils.createIntBuffer(1);
        idbuf.put(_id).rewind();
        AL10.alDeleteSources(idbuf);
        _id = 0;
        _queue.clear();
    }

    @Override
    protected void finalize ()
        throws Throwable
    {
        super.finalize();
        if (_id > 0) {
            _soundmgr.sourceFinalized(_id);
        }
    }

    /** The sound manager responsible for this source. */
    protected SoundManager _soundmgr;

    /** The OpenAL identifier for this source. */
    protected int _id;

    /** The position of the source. */
    protected float _px, _py, _pz;

    /** The velocity of the source. */
    protected float _vx, _vy, _vz;

    /** The gain of the source. */
    protected float _gain = 1f;

    /** Whether or not the source's position, velocity, etc. are relative to the listener. */
    protected boolean _sourceRelative;

    /** Whether or not the source is looping. */
    protected boolean _looping;

    /** The minimum gain. */
    protected float _minGain;

    /** The maximum gain. */
    protected float _maxGain = 1f;

    /** The reference distance for attenuation. */
    protected float _referenceDistance = 1f;

    /** The attenuation rolloff factor. */
    protected float _rolloffFactor = 1f;

    /** The maximum distance for attenuation. */
    protected float _maxDistance = Float.MAX_VALUE;

    /** The pitch multiplier. */
    protected float _pitch = 1f;

    /** The direction of the source. */
    protected float _dx, _dy, _dz;

    /** The inside angle of the sound cone. */
    protected float _coneInnerAngle = 360f;

    /** The outside angle of the sound cone. */
    protected float _coneOuterAngle = 360f;

    /** The gain outside the sound cone. */
    protected float _coneOuterGain;

    /** The source's queue of buffers (storing them keeps them from being garbage-collected). */
    protected ArrayList<Buffer> _queue = Lists.newArrayList();
}
