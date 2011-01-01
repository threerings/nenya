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

import flash.display.Bitmap;
import flash.display.DisplayObject;

import flash.geom.Point;

import as3isolib.core.IsoDisplayObject;
import as3isolib.display.IsoSprite;
import as3isolib.display.primitive.IsoBox;
import as3isolib.graphics.SolidColorFill;

import com.threerings.media.tile.Tile;
import com.threerings.media.tile.TileSet;
import com.threerings.media.tile.TileUtil;
import com.threerings.util.Log;
import com.threerings.miso.util.MisoSceneMetrics;

public class TileIsoSprite extends IsoSprite
    implements PriorityIsoDisplayObject
{
    private static var log :Log = Log.getLog(TileIsoSprite);

    public function TileIsoSprite (x :int, y :int, tileId :int, tile :Tile, priority :int,
        metrics :MisoSceneMetrics, fringe :Tile = null)
    {
        _tileId = tileId;
        _metrics = metrics;
        _priority = priority;
        _fringe = fringe;

        layout(x, y, tile);

        setSize(tile.getBaseWidth(), tile.getBaseHeight(), 1);

        if (tile.getImage() == null) {
            tile.notifyOnLoad(function() :void {
                gotTileImage(tile);
            });
        } else {
            gotTileImage(tile);
        }
    }

    public function layout (x :int, y :int, tile :Tile) :void
    {
        moveTo(x, y, 0);
    }

    protected function gotTileImage (tile :Tile) :void
    {
        var image :DisplayObject = tile.getImage();
        // as3isolib uses top instead of bottom.
        image.x = -tile.getOriginX() + _metrics.tilewid *
            ((tile.getBaseWidth() - tile.getBaseHeight())/2);
        image.y = -tile.getOriginY() + _metrics.tilehei *
            ((tile.getBaseWidth() + tile.getBaseHeight())/2);

        if (_fringe == null) {
            sprites = [image];
        } else {
            var fringeImg :DisplayObject = _fringe.getImage();
            // as3isolib uses top instead of bottom.
            fringeImg.x = -_fringe.getOriginX() + _metrics.tilewid *
                ((_fringe.getBaseWidth() - _fringe.getBaseHeight())/2);
            fringeImg.y = -_fringe.getOriginY() + _metrics.tilehei *
                ((_fringe.getBaseWidth() + _fringe.getBaseHeight())/2);

            sprites = [image, fringeImg];
        }
        render();
    }

    public function getPriority () :int
    {
        return _priority;
    }

    public function hitTest (stageX :int, stageY :int) :Boolean
    {
        if (sprites == null || sprites.length == 0) {
            return false;
        }

        if (sprites[0] is Bitmap) {
            if (!sprites[0].hitTestPoint(stageX, stageY, true)) {
                // Doesn't even hit the bounds...
                return false;
            }
            // Check the actual pixels...
            var pt :Point = sprites[0].globalToLocal(new Point(stageX, stageY));
            return Bitmap(sprites[0]).bitmapData.hitTest(new Point(0, 0), 0, pt);
        } else {
            return sprites[0].hitTestPoint(stageX, stageY, true);
        }
    }

    public function toString () :String
    {
        return x + ", " + y + ": " + _tileId;
    }

    protected var _tileId :int;

    protected var _priority :int;

    protected var _fringe :Tile;

    protected var _metrics :MisoSceneMetrics;
}
}