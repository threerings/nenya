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

import org.lwjgl.openal.AL10;

/**
 * Represents an instance of a sound clip which can be positioned in 3D space, gain and pitch
 * adjusted and played or looped.
 */
public class Sound
{
    /**
     * Used to await notification of the starting of a sound which may be delayed in loading.
     */
    public interface StartObserver
    {
        /**
         * Called when the specified sound has started playing. If sound is null then the sound
         * failed to play but soundStarted was called anyway to perform whatever actions were
         * waiting on the sound.
         */
        public void soundStarted (Sound sound);
    }

    /**
     * Returns a reference to the group to which the sound belongs.
     */
    public SoundGroup getGroup ()
    {
        return _group;
    }

    /**
     * Returns the buffer of audio data associated with this sound.
     */
    public ClipBuffer getBuffer ()
    {
        return _buffer;
    }

    /**
     * Sets the position of the sound.
     */
    public void setPosition (float x, float y, float z)
    {
        if (_source != null) {
            _source.setPosition(x, y, z);
        }
        _px = x;
        _py = y;
        _pz = z;
    }

    /**
     * Sets the velocity of the sound.
     */
    public void setVelocity (float x, float y, float z)
    {
        if (_source != null) {
            _source.setVelocity(x, y, z);
        }
        _vx = x;
        _vy = y;
        _vz = z;
    }

    /**
     * Sets the gain of the sound (which will be multiplied by the base gain).
     */
    public void setGain (float gain)
    {
        _gain = gain;
        updateSourceGain();
    }

    /**
     * Sets whether or not the position, velocity, etc., of the sound are relative to the
     * listener.
     */
    public void setSourceRelative (boolean relative)
    {
        if (_source != null) {
            _source.setSourceRelative(relative);
        }
        _sourceRelative = relative;
    }

    /**
     * Sets the minimum gain.
     */
    public void setMinGain (float gain)
    {
        if (_source != null) {
            _source.setMinGain(gain);
        }
        _minGain = gain;
    }

    /**
     * Sets the maximum gain.
     */
    public void setMaxGain (float gain)
    {
        if (_source != null) {
            _source.setMaxGain(gain);
        }
        _maxGain = gain;
    }

    /**
     * Sets the reference distance for attenuation.
     */
    public void setReferenceDistance (float distance)
    {
        if (_source != null) {
            _source.setReferenceDistance(distance);
        }
        _referenceDistance = distance;
    }

    /**
     * Sets the rolloff factor for attenuation.
     */
    public void setRolloffFactor (float rolloff)
    {
        if (_source != null) {
            _source.setRolloffFactor(rolloff);
        }
        _rolloffFactor = rolloff;
    }

    /**
     * Sets the maximum distance for attenuation.
     */
    public void setMaxDistance (float distance)
    {
        if (_source != null) {
            _source.setMaxDistance(distance);
        }
        _maxDistance = distance;
    }

    /**
     * Sets the pitch multiplier.
     */
    public void setPitch (float pitch)
    {
        if (_source != null) {
            _source.setPitch(pitch);
        }
        _pitch = pitch;
    }

    /**
     * Sets the direction of the sound.
     */
    public void setDirection (float x, float y, float z)
    {
        if (_source != null) {
            _source.setDirection(x, y, z);
        }
        _dx = x;
        _dy = y;
        _dz = z;
    }

    /**
     * Sets the inside angle of the sound cone.
     */
    public void setConeInnerAngle (float angle)
    {
        if (_source != null) {
            _source.setConeInnerAngle(angle);
        }
        _coneInnerAngle = angle;
    }

    /**
     * Sets the outside angle of the sound cone.
     */
    public void setConeOuterAngle (float angle)
    {
        if (_source != null) {
            _source.setConeOuterAngle(angle);
        }
        _coneOuterAngle = angle;
    }

    /**
     * Sets the gain outside of the sound cone.
     */
    public void setConeOuterGain (float gain)
    {
        if (_source != null) {
            _source.setConeOuterGain(gain);
        }
        _coneOuterGain = gain;
    }

    /**
     * Plays this sound from the beginning. While the sound is playing, an audio channel will be
     * locked and then freed when the sound completes.
     *
     * @param allowDefer if false, the sound will be played immediately or not at all. If true,
     * the sound will be queued up for loading if it is currently flushed from the cache and
     * played once loaded.
     *
     * @return true if the sound could be played and was started (or queued up to be loaded and
     * played ASAP if it was specified as deferrable) or false if the sound could not be played
     * either because it was not ready and deferral was not allowed or because too many other
     * sounds were playing concurrently.
     */
    public boolean play (boolean allowDefer)
    {
        return play(allowDefer, false, null);
    }

    /**
     * Loops this sound, starting from the beginning of the audio data. It will continue to loop
     * until {@link #pause}d or {@link #stop}ped. While the sound is playing an audio channel will
     * be locked.
     *
     * @return true if a channel could be obtained to play the sound (and the sound was thus
     * started) or false if no channels were available.
     */
    public boolean loop (boolean allowDefer)
    {
        return play(allowDefer, true, null);
    }

    /**
     * Plays this sound from the beginning, notifying the supplied observer when the audio starts.
     *
     * @param loop whether or not to loop the sampe until {@link #stop}ped.
     */
    public boolean play (StartObserver obs, boolean loop)
    {
        return play(true, loop, obs);
    }

    /**
     * Pauses this sound. A subsequent call to {@link #play} will resume the sound from the
     * precise position that it left off. While the sound is paused, its audio channel will remain
     * locked.
     */
    public void pause ()
    {
        _stateDesired = AL10.AL_PAUSED;
        if (_source != null) {
            _source.pause();
        }
    }

    /**
     * Stops this sound and rewinds to its beginning. The audio channel being used to play the
     * sound will be released.
     */
    public void stop ()
    {
        _stateDesired = AL10.AL_STOPPED;
        if (_source != null) {
            _source.stop();
        }
    }

    /**
     * Called to check if this sound is currently playing.
     */
    public boolean isPlaying ()
    {
        return _source != null && _source.isPlaying();
    }

    /**
     * Called to check if this sound wants to start playing.
     */
    public boolean isPending ()
    {
        return _stateDesired == AL10.AL_PLAYING;
    }

    protected Sound (SoundGroup group, ClipBuffer buffer)
    {
        _group = group;
        _buffer = buffer;
    }

    protected boolean play (boolean allowDefer, final boolean loop, final StartObserver obs)
    {
        // if we were unable to get our buffer, fail immediately
        if (_buffer == null) {
            if (obs != null) {
                obs.soundStarted(null);
            }
            _stateDesired = AL10.AL_INVALID;
            return false;
        }

        // if we're not ready to go...
        if (!_buffer.isPlayable()) {
            if (allowDefer) {
                // save the desired state, which may be overridden by calls to play/pause/stop
                _stateDesired = AL10.AL_PLAYING;
                _loopDesired = loop;

                // resolve the buffer and instruct it to play once it is resolved
                _buffer.resolve(new ClipBuffer.Observer() {
                    public void clipLoaded (ClipBuffer buffer) {
                        if (_stateDesired == AL10.AL_STOPPED) {
                            return;
                        }
                        play(false, _loopDesired, obs);
                        if (_stateDesired == AL10.AL_PAUSED) {
                            pause();
                        }
                    }
                    public void clipFailed (ClipBuffer buffer) {
                        // well, let's pretend like the sound started so that the observer isn't
                        // left hanging
                        if (obs != null && _stateDesired != AL10.AL_STOPPED) {
                            obs.soundStarted(Sound.this);
                            _stateDesired = AL10.AL_INVALID;
                        }
                    }
                });
                return true;
            } else {
                // sorry charlie...
                if (obs != null) {
                    obs.soundStarted(null);
                }
                _stateDesired = AL10.AL_INVALID;
                return false;
            }
        }

        // let the observer know that (as far as they're concerned), we're started
        if (obs != null) {
            obs.soundStarted(this);
        }

        // if we do not already have a source, obtain one
        if (_source == null) {
            _source = _group.acquireSource(this);
            if (_source == null) {
                _stateDesired = AL10.AL_INVALID;
                return false;
            }

            // bind our clip buffer to the source and notify it
            _source.setBuffer(_buffer.getBuffer());
            _buffer.sourceBound();

            // configure the source with our ephemera
            _source.setPosition(_px, _py, _pz);
            _source.setVelocity(_vx, _vy, _vz);
            updateSourceGain();
            _source.setSourceRelative(_sourceRelative);
            _source.setMinGain(_minGain);
            _source.setMaxGain(_maxGain);
            _source.setReferenceDistance(_referenceDistance);
            _source.setRolloffFactor(_rolloffFactor);
            _source.setMaxDistance(_maxDistance);
            _source.setPitch(_pitch);
            _source.setDirection(_dx, _dy, _dz);
            _source.setConeInnerAngle(_coneInnerAngle);
            _source.setConeOuterAngle(_coneOuterAngle);
            _source.setConeOuterGain(_coneOuterGain);
        }

        // configure whether or not we should loop
        _source.setLooping(loop);

        // and start that damned thing up!
        _source.play();

        return true;
    }

    /**
     * Updates the source gain according to our configured gain and the base gain.
     */
    protected void updateSourceGain ()
    {
        if (_source != null) {
            _source.setGain(_gain * _group.getInheritedBaseGain());
        }
    }

    /**
     * Called by the {@link SoundGroup} when it wants to reclaim our source.
     *
     * @return false if we have no source to reclaim or if we're still busy playing our sound,
     * true if we gave up our source.
     */
    protected boolean reclaim ()
    {
        if (_source != null && _source.isStopped()) {
            _source.setBuffer(null);
            _buffer.sourceUnbound();
            _source = null;
            return true;
        }
        return false;
    }

    /** The sound group with which we are associated. */
    protected SoundGroup _group;

    /** The OpenAL buffer from which we get our sound data. */
    protected ClipBuffer _buffer;

    /** The source via which we are playing our sound currently. */
    protected Source _source;

    /** The desired state of the sound (stopped, playing, paused) after resolution. */
    protected int _stateDesired = AL10.AL_INVALID;

    /** Whether or not looping is desired after resolution. */
    protected boolean _loopDesired;

    /** The position of the sound. */
    protected float _px, _py, _pz;

    /** The velocity of the sound. */
    protected float _vx, _vy, _vz;

    /** The gain of the sound. */
    protected float _gain = 1f;

    /** Whether or not the sound's position, velocity, etc. are relative to the listener. */
    protected boolean _sourceRelative;

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

    /** The direction of the sound. */
    protected float _dx, _dy, _dz;

    /** The inside angle of the sound cone. */
    protected float _coneInnerAngle = 360f;

    /** The outside angle of the sound cone. */
    protected float _coneOuterAngle = 360f;

    /** The gain outside the sound cone. */
    protected float _coneOuterGain;
}
