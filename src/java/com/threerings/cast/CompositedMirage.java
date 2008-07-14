package com.threerings.cast;

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
