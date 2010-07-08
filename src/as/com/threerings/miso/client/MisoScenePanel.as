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

import flash.display.Sprite;

import flash.events.Event;

import flash.geom.Point;
import flash.geom.Rectangle;

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
import com.threerings.util.Log;
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

        addChild(_isoView);
    }

    public function onClick (event :MouseEvent) :void
    {
        var viewPt :Point = _isoView.globalToLocal(new Point(event.stageX, event.stageY));
        _isoView.centerOnPt(new Pt(viewPt.x - _isoView.width / 2 + _isoView.currentX,
            viewPt.y - _isoView.height / 2 + _isoView.currentY), false);
    }

    public function setSceneModel (model :MisoSceneModel) :void
    {
        _model = model;
        refreshScene();
    }

    public function willEnterPlace (plobj :PlaceObject) :void
    {
    }

    public function didLeavePlace (plobj :PlaceObject) :void
    {
    }

    protected function refreshScene () :void
    {
        // Clear it out...
        _isoView.removeAllScenes();

        var scene :IsoScene = new IsoScene();
        scene.layoutRenderer = new ClassFactory(PrioritizedSceneLayoutRenderer);
        var time :int = getTimer();
        var baseArr :Array = [];

        for (var si :int = -2; si < 4; si++) {
            for (var sj :int = -1; sj < 3; sj++) {
                var baseScene :IsoScene = new IsoScene();
                for (var ii :int = 10*si; ii < 10*si + 10; ii++) {
                    for (var jj :int = 10*sj; jj < 10*sj + 10; jj++) {
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
                _isoView.addScene(baseScene);
            }
        }

        var set :ObjectSet = new ObjectSet();
        _model.getObjects(new Rectangle(-100, -100, 200, 200), set);
        time = getTimer();
        for (ii = 0; ii < set.size(); ii++) {
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

    protected const DEF_WIDTH :int = 985;
    protected const DEF_HEIGHT :int = 560;
}
}
