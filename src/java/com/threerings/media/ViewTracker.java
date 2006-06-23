//
// $Id: ViewTracker.java 4191 2006-06-13 22:42:20Z ray $

package com.threerings.media;

/**
 * An interface used by entities that wish to respond to the scrolling of a
 * {@link VirtualMediaPanel}.
 *
 * @see VirtualMediaPanel#addViewTracker
 */
public interface ViewTracker
{
    /**
     * Called by a {@link VirtualMediaPanel} when it scrolls.
     */
    public void viewLocationDidChange (int dx, int dy);
}
