package com.threerings.media.sound;

import com.threerings.resource.ResourceManager;

/**
 * Exposes JavaSoundPlayer under its old name.
 *
 * @deprecated - use {@link JavaSoundPlayer}
 */
@Deprecated
public class SoundManager extends JavaSoundPlayer
{
    public SoundManager (ResourceManager rmgr)
    {
        super(rmgr);
    }

    public SoundManager (ResourceManager rmgr, String defaultBundle, String defaultClip)
    {
        super(rmgr);
    }
}
