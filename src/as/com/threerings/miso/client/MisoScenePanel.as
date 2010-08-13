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

import as3isolib.core.IsoDisplayObject;
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
import com.threerings.media.Tickable;
import com.threerings.media.tile.BaseTile;
import com.threerings.media.tile.Colorizer;
import com.threerings.media.tile.NoSuchTileSetError;
import com.threerings.media.tile.TileSet;
import com.threerings.media.tile.TileUtil;
import com.threerings.media.util.AStarPathSearch;
import com.threerings.media.util.AStarPathSearch_Stepper;
import com.threerings.media.util.LineSegmentPath;
import com.threerings.media.util.Path;
import com.threerings.media.util.Pathable;
import com.threerings.media.util.TraversalPred;
import com.threerings.miso.client.MisoMetricsTransformation;
import com.threerings.miso.client.PrioritizedSceneLayoutRenderer;
import com.threerings.miso.data.MisoSceneModel;
import com.threerings.miso.data.ObjectInfo;
import com.threerings.miso.tile.FringeTile;
import com.threerings.miso.util.MisoContext;
import com.threerings.miso.util.ObjectSet;
import com.threerings.miso.util.MisoSceneMetrics;

public class MisoScenePanel extends Sprite
    implements PlaceView, TraversalPred, Tickable
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

        _centerer = new Centerer(_isoView);

        _vbounds = new Rectangle(0, 0,
            _isoView.size.x, _isoView.size.y);

        _isoView.addEventListener(MouseEvent.CLICK, onClick);

        _objScene = new IsoScene();
        _objScene.layoutRenderer = new ClassFactory(PrioritizedSceneLayoutRenderer);

        addChild(_loading = createLoadingPanel());

        addEventListener(Event.ADDED_TO_STAGE, addedToStage);
        addEventListener(Event.REMOVED_FROM_STAGE, removedFromStage);
    }

    /**
     * Handles Event.ADDED_TO_STAGE.
     */
    protected function addedToStage (event :Event) :void
    {
        _ctx.getTicker().registerTickable(this);
    }

    /**
     * Handles Event.REMOVED_FROM_STAGE.
     */
    protected function removedFromStage (event :Event) :void
    {
        _ctx.getTicker().removeTickable(this);
    }

    public function tick (tickStamp :int) :void
    {
        _centerer.tick(tickStamp);
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
        // TODO - handle _hobject
        handleMousePressed(null, event);
    }

    public function handleMousePressed (hobject :Object, event :MouseEvent) :Boolean
    {
        var viewPt :Point = _isoView.globalToLocal(new Point(event.stageX, event.stageY));
        moveBy(new Point(viewPt.x - _isoView.width / 2, viewPt.y - _isoView.height / 2), false);
        return true;
    }

    public function moveBy (pt :Point, immediate :Boolean) :void
    {
        // No scene model yet, just do it...
        if (_model == null) {
            _centerer.moveTo(pt.x + _isoView.currentX, pt.y + _isoView.currentY, immediate);
            return;
        }

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

    protected function getBaseBlocks (buffer :Boolean) :Set
    {
        var blocks :Set = Sets.newSetOf(int);

        var size :Point = _isoView.size;

        var xMove :int = (_pendingMoveBy == null ? 0 : _pendingMoveBy.x);
        var yMove :int = (_pendingMoveBy == null ? 0 : _pendingMoveBy.y);

        var minX :int = xMove - (buffer ? size.x/2 : 0);
        var maxX :int = size.x + xMove + (buffer ? size.x/2 : 0);
        var minY :int = yMove - (buffer ? size.x/2 : 0);
        var maxY :int = BOTTOM_BUFFER + size.y + yMove + (buffer ? size.x/2 : 0);

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

        // Clear em til we're done resolving.
        _pendingPrefetchBlocks = [];

        var blocks :Set = getBaseBlocks(false);

        // Keep this to use in our function...
        var thisRef :MisoScenePanel = this;

        // Postpone calling complete til they're all queued up.
        _skipComplete = true;

        blocks.forEach(function(blockKey :int) :void {
            if (!_blocks.containsKey(blockKey)) {
                if (_prefetchedBlocks.containsKey(blockKey)) {
                    _readyBlocks.add(_prefetchedBlocks.remove(blockKey));
                } else {
                    var sceneBlock :SceneBlock = createSceneBlock(blockKey);
                    _pendingBlocks.add(sceneBlock);
                    sceneBlock.resolve(_ctx, _model, thisRef, blockResolved);
                }
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
            newBlock.addToCovered(_covered);
            _blocks.put(newBlock.getKey(), newBlock);
        }
        _readyBlocks = Sets.newSetOf(SceneBlock);

        // Force to end of list so it's on top...
        _isoView.removeScene(_objScene);
        _isoView.addScene(_objScene);

        _objScene.render();

        // Then we let the scene finally move if it's trying to...
        if (_pendingMoveBy != null) {
            _centerer.moveTo(_pendingMoveBy.x + _isoView.currentX,
                _pendingMoveBy.y + _isoView.currentY, false);
            _pendingMoveBy = null;
        }

        // Now, take out any old blocks no longer in our valid blocks.
        var blocks :Set = getBaseBlocks(true);
        for each (var baseKey :int in _blocks.keys()) {
            if (!blocks.contains(baseKey)) {
                _blocks.remove(baseKey).release();
            }
        }
        // And any prefetch blocks that are bogus can go away too.
        for each (var preKey :int in _prefetchedBlocks.keys()) {
            if (!blocks.contains(preKey)) {
                _prefetchedBlocks.remove(preKey);
            }
        }

        log.info("Scene Block Resolution took: " + (getTimer() - _resStartTime) + "ms");

        // Let's setup some prefetching...
        blocks.forEach(function(blockKey :int) :void {
            if (!_blocks.containsKey(blockKey)) {
                var sceneBlock :SceneBlock = createSceneBlock(blockKey);
                _pendingPrefetchBlocks.push(sceneBlock);
            }
        });

        maybePrefetchABlock();
    }

    protected function maybePrefetchABlock () :void
    {
        if (_pendingPrefetchBlocks.length != 0) {
            _pendingPrefetchBlocks.shift().resolve(_ctx, _model, this, prefetchBlockResolved);
        }
    }

    protected function prefetchBlockResolved (resolved :SceneBlock) :void
    {
        _prefetchedBlocks.put(resolved.getKey(), resolved);

        // If we haven't moved on to resolve a new region, let's try another block.
        if (_pendingBlocks.size() == 0) {
            DelayUtil.delayFrame(function() :void {
                maybePrefetchABlock();
            });
        }
    }

    protected function refreshScene () :void
    {
        // Clear it out...
        _isoView.removeAllScenes();

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

    // documentation inherited
    public function canTraverse (traverser :Object, tx :int, ty :int) :Boolean
    {
        var block :SceneBlock = _blocks.get(SceneBlock.getBlockKey(tx, ty));
        var baseTraversable :Boolean = (block == null) ? canTraverseUnresolved(traverser, tx, ty) :
            block.canTraverseBase(traverser, tx, ty);

        return baseTraversable && !_covered.contains(StringUtil.toCoordsString(tx, ty));
    }

    /**
     * Derived classes can control whether or not we consider unresolved tiles to be traversable
     * or not.
     */
    protected function canTraverseUnresolved (traverser :Object, tx :int, ty :int) :Boolean
    {
        return false;
    }

    /**
     * Computes a path for the specified sprite to the specified tile coordinates.
     *
     * @param loose if true, an approximate path will be returned if a complete path cannot be
     * located. This path will navigate the sprite "legally" as far as possible and then walk the
     * sprite in a straight line to its final destination. This is generally only useful if the
     * the path goes "off screen".
     */
    public function getPath (sprite :IsoDisplayObject, x :int, y :int, loose :Boolean) :Path
    {
        // sanity check
        if (sprite == null) {
            throw new Error("Can't get path for null sprite [x=" + x + ", y=" + y + ".");
        }

        // compute our longest path from the screen size
        var longestPath :int = 3 * (width / _metrics.tilewid);

        // get a reasonable tile path through the scene
        var start :int = getTimer();
        var search :AStarPathSearch = new AStarPathSearch(this, new AStarPathSearch_Stepper());
        var points :Array = search.getPath(sprite, longestPath, int(Math.round(sprite.x)),
            int(Math.round(sprite.y)), x, y, loose);

        // Replace the starting point with the Number values rather than the rounded version...
        if (points != null) {
            points[0] = new Point(sprite.x, sprite.y);
        }

        var duration :int = getTimer() - start;

        // sanity check the number of nodes searched so that we can keep an eye out for bogosity
        if (duration > 500) {
            log.warning("Considered a lot of nodes for path from " +
                StringUtil.toCoordsString(sprite.x, sprite.y) + " to " +
                StringUtil.toCoordsString(x, y) +
                " [duration=" + duration + "].");
        }

        // construct a path object to guide the sprite on its merry way
        return (points == null) ? null : LineSegmentPath.createWithList(points);
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

    /** All of the active blocks that are part of the scene. */
    protected var _blocks :Map = Maps.newMapOf(int);

    /** If we're resolving in preparation for moving, this is how much we'll move by when ready. */
    protected var _pendingMoveBy :Point;

    /** Required blocks we're working on resolving. */
    protected var _pendingBlocks :Set = Sets.newSetOf(SceneBlock);

    /** Blocks that are resolved and ready for adding to the scene. */
    protected var _readyBlocks :Set = Sets.newSetOf(SceneBlock);

    /** The queue of blocks we'd like to prefetch when we have a chance. */
    protected var _pendingPrefetchBlocks :Array = [];

    /** The blocks already prefetched and ready to go if we need them. */
    protected var _prefetchedBlocks :Map = Maps.newMapOf(int);

    protected var _masks :Map = Maps.newMapOf(int);
    protected var _fringes :Map = new WeakValueMap(Maps.newMapOf(FringeTile));

    /** What time did we start the current scene resolution. */
    protected var _resStartTime :int;

    /** If any block happens to resolve should we currently skip calling completion. */
    protected var _skipComplete :Boolean

    protected var _vbounds :Rectangle;

    protected var _covered :Set = Sets.newSetOf(String);

    protected var _centerer :Centerer;

    protected const DEF_WIDTH :int = 985;
    protected const DEF_HEIGHT :int = 560;

    protected const BOTTOM_BUFFER :int = 300;
}
}

import as3isolib.display.IsoView;
import as3isolib.geom.Pt;

import com.threerings.media.util.LineSegmentPath;
import com.threerings.media.util.Path;
import com.threerings.media.util.Pathable;
import com.threerings.media.Tickable;

import com.threerings.util.DirectionCodes;


class Centerer
    implements Pathable, Tickable
{
    public function Centerer (isoView :IsoView)
    {
        _isoView = isoView;
    }

    public function moveTo (x :int, y:int, immediate :Boolean) :void
    {
        if (immediate) {
            cancelMove();
            setLocation(x, y);
        } else {
            var path :LineSegmentPath =
                LineSegmentPath.createWithInts(_isoView.currentX, _isoView.currentY, x, y);
            path.setVelocity(SCROLL_VELOCITY);
            move(path);
        }
    }

    public function move (path :Path) :void
    {
        // if there's a previous path, let it know that it's going away
        cancelMove();

        // save off this path
        _path = path;

        // we'll initialize it on our next tick thanks to a zero path stamp
        _pathStamp = 0;
    }

    public function cancelMove () :void
    {
        if (_path != null) {
            var oldpath :Path = _path;
            _path = null;
            oldpath.wasRemoved(this);
        }
    }

    public function tick (tickStamp :int) :void
    {
        if (_path != null) {
            if (_pathStamp == 0) {
                _pathStamp = tickStamp
                _path.init(this, _pathStamp);
            }
            _path.tick(this, tickStamp);
        }
    }

    public function getX () :Number
    {
        return _isoView.currentX;
    }

    public function getY () :Number
    {
        return _isoView.currentY;
    }

    public function setLocation (x :Number, y :Number) :void
    {
        _isoView.centerOnPt(new Pt(x, y), false);
    }

    public function setOrientation (orient :int) :void
    {
    }

    public function getOrientation () :int
    {
        return DirectionCodes.NONE;
    }

    public function pathBeginning () :void
    {
    }

    public function pathCompleted (timestamp :int) :void
    {
        _path = null;
    }

    protected var _isoView :IsoView;
    protected var _path :Path;
    protected var _pathStamp :int;

    /** Our scroll path velocity. */
    protected static const SCROLL_VELOCITY :Number = 0.4;
}