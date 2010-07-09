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

package com.threerings.miso.client {

import flash.utils.getTimer;

import flash.display.DisplayObject;
import flash.display.Sprite;

import flash.events.Event;

import flash.geom.Point;
import flash.geom.Rectangle;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

import flash.events.MouseEvent;

import mx.core.ClassFactory;

import as3isolib.display.primitive.IsoBox;
import as3isolib.geom.Pt;
import as3isolib.geom.IsoMath;
import as3isolib.display.scene.IsoGrid;
import as3isolib.display.scene.IsoScene;
import as3isolib.display.IsoView;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.util.DelayUtil;
import com.threerings.util.Log;
import com.threerings.util.Map;
import com.threerings.util.Maps;
import com.threerings.util.MathUtil;
import com.threerings.util.Set;
import com.threerings.util.Sets;
import com.threerings.util.StringUtil;
import com.threerings.media.tile.Colorizer;
import com.threerings.media.tile.NoSuchTileSetError;
import com.threerings.media.tile.TileSet;
import com.threerings.media.tile.TileUtil;
import com.threerings.miso.client.MisoMetricsTransformation;
import com.threerings.miso.client.PrioritizedSceneLayoutRenderer;
import com.threerings.miso.data.MisoSceneModel;
import com.threerings.miso.data.ObjectInfo;
import com.threerings.miso.util.MisoContext;
import com.threerings.miso.util.ObjectSet;
import com.threerings.miso.util.MisoSceneMetrics;

public class MisoScenePanel extends Sprite
    implements PlaceView
{
    private var log :Log = Log.getLog(MisoScenePanel);

    public function MisoScenePanel (ctx :MisoContext, metrics :MisoSceneMetrics)
    {
        _ctx = ctx;
        // Excitingly, we get to override this globally for as3isolib...
        IsoMath.transformationObject = new MisoMetricsTransformation(metrics,
            metrics.tilehei * 3 / 4);

        _metrics = metrics;

        _isoView = new IsoView();
        _isoView.setSize(DEF_WIDTH, DEF_HEIGHT);

        _isoView.addEventListener(MouseEvent.CLICK, onClick);

        addChild(_loading = createLoadingPanel());
    }

    /**
     * Creates whatever we want to show while we're waiting to load our exciting scene tile data.
     */
    protected function createLoadingPanel () :DisplayObject
    {
        // By default we just stick up a basic label for this...
        var loadingText :TextField = new TextField();
        loadingText.textColor = 0xFFFFFF;
        var loadingFmt :TextFormat = new TextFormat();
        loadingFmt.size = 24;
        loadingText.defaultTextFormat = loadingFmt;
        loadingText.autoSize = TextFieldAutoSize.LEFT;

        _loadingProgressFunc = function (progress :Number) :void {
            loadingText.text = "Loading... " + int(progress*100) + "%";
        };
        _loadingProgressFunc(0.0);
        return loadingText;
    }

    protected function loadingProgress (progress :Number) :void
    {
        if (_loadingProgressFunc != null) {
            _loadingProgressFunc(progress);
        }
    }

    public function onClick (event :MouseEvent) :void
    {
        var viewPt :Point = _isoView.globalToLocal(new Point(event.stageX, event.stageY));
        _isoView.centerOnPt(new Pt(viewPt.x - _isoView.width / 2 + _isoView.currentX,
            viewPt.y - _isoView.height / 2 + _isoView.currentY), false);
        refreshBaseBlockScenes();
    }

    public function setSceneModel (model :MisoSceneModel) :void
    {
        _model = model;
        _ctx.getTileManager().ensureLoaded(_model.getAllTilesets(), function() :void {
                DelayUtil.delayFrame(function() :void {
                    refreshScene();
                    removeChild(_loading);
                    addChild(_isoView);
                });
            }, loadingProgress);
    }

    public function willEnterPlace (plobj :PlaceObject) :void
    {
    }

    public function didLeavePlace (plobj :PlaceObject) :void
    {
    }

    protected function getBaseBlocks () :Set
    {
        var blocks :Set = Sets.newSetOf(int);

        var size :Point = _isoView.size;
        var topLeft :Point = _isoView.localToIso(new Point(0, 0));
        var topRight :Point = _isoView.localToIso(new Point(size.x, 0));
        var btmLeft :Point = _isoView.localToIso(new Point(0, size.y));
        var btmRight :Point = _isoView.localToIso(new Point(size.x, size.y));

        for (var yy :int = topRight.y - 1; yy <= btmLeft.y + 1; yy++) {
            for (var xx :int = topLeft.x - 1; xx <= btmRight.x + 1; xx++) {

                // Toss out any that aren't actually in our view.
                var blkTop :Point = _isoView.isoToLocal(new Pt(xx, yy, 0));
                var blkRight :Point = _isoView.isoToLocal(new Pt(xx + BLOCK_SIZE, yy, 0));
                var blkLeft :Point = _isoView.isoToLocal(new Pt(xx, yy + BLOCK_SIZE, 0));
                var blkBtm :Point =
                    _isoView.isoToLocal(new Pt(xx + BLOCK_SIZE, yy + BLOCK_SIZE, 0));
                if (blkTop.y < size.y &&
                    blkBtm.y > 0 &&
                    blkLeft.x < size.x &&
                    blkRight.x > 0) {
                    blocks.add(getBlockKey(xx, yy));
                }
            }
        }

        return blocks;
    }

    protected function getBlockKey (x :int, y :int) :int
    {
        return (MathUtil.floorDiv(x, BLOCK_SIZE) << 16 |
            (MathUtil.floorDiv(y, BLOCK_SIZE) & 0xFFFF));
    }

    protected function getBlockX (key :int) :int
    {
        return (key >> 16) * BLOCK_SIZE;
    }

    protected function getBlockY (key :int) :int
    {
        // We really do mean to do this crazy shift left then back right thing to get our sign
        //  back from before we encoded.
        return (((key & 0xFFFF) * BLOCK_SIZE) << 16) >> 16;
    }

    protected function refreshBaseBlockScenes () :void
    {
        var blocks :Set = getBaseBlocks();

        // Take out any scenes no longer in our valid blocks.
        for each (var baseKey :int in _baseScenes.keys()) {
            if (!blocks.contains(baseKey)) {
                // It's not valid anymore, so toss it.
                _isoView.removeScene(_baseScenes.remove(baseKey));
            }
        }

        blocks.forEach(function(blockKey :int) :void {
            if (!_baseScenes.containsKey(blockKey)) {
                // Not already in there, let's create and add it.
                var x :int = getBlockX(blockKey);
                var y :int = getBlockY(blockKey);

                var baseScene :IsoScene = new IsoScene();
                for (var ii :int = x; ii < x + BLOCK_SIZE; ii++) {
                    for (var jj :int = y; jj < y + BLOCK_SIZE; jj++) {
                        var tileId :int = _model.getBaseTileId(ii, jj);
                        if (tileId <= 0) {
                            var defSet :TileSet;
                            try {
                                var setId :int = _model.getDefaultBaseTileSet();
                                defSet = _ctx.getTileManager().getTileSet(setId);
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
                                _ctx.getTileManager().getTileSet(TileUtil.getTileSetId(tileId));
                        } catch (err :NoSuchTileSetError) {
                            // Someone else already complained...
                            continue;
                        }

                        if (tileSet == null) {
                            log.warning("TileManager returned null tilset: " +
                                TileUtil.getTileSetId(tileId));
                            continue;
                        }

                        baseScene.addChild(
                            new BaseTileIsoSprite(ii, jj, tileId,
                                tileSet.getTile(TileUtil.getTileIndex(tileId)), _metrics));
                    }
                }
                baseScene.render();
                _isoView.addSceneAt(baseScene, 0);
                _baseScenes.put(blockKey, baseScene);
            }
        });
    }

    protected function refreshScene () :void
    {
        // Clear it out...
        _isoView.removeAllScenes();

        var scene :IsoScene = new IsoScene();
        scene.layoutRenderer = new ClassFactory(PrioritizedSceneLayoutRenderer);
        var time :int = getTimer();

        refreshBaseBlockScenes();

        var set :ObjectSet = new ObjectSet();
        _model.getObjects(new Rectangle(-100, -100, 200, 200), set);
        time = getTimer();
        for (var ii :int = 0; ii < set.size(); ii++) {
            var objInfo :ObjectInfo = set.get(ii);
            var objTileId :int = objInfo.tileId;
            var objTileSet :TileSet;
            try {
                objTileSet =
                    _ctx.getTileManager().getTileSet(TileUtil.getTileSetId(objTileId));
            } catch (err :NoSuchTileSetError) {
                // Someone else already complained...
                continue;
            }

            if (objTileSet == null) {
                log.warning("TileManager returned null TileSet: " +
                    TileUtil.getTileSetId(objTileId));
                continue;
            }

            scene.addChild(
                new ObjectTileIsoSprite(objInfo.x, objInfo.y, objTileId,
                    objTileSet.getTile(TileUtil.getTileIndex(objTileId), getColorizer(objInfo)),
                    objInfo.priority, _metrics));
        }

        time = getTimer();
        scene.render();

        _isoView.addScene(scene);
    }

    /**
     * Derived classes can override this method and provide a colorizer that will be used to
     * colorize the supplied scene object when rendering.
     */
    protected function getColorizer (oinfo :ObjectInfo) :Colorizer
    {
        return null;
    }

    protected var _model :MisoSceneModel;

    protected var _isoView :IsoView;

    protected var _ctx :MisoContext;

    protected var _metrics :MisoSceneMetrics;

    /** What we display while we're loading up our tilesets. */
    protected var _loading :DisplayObject;

    /** If we should do something when we hear about progress updates, this is it. */
    protected var _loadingProgressFunc :Function;

    protected var _baseScenes :Map = Maps.newMapOf(int);

    protected const DEF_WIDTH :int = 985;
    protected const DEF_HEIGHT :int = 560;

    protected const BLOCK_SIZE :int = 4;
}
}
