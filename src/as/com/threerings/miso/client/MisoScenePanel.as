package com.threerings.miso.client {

import flash.utils.getTimer;

import flash.display.Sprite;

import flash.geom.Point;
import flash.geom.Rectangle;

import flash.events.MouseEvent;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.miso.client.MisoMetricsTransformation;
import com.threerings.miso.data.MisoSceneModel;
import com.threerings.miso.data.ObjectInfo;
import com.threerings.miso.util.MisoContext;
import com.threerings.miso.util.ObjectSet;
import com.threerings.miso.util.MisoSceneMetrics;

import as3isolib.display.primitive.IsoBox;
import as3isolib.geom.Pt;
import as3isolib.geom.IsoMath;
import as3isolib.display.scene.IsoGrid;
import as3isolib.display.scene.IsoScene;
import as3isolib.display.IsoView;

public class MisoScenePanel extends Sprite
    implements PlaceView
{
    public function MisoScenePanel (ctx :MisoContext, metrics :MisoSceneMetrics)
    {
        // Excitingly, we get to override this globally for as3isolib...
        IsoMath.transformationObject = new MisoMetricsTransformation(metrics,
            metrics.tilehei * 3 / 4);

        _isoView = new IsoView();
        _isoView.setSize(DEF_WIDTH, DEF_HEIGHT);

        _isoView.addEventListener(MouseEvent.CLICK, onClick);

        addChild(_isoView);
    }

    public function setSize (width :int, height :int) :void
    {
        _isoView.setSize(width, height);
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

        var time :int = getTimer();
        var baseArr :Array = [];

        for (var si :int = -10; si < 3; si++) {
            for (var sj :int = -8; sj < 4; sj++) {
                var baseScene :IsoScene = new IsoScene();
                for (var ii :int = 10*si; ii < 10*si + 10; ii++) {
                    for (var jj :int = 10*sj; jj < 10*sj + 10; jj++) {
                        baseScene.addChild(
                            new BaseTileIsoSprite(ii, jj, _model.getBaseTileId(ii, jj)));
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
            if (objInfo.priority == 0) {
                scene.addChild(new ObjectTileIsoSprite(objInfo.x, objInfo.y, objInfo.tileId));
            }
        }

        time = getTimer();
        scene.render();

        _isoView.addScene(scene);
    }

    protected var _model :MisoSceneModel;

    protected var _isoView :IsoView;

    protected const DEF_WIDTH :int = 985;
    protected const DEF_HEIGHT :int = 560;
}
}
