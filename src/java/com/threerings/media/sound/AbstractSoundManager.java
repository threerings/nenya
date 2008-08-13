package com.threerings.media.sound;

import java.util.Set;

import com.google.common.collect.Sets;

import com.samskivert.util.Interval;
import com.samskivert.util.RunQueue;

import com.threerings.media.sound.SoundManager.Frob;
import com.threerings.media.sound.SoundManager.SoundType;

public abstract class AbstractSoundManager
{
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
     * Turns on or off the specified sound type.
     */
    public void setEnabled (SoundType type, boolean enabled)
    {
        if (enabled) {
            _disabledTypes.remove(type);
        } else {
            _disabledTypes.add(type);
        }
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
        return play(type, pkgPath, key, 0, SoundManager.PAN_CENTER);
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
        return play(type, pkgPath, key, delay, SoundManager.PAN_CENTER);
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
        return loop(type, pkgPath, key, SoundManager.PAN_CENTER);
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

    protected boolean shouldPlay (SoundType type)
    {
        if (type == null) {
            type = SoundManager.DEFAULT; // let the lazy kids play too
        }

        return _clipVol != 0f && isEnabled(type);
    }

    /**
     * Gets the run queue on which sound should be played. It defaults to {@link RunQueue#AWT}.
     */
    protected abstract RunQueue getSoundQueue ();

    /** Volume level for sound clips. */
    protected float _clipVol = 1f;

    /** A set of soundTypes for which sound is enabled. */
    protected Set<SoundType> _disabledTypes = Sets.newHashSet();

}
