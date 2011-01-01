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

import com.threerings.media.tile.ObjectTile;
import com.threerings.media.tile.Tile;
import com.threerings.miso.util.MisoSceneMetrics;

public class ObjectTileIsoSprite extends TileIsoSprite
{
    public function ObjectTileIsoSprite (x :int, y :int, tileId :int, tile :Tile,
                                         priority :int, metrics :MisoSceneMetrics)
    {
        super(x, y, tileId, tile, ObjectTile(tile).getPriority() == 0 ?
                priority : ObjectTile(tile).getPriority(), metrics);
    }

    public override function layout (x :int, y :int, tile :Tile) :void
    {
        super.layout(x, y, tile);

        setSize(tile.getBaseWidth(), tile.getBaseHeight(), 1);
        moveBy(-(tile.getBaseWidth() - 1), -(tile.getBaseHeight() - 1), 0);
    }
}
}
