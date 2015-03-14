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

/**
 * Implemented by any sprite that wishes to be treated as occupying a larger-than-one-tile region
 *  by Miso's render order finding algorithms.
 */
public interface MultiTileSprite
{
    /** Returns the number of tiles the sprite occupies along the x-axis.  Note that the origin is
     * defined to be the tile with the maximal x and y coordinates. */
    public int getBaseWidth ();

    /** Returns the number of tiles the sprite occupies along the y-axis.  Note that the origin is
     * defined to be the tile with the maximal x and y coordinates. */
    public int getBaseHeight ();
}
