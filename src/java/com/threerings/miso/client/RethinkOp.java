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