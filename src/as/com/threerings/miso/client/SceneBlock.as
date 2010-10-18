//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
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

import flash.geom.Point;
import flash.geom.Rectangle;

import as3isolib.display.scene.IsoScene;
import as3isolib.display.IsoView;

import com.threerings.util.Log;
import com.threerings.util.MathUtil;
import com.threerings.util.Set;
import com.threerings.util.StringUtil;

import com.threerings.media.tile.BaseTile;
import com.threerings.media.tile.Colorizer;
import com.threerings.media.tile.NoSuchTileSetError;
import com.threerings.media.tile.Tile;
import com.threerings.media.tile.TileSet;
import com.threerings.media.tile.TileUtil;
import com.threerings.miso.client.BaseTileIsoSprite;
import com.threerings.miso.client.MisoScenePanel;
import com.threerings.miso.client.ObjectTileIsoSprite;
import com.threerings.miso.data.MisoSceneModel;
import com.threerings.miso.data.ObjectInfo;
import com.threerings.miso.util.MisoContext;
import com.threerings.miso.util.MisoSceneMetrics;
import com.threerings.miso.util.ObjectSet;

public class SceneBlock
{
    private var log :Log = Log.getLog(SceneBlock);

    public static const BLOCK_SIZE :int = 4;

    public function SceneBlock (key :int, objScene :IsoScene, isoView :IsoView,
        metrics :MisoSceneMetrics)
    {
        _key = key;
        _objScene = objScene;
        _isoView = isoView;
        _metrics = metrics;
    }

    public function getKey () :int
    {
        return _key;
    }

    public static function getBlockKey (x :int, y :int) :int
    {
        return (MathUtil.floorDiv(x, BLOCK_SIZE) << 16 |
            (MathUtil.floorDiv(y, BLOCK_SIZE) & 0xFFFF));
    }

    public static function getBlockX (key :int) :int
    {
        return (key >> 16) * BLOCK_SIZE;
    }

    public static function getBlockY (key :int) :int
    {
        // We really do mean to do this crazy shift left then back right thing to get our sign
        //  back from before we encoded.
        return (((key & 0xFFFF) * BLOCK_SIZE) << 16) >> 16;
    }

    /**
     * Starts getting all the tiles for the block loaded up and ready to go.
     */
    public function resolve (ctx :MisoContext, model :MisoSceneModel, panel :MisoScenePanel,
        completeCallback :Function) :void
    {
        _completeCallback = completeCallback;

        var x :int = getBlockX(_key);
        var y :int = getBlockY(_key);

        _baseScene = new IsoScene();
        _baseScene.layoutEnabled = false;
        for (var ii :int = x; ii < x + BLOCK_SIZE; ii++) {
            for (var jj :int = y; jj < y + BLOCK_SIZE; jj++) {
                var tileId :int = model.getBaseTileId(ii, jj);
                if (tileId <= 0) {
                    var defSet :TileSet;
                    try {
                        var setId :int = model.getDefaultBaseTileSet();
                        defSet = ctx.getTileManager().getTileSet(setId);
                        tileId = TileUtil.getFQTileId(setId,
                            TileUtil.getTileHash(ii, jj) % defSet.getTileCount());
                    } catch (err :NoSuchTileSetError) {
                        // Someone else already complained...
                        continue;
                    }
                }

                var tileSet :TileSet;
                try {
                    tileSet =
                        ctx.getTileManager().getTileSet(TileUtil.getTileSetId(tileId));
                } catch (err :NoSuchTileSetError) {
                    // Someone else already complained...
                    continue;
                }

                if (tileSet == null) {
                    log.warning("TileManager returned null tilset: " +
                        TileUtil.getTileSetId(tileId));
                    continue;
                }

                createBaseSprite(tileSet, ii, jj, tileId, panel);
            }
        }

        // And now grab the appropriate objects.
        var set :ObjectSet = new ObjectSet();
        model.getObjects(new Rectangle(x, y, BLOCK_SIZE, BLOCK_SIZE), set);
        _objSprites = [];
        for (ii = 0; ii < set.size(); ii++) {
            var objInfo :ObjectInfo = set.get(ii);
            var objTileId :int = objInfo.tileId;
            var objTileSet :TileSet;
            try {
                objTileSet =
                    ctx.getTileManager().getTileSet(TileUtil.getTileSetId(objTileId));
            } catch (err :NoSuchTileSetError) {
                // Someone else already complained...
                continue;
            }

            if (objTileSet == null) {
                log.warning("TileManager returned null TileSet: " +
                    TileUtil.getTileSetId(objTileId));
                continue;
            }

            createObjSprite(objTileSet, objInfo, panel);
        }

        maybeLoaded();
    }

    /**
     * Actually adds all our tiles as sprites to the scenes.
     */
    public function render () :void
    {
        _isoView.addSceneAt(_baseScene, 0);
        _baseScene.render();
        for each (var sprite :ObjectTileIsoSprite in _objSprites) {
            _objScene.addChild(sprite);
        }
    }

    public function addToCovered (covered :Set) :void
    {
        for each (var sprite :ObjectTileIsoSprite in _objSprites) {
            for (var xx :int = sprite.x; xx < sprite.x + sprite.width; xx++) {
                for (var yy :int = sprite.y; yy < sprite.y + sprite.length; yy++) {
                    covered.add(StringUtil.toCoordsString(xx, yy));
                }
            }
        }
    }

    /**
     * Removes our tiles' sprites from the scenes.
     */
    public function release () :void
    {
        _isoView.removeScene(_baseScene);

        for each (var sprite :ObjectTileIsoSprite in _objSprites) {
            _objScene.removeChild(sprite);
        }
    }

    protected function createBaseSprite (tileSet :TileSet, x :int, y :int, tileId :int,
        panel :MisoScenePanel) :void
    {
        var fringeTile :BaseTile = panel.computeFringeTile(x, y);

        var tile :BaseTile = BaseTile(tileSet.getTile(TileUtil.getTileIndex(tileId)));

        var bx :int = getBlockX(_key);
        var by :int = getBlockY(_key);
        _passable[(y - by) * BLOCK_SIZE + (x - bx)] =
            tile.isPassable() && (fringeTile == null || fringeTile.isPassable());

        if (tile.getImage() != null && (fringeTile == null || fringeTile.getImage() != null)) {
            _baseScene.addChild(new BaseTileIsoSprite(x, y, tileId, tile, _metrics, fringeTile));
        } else {
            noteTileToLoad();

            var maybeLoaded :Function = function (loaded :Tile) :void {
                if (tile.getImage() != null &&
                    (fringeTile == null || fringeTile.getImage() != null)) {
                    _baseScene.addChild(new BaseTileIsoSprite(x, y, tileId, tile, _metrics,
                        fringeTile));
                    noteTileLoaded();
                }
            };

            if (tile.getImage() == null) {
                tile.notifyOnLoad(maybeLoaded);
            }
            if (fringeTile != null && fringeTile.getImage() == null) {
                fringeTile.notifyOnLoad(maybeLoaded);
            }
        }
    }

    protected function createObjSprite (tileSet :TileSet, objInfo :ObjectInfo,
        panel :MisoScenePanel) :void
    {
        var tile :Tile = tileSet.getTile(TileUtil.getTileIndex(objInfo.tileId),
            panel.getColorizer(objInfo));

        if (tile.getImage() != null) {
            _objSprites.push(new ObjectTileIsoSprite(objInfo.x, objInfo.y, objInfo.tileId, tile,
                objInfo.priority, _metrics));
        } else {
            noteTileToLoad();

            tile.notifyOnLoad(function (tile :Tile) :void {
                _objSprites.push(new ObjectTileIsoSprite(objInfo.x, objInfo.y, objInfo.tileId, tile,
                    objInfo.priority, _metrics));
                noteTileLoaded();
            });
        }
    }

    /**
     * Returns whether the base & fringe tiles are traversable here.
     */
    public function canTraverseBase (traverser :Object, tx :int, ty :int) :Boolean
    {
        // Only handles the base tiles since the objects cross multiple scene blocks...
        var bx :int = getBlockX(_key);
        var by :int = getBlockY(_key);

        return _passable[(ty - by) * BLOCK_SIZE + (tx - bx)];
    }

    protected function noteTileToLoad () :void
    {
        _pendingCt++;
    }

    protected function noteTileLoaded () :void
    {
        _pendingCt--;
        maybeLoaded();
    }

    protected function maybeLoaded () :void
    {
        if (_pendingCt == 0) {
            _completeCallback(this);
            _completeCallback = null;
        }
    }

    protected var _passable :Array = new Array(BLOCK_SIZE * BLOCK_SIZE);

    protected var _baseScene :IsoScene;
    protected var _objSprites :Array;

    protected var _key :int;

    protected var _completeCallback :Function;

    protected var _pendingCt :int;

    protected var _objScene :IsoScene;
    protected var _isoView :IsoView;
    protected var _metrics :MisoSceneMetrics;
}
}