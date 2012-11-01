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

package com.threerings.miso.client;

import java.awt.Point;
import java.awt.Rectangle;

import com.threerings.miso.util.MisoSceneMetrics;
import com.threerings.miso.util.MisoUtil;

/**
 * Applies a TileOp to all tiles within a region.
 */
public class TileOpApplicator
{

    public TileOpApplicator (MisoSceneMetrics metrics)
    {
        _metrics = metrics;
        _tbounds = new Rectangle(0, 0, _metrics.tilewid, _metrics.tilehei);
    }

    /**
     * Applies the supplied tile operation to all tiles that intersect the supplied screen
     * rectangle.
     */
    public void applyToTiles (Rectangle region, TileOp op)
    {
        // determine which tiles intersect this region: this is going to
        // be nearly incomprehensible without some sort of diagram; i'll
        // do what i can to comment it, but you'll want to print out a
        // scene diagram (docs/miso/scene.ps) and start making notes if
        // you want to follow along

        // obtain our upper left tile
        Point tpos = MisoUtil.screenToTile(_metrics, region.x, region.y, new Point());

        // determine which quadrant of the upper left tile we occupy
        Point spos = MisoUtil.tileToScreen(_metrics, tpos.x, tpos.y, new Point());
        boolean left = (region.x - spos.x < _metrics.tilehwid);
        boolean top = (region.y - spos.y < _metrics.tilehhei);

        // set up our tile position counters
        int dx, dy;
        if (left) {
            dx = 0;
            dy = 1;
        } else {
            dx = 1;
            dy = 0;
        }

        // if we're in the top-half of the tile we need to move up a row,
        // either forward or back depending on whether we're in the left
        // or right half of the tile
        if (top) {
            if (left) {
                tpos.x -= 1;
            } else {
                tpos.y -= 1;
            }
            // we'll need to start zig-zagging the other way as well
            dx = 1 - dx;
            dy = 1 - dy;
        }

        // these will bound our loops
        int rightx = region.x + region.width, bottomy = region.y + region.height;

// Log.info("Preparing to apply [tpos=" + StringUtil.toString(tpos) +
// ", left=" + left + ", top=" + top +
// ", bounds=" + StringUtil.toString(bounds) +
// ", spos=" + StringUtil.toString(spos) +
// "].");

        // obtain the coordinates of the tile that starts the first row
        // and loop through, applying to the intersecting tiles
        MisoUtil.tileToScreen(_metrics, tpos.x, tpos.y, spos);
        while (spos.y < bottomy) {
            // set up our row counters
            int tx = tpos.x, ty = tpos.y;
            _tbounds.x = spos.x;
            _tbounds.y = spos.y;

// Log.info("Applying to row [tx=" + tx + ", ty=" + ty + "].");

            // apply to the tiles in this row
            while (_tbounds.x < rightx) {
                op.apply(tx, ty, _tbounds);

                // move one tile to the right
                tx += 1;
                ty -= 1;
                _tbounds.x += _metrics.tilewid;
            }

            // update our tile coordinates
            tpos.x += dx;
            dx = 1 - dx;
            tpos.y += dy;
            dy = 1 - dy;

            // obtain the screen coordinates of the next starting tile
            MisoUtil.tileToScreen(_metrics, tpos.x, tpos.y, spos);
        }
    }

    protected MisoSceneMetrics _metrics;

    /** Used when rendering tiles. */
    protected Rectangle _tbounds;

}
