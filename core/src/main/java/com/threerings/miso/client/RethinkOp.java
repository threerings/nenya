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

import java.util.Set;

import java.awt.Point;
import java.awt.Rectangle;

import com.google.common.collect.Sets;

import com.threerings.media.util.MathUtil;

import com.threerings.miso.util.MisoSceneMetrics;

/**
 * Constructs the set of blocks that intersect the op bounds.
 */
public class RethinkOp
    implements TileOp
{
    public Set<Point> blocks = Sets.newHashSet();

    public RethinkOp (MisoSceneMetrics metrics)
    {
        _metrics = metrics;
    }

    public void apply (int tx, int ty, Rectangle tbounds)
    {
        _key.x = MathUtil.floorDiv(tx, _metrics.blockwid) * _metrics.blockwid;
        _key.y = MathUtil.floorDiv(ty, _metrics.blockhei) * _metrics.blockhei;
        if (!blocks.contains(_key)) {
            blocks.add(new Point(_key.x, _key.y));
        }
    }

    protected Point _key = new Point();
    protected MisoSceneMetrics _metrics;
}