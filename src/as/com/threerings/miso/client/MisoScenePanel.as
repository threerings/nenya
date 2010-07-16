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
import com.threerings.util.maps.WeakValueMap;
import com.threerings.media.tile.BaseTile;
import com.threerings.media.tile.Colorizer;
import com.threerings.media.tile.NoSuchTileSetError;
import com.threerings.media.tile.TileSet;
import com.threerings.media.tile.TileUtil;
import com.threerings.miso.client.MisoMetricsTransformation;
import com.threerings.miso.client.PrioritizedSceneLayoutRenderer;
import com.threerings.miso.data.MisoSceneModel;
import com.threerings.miso.data.ObjectInfo;
import com.threerings.miso.tile.FringeTile;
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
        moveBy(new Point(viewPt.x - _isoView.width / 2, viewPt.y - _isoView.height / 2));
    }

    public function moveBy (pt :Point) :void
    {
        if (_pendingMoveBy != null) {
            log.info("Already performing a move...");
            return;
        }
        _pendingMoveBy = pt;

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

    /** Computes the fringe tile for the specified coordinate. */
    public function computeFringeTile (tx :int, ty :int) :BaseTile
    {
        return _ctx.getTileManager().getFringer().getFringeTile(_model, tx, ty, _fringes,
            _masks);
    }

    protected function getBaseBlocks () :Set
    {
        var blocks :Set = Sets.newSetOf(int);

        var size :Point = _isoView.size;

        var xMove :int = (_pendingMoveBy == null ? 0 : _pendingMoveBy.x);
        var yMove :int = (_pendingMoveBy == null ? 0 : _pendingMoveBy.y);

        var minX :int = xMove;
        var maxX :int = size.x + xMove;
        var minY :int = yMove;
        var maxY :int = BOTTOM_BUFFER + size.y + yMove;

        var topLeft :Point = _isoView.localToIso(new Point(minX, minY));
        var topRight :Point = _isoView.localToIso(new Point(maxX, minY));
        var btmLeft :Point = _isoView.localToIso(new Point(minX, maxY));
        var btmRight :Point = _isoView.localToIso(new Point(maxX, maxY));

        for (var yy :int = topRight.y - 1; yy <= btmLeft.y + 1; yy++) {
            for (var xx :int = topLeft.x - 1; xx <= btmRight.x + 1; xx++) {

                // Toss out any that aren't actually in our view.
                var blkTop :Point = _isoView.isoToLocal(new Pt(xx, yy, 0));
                var blkRight :Point =
                    _isoView.isoToLocal(new Pt(xx + SceneBlock.BLOCK_SIZE, yy, 0));
                var blkLeft :Point =
                    _isoView.isoToLocal(new Pt(xx, yy + SceneBlock.BLOCK_SIZE, 0));
                var blkBtm :Point =
                    _isoView.isoToLocal(new Pt(xx + SceneBlock.BLOCK_SIZE,
                        yy + SceneBlock.BLOCK_SIZE, 0));
                if (blkTop.y < maxY &&
                    blkBtm.y > minY &&
                    blkLeft.x < maxX &&
                    blkRight.x > minX) {
                    blocks.add(SceneBlock.getBlockKey(xx, yy));
                }
            }
        }

        return blocks;
    }

    protected function createSceneBlock (blockKey :int) :SceneBlock
    {
        return new SceneBlock(blockKey, _objScene, _isoView, _metrics);
    }

    protected function refreshBaseBlockScenes () :void
    {
        _resStartTime = getTimer();

        var blocks :Set = getBaseBlocks();

        // Keep this to use in our function...
        var thisRef :MisoScenePanel = this;


        // Postpone calling complete til they're all queued up.
        _skipComplete = true;

        blocks.forEach(function(blockKey :int) :void {
            if (!_blocks.containsKey(blockKey)) {
                var sceneBlock :SceneBlock = createSceneBlock(blockKey);
                _pendingBlocks.add(sceneBlock);
                sceneBlock.resolve(_ctx, _model, thisRef, blockResolved);
            }
        });

        _skipComplete = false;


        if (_pendingBlocks.size() == 0) {
            resolutionComplete();
        }
    }

    protected function blockResolved (resolved :SceneBlock) :void
    {
        if (!_pendingBlocks.contains(resolved)) {
            log.info("Trying to resolve non-pending block???: " + resolved.getKey());
        }
        // Move that guy from pending to ready...
        _readyBlocks.add(resolved);
        _pendingBlocks.remove(resolved);
        if (_pendingBlocks.size() == 0 && !_skipComplete) {
            resolutionComplete();
        }
    }

    protected function resolutionComplete () :void
    {
        // First, we add in our new blocks...
        for each (var newBlock :SceneBlock in _readyBlocks.toArray()) {
            newBlock.render();
            _blocks.put(newBlock.getKey(), newBlock);
        }
        _readyBlocks = Sets.newSetOf(SceneBlock);

        // Force to end of list so it's on top...
        _isoView.removeScene(_objScene);
        _isoView.addScene(_objScene);

        _objScene.render();

        // Then we let the scene finally move if it's trying to...
        if (_pendingMoveBy != null) {
            _isoView.centerOnPt(new Pt(_pendingMoveBy.x + _isoView.currentX,
                _pendingMoveBy.y + _isoView.currentY), false);
            _pendingMoveBy = null;
        }

        // Now, take out any old blocks no longer in our valid blocks.
        var blocks :Set = getBaseBlocks();
        for each (var baseKey :int in _blocks.keys()) {
            if (!blocks.contains(baseKey)) {
                _blocks.remove(baseKey).release();
            }
        }

        trace("Scene Block Resolution took: " + (getTimer() - _resStartTime) + "ms");
    }

    protected function refreshScene () :void
    {
        // Clear it out...
        _isoView.removeAllScenes();

        _objScene = new IsoScene();
        _objScene.layoutRenderer = new ClassFactory(PrioritizedSceneLayoutRenderer);

        refreshBaseBlockScenes();
    }

    /**
     * Derived classes can override this method and provide a colorizer that will be used to
     * colorize the supplied scene object when rendering.
     */
    public function getColorizer (oinfo :ObjectInfo) :Colorizer
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

    protected var _objScene :IsoScene;

    protected var _blocks :Map = Maps.newMapOf(int);

    protected var _pendingMoveBy :Point;

    protected var _pendingBlocks :Set = Sets.newSetOf(SceneBlock);
    protected var _readyBlocks :Set = Sets.newSetOf(SceneBlock);

    protected var _masks :Map = Maps.newMapOf(int);
    protected var _fringes :Map = new WeakValueMap(Maps.newMapOf(FringeTile));

    protected var _resStartTime :int;

    protected var _skipComplete :Boolean

    protected const DEF_WIDTH :int = 985;
    protected const DEF_HEIGHT :int = 560;

    protected const BOTTOM_BUFFER :int = 300;
}
}

