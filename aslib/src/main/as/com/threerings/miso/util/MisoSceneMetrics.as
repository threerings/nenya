//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
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

package com.threerings.miso.util {

public class MisoSceneMetrics
{
    /** Tile dimensions and half-dimensions in the view. */
    public var tilewid :int;
    public var tilehei :int;
    public var tilehwid :int;
    public var tilehhei :int;

    /** Fine coordinate dimensions. */
    public var finehwid :int;
    public var finehhei :int;

    /** Number of fine coordinates on each axis within a tile. */
    public var finegran :int;

    /** Dimensions of our scene blocks in tile count. */
    public var blockwid :int = 4;
    public var blockhei :int = 4;

    /** The length of a tile edge in pixels. */
    public var tilelen :Number;

    /** The slope of the x- and y-axis lines. */
    public var slopeX :Number;
    public var slopeY :Number;

    /** The length between fine coordinates in pixels. */
    public var finelen :Number;

    /** The y-intercept of the x-axis line within a tile. */
    public var fineBX :Number;

    /** The slope of the x- and y-axis lines within a tile. */
    public var fineSlopeX :Number;
    public var fineSlopeY :Number;

    /**
     * Constructs scene metrics by directly specifying the desired config
     * parameters.
     *
     * @param tilewid the width in pixels of the tiles.
     * @param tilehei the height in pixels of the tiles.
     * @param finegran the number of sub-tile divisions to use for fine
     * coordinates.
     */
    public function MisoSceneMetrics (tilewid :int = DEF_TILE_WIDTH, tilehei :int = DEF_TILE_HEIGHT,
        finegran :int = DEF_FINE_GRAN)
    {
        // keep track of this stuff
        this.tilewid = tilewid;
        this.tilehei = tilehei;
        this.finegran = finegran;

        // halve the dimensions
        tilehwid = (tilewid / 2);
        tilehhei = (tilehei / 2);

        // calculate the length of a tile edge in pixels
        tilelen = Math.sqrt(
            (tilehwid * tilehwid) + (tilehhei * tilehhei));

        // calculate the slope of the x- and y-axis lines
        slopeX = Number(tilehei) / Number(tilewid);
        slopeY = -slopeX;

        // calculate the edge length separating each fine coordinate
        finelen = tilelen / finegran;

        // calculate the fine-coordinate x-axis line
        fineSlopeX = Number(tilehei) / Number(tilewid);
        fineBX = -(fineSlopeX * tilehwid);
        fineSlopeY = -fineSlopeX;

        // calculate the fine coordinate dimensions
        finehwid = int(Number(tilehwid) / Number(finegran));
        finehhei = int(Number(tilehhei) / Number(finegran));
    }

    /** Default scene view parameters. */
    protected static const DEF_TILE_WIDTH :int = 64;
    protected static const DEF_TILE_HEIGHT :int = 48;
    protected static const DEF_FINE_GRAN :int = 4;

}
}

