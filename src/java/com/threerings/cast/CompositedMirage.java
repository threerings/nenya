package com.threerings.cast;

import java.util.Comparator;

import com.threerings.cast.CompositedActionFrames.ComponentFrames;
import com.threerings.media.image.Mirage;

public interface CompositedMirage
    extends Mirage, Comparator<ComponentFrames>
{
    /**
     * Returns the x offset into our image.
     */
    public int getX ();

    /**
     * Returns the y offset into our image.
     */
    public int getY ();

}
