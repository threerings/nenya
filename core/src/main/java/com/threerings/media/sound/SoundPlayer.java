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

package com.threerings.media.sound;

import java.util.Set;

import com.google.common.collect.Sets;

import com.samskivert.util.Interval;
import com.samskivert.util.ObserverList;
import com.samskivert.util.RunQueue;
import com.samskivert.util.ObserverList.ObserverOp;

/**
 * Loads, plays and loops sounds.
 */
public abstract class SoundPlayer
{
    /** A pan value indicating that a sound should play from the left only. */
    public static final float PAN_LEFT = -1f;

    /** A pan value indicating that a sound should play from the right only. */
    public static final float PAN_RIGHT = 1f;

    /** A pan value indicating that a sound should play from center. */
    public static final float PAN_CENTER = 0f;

    /**
     * Create instances of this for your application to differentiate
     * between different types of sounds.
     */
    public static class SoundType
    {
        /**
         * Construct a new SoundType.
         * Which should be a static variable stashed somewhere for the entire application to share.
         *
         * @param strname a short string identifier, preferably without spaces.
         */
        public SoundType (String strname)
        {
            _strname = strname;
        }

        @Override
        public String toString ()
        {
            return _strname;
        }

        protected String _strname;
    }

    /**
     * A control for sounds.
     */
    public static interface Frob
    {
        /**
         * Stop playing or looping the sound.
         * At present, the granularity of this command is limited to the buffer size of the
         * line spooler, or about 8k of data. Thus, if playing an 11khz sample, it could take
         * 8/11ths of a second for the sound to actually stop playing.
         */
        public void stop ();

        /**
         * Set the volume of the sound.
         */
        public void setVolume (float vol);

        /**
         * Get the volume of this sound.
         */
        public float getVolume ();

        /**
         * Set the pan value for the sound. Valid values are
         * -1 for left-only, 0 is centered, all the way to +1 for right-only.
         */
        public void setPan (float pan);

        /**
         * Get the pan value of this sound.
         */
        public float getPan ();
    }

    /** The default sound type. */
    public static final SoundType DEFAULT = new SoundType("default");
    /**
     * Shut the damn thing off.
     */
    public abstract void shutdown ();

    /**
     * Returns a string summarizing our volume settings and disabled sound types.
     */
    public String summarizeState ()
    {
        StringBuilder buf = new StringBuilder();
        buf.append("clipVol=").append(_clipVol);
        buf.append(", disabled=[");
        int ii = 0;
        for (SoundType soundType : _disabledTypes) {
            if (ii++ > 0) {
                buf.append(", ");
            }
            buf.append(soundType);
        }
        return buf.append("]").toString();
    }

    /**
     * Is the specified soundtype enabled?
     */
    public boolean isEnabled (SoundType type)
    {
        // by default, types are enabled..
        return (!_disabledTypes.contains(type));
    }

    /**
     * Is sound on and is the specified soundtype enabled?
     */
    public boolean shouldPlay (SoundType type)
    {
        if (type == null) {
            type = DEFAULT; // let the lazy kids play too
        }

        return _clipVol != 0f && isEnabled(type);
    }

    /**
     * Turns on or off the specified sound type.
     */
    public void setEnabled (final SoundType type, final boolean enabled)
    {
        boolean changed;
        if (enabled) {
            changed = _disabledTypes.remove(type);
        } else {
            changed = _disabledTypes.add(type);
        }
        if (changed) {
            _enabledObservers.apply(new ObserverOp<SoundEnabledObserver>() {
                public boolean apply (SoundEnabledObserver observer) {
                    observer.enabledChanged(type, enabled);
                    return true;
                }
            });
        }
    }

    public void addSoundEnabledObserver (SoundEnabledObserver listener)
    {
        _enabledObservers.add(listener);
    }

    public void removeSoundEnabledObserver (SoundEnabledObserver listener)
    {
        _enabledObservers.remove(listener);
    }

    /**
     * Sets the volume for all sound clips.
     *
     * @param vol a volume parameter between 0f and 1f, inclusive.
     */
    public void setClipVolume (float vol)
    {
        _clipVol = Math.max(0f, Math.min(1f, vol));
    }

    /**
     * Get the volume for all sound clips.
     */
    public float getClipVolume ()
    {
        return _clipVol;
    }

    /**
     * Optionally lock each of these keys prior to playing, to guarantee that it will be quickly
     * available for playing.
     */
    public abstract void lock (String pkgPath, String... keys);

    /**
     *Unlock the specified sounds so that its resources can be freed.
     */
    public abstract void unlock (String pkgPath, String... keys);

    /**
     * Play the specified sound as the specified type of sound, immediately. Note that a sound
     * need not be locked prior to playing.
     *
     * @return true if the sound actually played, or false if its sound type was disabled or if
     * sound is off altogether.
     */
    public boolean play (SoundType type, String pkgPath, String key)
    {
        return play(type, pkgPath, key, 0, PAN_CENTER);
    }

    /**
     * Play the specified sound as the specified type of sound, immediately, with the specified
     * pan value. Note that a sound need not be locked prior to playing.
     *
     * @param pan a value from -1f (all left) to +1f (all right).
     * @return true if the sound actually played, or false if its sound type was disabled or if
     * sound is off altogether.
     */
    public boolean play (SoundType type, String pkgPath, String key, float pan)
    {
        return play(type, pkgPath, key, 0, pan);
    }

    /**
     * Play the specified sound after the specified delay.
     * @param delay the delay in milliseconds.
     * @return true if the sound actually played, or false if its sound type was disabled or if
     * sound is off altogether.
     */
    public boolean play (SoundType type, String pkgPath, String key, int delay)
    {
        return play(type, pkgPath, key, delay, PAN_CENTER);
    }

    /**
     * Play the specified sound after the specified delay.
     * @param delay the delay in milliseconds.
     * @param pan a value from -1f (all left) to +1f (all right).
     * @return true if the sound actually played, or false if its sound type was disabled or if
     * sound is off altogether.
     */
    public boolean play (SoundType type, final String pkgPath, final String key, int delay,
        final float pan)
    {
        if (!shouldPlay(type)) {
            return false;
        }

        if (delay > 0) {
            new Interval(getSoundQueue()) {
                @Override
                public void expired () {
                    play(pkgPath, key, pan);
                }
            }.schedule(delay);
        } else {
            play(pkgPath, key, pan);
        }
        return true;
    }

    /**
     * Play the specified sound after the specified delay.
     * @param pan a value from -1f (all left) to +1f (all right).
     */
    protected abstract void play (String pkgPath, String key, float pan);

    /**
     * Loop the specified sound, stopping as quickly as possible when stop is called.
     */
    public Frob loop (SoundType type, String pkgPath, String key)
    {
        return loop(type, pkgPath, key, PAN_CENTER);
    }

    /**
     * Loop the specified sound, stopping as quickly as possible when stop is called.
     */
    public Frob loop (SoundType type, String pkgPath, String key, float pan)
    {
        if (!shouldPlay(type)) {
            return null;
        }
        return loop(pkgPath, key, pan);
    }

    /**
     * Loop the specified sound, stopping as quickly as possible when stop is called.
     */
    protected abstract Frob loop (String pkgPath, String key, float pan);

    /**
     * Gets the run queue on which sound should be played. It defaults to {@link RunQueue#AWT}.
     */
    protected abstract RunQueue getSoundQueue ();

    /** Volume level for sound clips. */
    protected float _clipVol = 1f;

    /** A set of soundTypes for which sound is enabled. */
    protected Set<SoundType> _disabledTypes = Sets.newHashSet();

    protected ObserverList<SoundEnabledObserver> _enabledObservers = ObserverList.newFastUnsafe();

}
