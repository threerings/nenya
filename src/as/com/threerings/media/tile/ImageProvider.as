//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
// http://www.threerings.net/code/nenya/
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

package com.threerings.media.tile {

import flash.display.DisplayObject;
import flash.geom.Rectangle;

public interface ImageProvider
{
    /**
     * Returns the raw tileset image with the specified path.
     *
     * @param path the path that identifies the desired image (corresponds to the image path from
     * the tileset).
     * @param zations if non-null, colorizations to apply to the source image before returning it.
     */
    function getTileSetImage (path :String, zations :Array, callback :Function) :void;

    /**
     * Obtains the tile image with the specified path in the form of a {@link Mirage}. It should be
     * cropped from the tileset image identified by the supplied path.
     *
     * @param path the path that identifies the desired image (corresponds to the image path from
     * the tileset).
     * @param bounds if non-null, the region of the image to be returned as a mirage. If null, the
     * entire image should be returned.
     * @param zations if non-null, colorizations to apply to the image before converting it into a
     * mirage.
     */
    function getTileImage (path :String, bounds :Rectangle, zations :Array, callback :Function)
        :void;
}
}