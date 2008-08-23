package com.threerings.media.sound;

import com.threerings.media.sound.SoundPlayer.SoundType;

/**
 * Exposes a {@link SoundType} being enabled or disabled in {@link SoundPlayer}.
 */
public interface SoundEnabledObserver
{
    /**
     * Called when the given type is either enabled or disabled.
     */
    void enabledChanged(SoundType type, boolean enabled);
}
