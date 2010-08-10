package com.threerings.miso.client {

import as3isolib.geom.Pt;
import as3isolib.geom.transformations.IAxonometricTransformation;

import com.threerings.miso.util.MisoSceneMetrics;

public class MisoMetricsTransformation implements IAxonometricTransformation
{
    public function MisoMetricsTransformation (metrics :MisoSceneMetrics, zFact :Number)
    {
        _metrics = metrics;
        _zFact = zFact;
    }

    public function screenToSpace (screenPt :Pt) :Pt
    {
        var tpos :Pt = new Pt();

        // determine the upper-left of the quadrant that contains our point
        var zx :int = int(Math.floor(screenPt.x / _metrics.tilewid));
        var zy :int = int(Math.floor(screenPt.y / _metrics.tilehei));

        // these are the screen coordinates of the tile's top
        var ox :int = (zx * _metrics.tilewid);
        var oy :int = (zy * _metrics.tilehei);

        // these are the tile coordinates
        tpos.x = zy + zx;
        tpos.y = zy - zx;
        tpos.z = 0;

        // these are the tile coordinates
        tpos.x = zy + zx; tpos.y = zy - zx;

        // now determine which of the four tiles our point occupies
        var dx :int = screenPt.x - ox;
        var dy :int = screenPt.y - oy;

        if (Math.round(_metrics.slopeY * dx + _metrics.tilehei) <= dy) {
            tpos.x += 1;
        }

        if (Math.round(_metrics.slopeX * dx) > dy) {
            tpos.y -= 1;
        }

        return tpos;
    }

    public function spaceToScreen (spacePt :Pt) :Pt
    {
        var spos :Pt = new Pt();
        spos.x = (spacePt.x - spacePt.y) * _metrics.tilehwid;
        spos.y = (spacePt.x + spacePt.y) * _metrics.tilehhei - spacePt.z * _zFact;
        spos.z = 0;
        return spos;
    }

    protected var _metrics :MisoSceneMetrics;

    /** Handles any vertical transformations. */
    protected var _zFact :Number;
}
}
