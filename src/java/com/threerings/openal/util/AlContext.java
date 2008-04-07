//
// $Id$

package com.threerings.openal.util;

import com.threerings.openal.ClipProvider;
import com.threerings.openal.SoundManager;

/**
 * Provides access to the various components of the OpenAL sound system.
 */
public interface AlContext
{
    /**
     * Returns a reference to the sound manager.
     */
    public SoundManager getSoundManager ();

    /**
     * Returns a reference to the clip provider.
     */
    public ClipProvider getClipProvider ();
}
