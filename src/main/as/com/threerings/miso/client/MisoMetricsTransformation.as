//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2011 Three Rings Design, Inc., All Rights Reserved
// http://code.google.com/p/nenya/
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
