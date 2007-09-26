package com.threerings.cast;

import java.util.Comparator;

import com.threerings.cast.CompositedActionFrames.ComponentFrames;
import com.threerings.media.image.Mirage;

public interface CompositedMirage
    extends Mirage
{
    /**
     * Returns the x offset into our image.
     */
    public int getXOrigin ();

    /**
     * Returns the y offset into our image.
     */
    public int getYOrigin ();

}
