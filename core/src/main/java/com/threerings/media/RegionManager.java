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

package com.threerings.media;

import java.util.List;

import java.awt.EventQueue;
import java.awt.Rectangle;

import com.google.common.collect.Lists;

import static com.threerings.media.Log.log;

/**
 * Manages regions (rectangles) that are invalidated in the process of ticking animations and
 * sprites and generally doing other display related business.
 */
public class RegionManager
{
    /**
     * Invalidates the specified region.
     */
    public void invalidateRegion (int x, int y, int width, int height)
    {
        if (isValidSize(width, height)) {
            addDirtyRegion(new Rectangle(x, y, width, height));
        }
    }

    /**
     * Invalidates the specified region (the supplied rectangle will be cloned as the region
     * manager fiddles with the rectangles it uses internally).
     */
    public void invalidateRegion (Rectangle rect)
    {
        if (isValidSize(rect.width, rect.height)) {
            addDirtyRegion((Rectangle)rect.clone());
        }
    }

    /**
     * Adds the supplied rectangle to the dirty regions. Control of the rectangle is given to the
     * region manager as it may choose to bend, fold or mutilate it later. If you don't want the
     * region manager messing with your rectangle, use {@link #invalidateRegion}.
     */
    public void addDirtyRegion (Rectangle rect)
    {
        // make sure we're on an AWT thread
        if (!EventQueue.isDispatchThread()) {
            log.warning("Oi! Region dirtied on non-AWT thread", "rect", rect, new Exception());
        }

        // sanity check
        if (rect == null) {
            log.warning("Attempt to dirty a null rect!?", new Exception());
            return;
        }

        // more sanity checking
        long x = rect.x, y = rect.y;
        if ((Math.abs(x) > Integer.MAX_VALUE/2) || (Math.abs(y) > Integer.MAX_VALUE/2)) {
            log.warning("Requested to dirty questionable region", "rect", rect, new Exception());
            return; // Let's not do it!
        }

        if (isValidSize(rect.width, rect.height)) {
            // Log.info("Invalidating " + StringUtil.toString(rect));
            _dirty.add(rect);
        }
    }

    /** Used to ensure our dirty regions are not invalid. */
    protected final boolean isValidSize (int width, int height)
    {
        if (width < 0 || height < 0) {
            log.warning("Attempt to add invalid dirty region?!",
                        "size", (width + "x" + height), new Exception());
            return false;

        } else if (width == 0 || height == 0) {
            // no need to complain about zero sized rectangles, just ignore them
            return false;

        } else {
            return true;
        }
    }

    /**
     * Returns true if dirty regions have been accumulated since the last call to {@link
     * #getDirtyRegions}.
     */
    public boolean haveDirtyRegions ()
    {
        return (_dirty.size() > 0);
    }

    /**
     * Returns our unmerged list of dirty regions. <em>Do not</em> modify the returned list. It's
     * just for peeking. Unlike {@link #getDirtyRegions}, this does not clear out the list of dirty
     * regions and prepare for the next frame.
     */
    public List<Rectangle> peekDirtyRegions ()
    {
        return _dirty;
    }

    /**
     * Merges all outstanding dirty regions into a single list of rectangles and returns that to
     * the caller. Internally, the list of accumulated dirty regions is cleared out and prepared
     * for the next frame.
     */
    public Rectangle[] getDirtyRegions ()
    {
        List<Rectangle> merged = Lists.newArrayList();

        for (int ii = _dirty.size() - 1; ii >= 0; ii--) {
            // pop the next rectangle from the dirty list
            Rectangle mr = _dirty.remove(ii);

            // merge in any overlapping rectangles
            for (int jj = ii - 1; jj >= 0; jj--) {
                Rectangle r = _dirty.get(jj);
                if (mr.intersects(r)) {
                    // remove the overlapping rectangle from the list
                    _dirty.remove(jj);
                    ii--;
                    // grow the merged dirty rectangle
                    mr.add(r);
                }
            }

            // add the merged rectangle to the list
            merged.add(mr);
        }

        return merged.toArray(new Rectangle[merged.size()]);
    }

    /** A list of dirty rectangles. */
    protected List<Rectangle> _dirty = Lists.newArrayList();
}
