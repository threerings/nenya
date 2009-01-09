//
// $Id$

package com.threerings.flash {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.Sprite;

import flash.events.StatusEvent;

import flash.geom.Matrix;

import flash.media.Camera;
import flash.media.Video;

import flash.text.TextField;

import com.threerings.util.MethodQueue;

// TODO: use PreferredCamera?
public class CameraSnapshotter extends Sprite
{
    /**
     * Static method to determine if the user even has a camera.
     */
    public static function hasCamera () :Boolean
    {
        var names :Array = Camera.names;
        return (names != null && names.length > 0);
    }

    /**
     * @see setCamera
     */
    public function CameraSnapshotter (cameraName :String = null)
    {
        // create a label behind the videos that only shows up if a camera doesn't work
        var tf :TextField = new TextField();
        tf.text = "Camera unavailable.";
        addChild(tf);

        setCameraName(cameraName);
    }

    override public function get width () :Number
    {
        return (_camera == null) ? 0 : _camera.width;
    }

    override public function get height () :Number
    {
        return (_camera == null) ? 0 : _camera.height;
    }

    /**
     * @param cameraName the actual name of the camera, not the index (as used to get it from
     * the Camera class).
     */
    public function setCameraName (cameraName :String = null) :void
    {
        // translate the real name of the camera into the "name" used to get it.
        var nameIdx :String = null;
        if (cameraName != null) {
            var names :Array = Camera.names;
            for (var ii :int = 0; ii < names.length; ii++) {
                if (cameraName === names[ii]) {
                    nameIdx = String(ii);
                    break;
                }
            }
        }

        setCamera(Camera.getCamera(nameIdx));
    }

    public function setCamera (camera :Camera) :void
    {
        if (_video != null) {
            if (_bitmap.parent != null) {
                removeChild(_bitmap);
            }
            if (_video.parent != null) {
                removeChild(_video);
            }
            _bitmap = null;
            _video = null;
        }

        _camera = camera;
        if (_camera == null) {
            return;
        }

        attachVideo();
    }

    public function getCameraName () :String
    {
        return (_camera == null) ? null : _camera.name;
    }

    /**
     * Just like Camera's setMode().
     * @see flash.media.Camera#setMode()
     */
    public function setMode (width :int, height :int, fps :Number, favorArea :Boolean = true) :void
    {
        clearSnapshot();
        removeChild(_video);
        _camera.setMode(width, height, fps, favorArea);

        attachVideo();
    }

    public function takeSnapshot () :void
    {
        if (_camera == null) {
            return; // throw exception?
        }

        // NOTE: we specify a matrix to scale the video, because no matter what size it really
        // is, it will usually snapshot as if it were at the smallest size. STRANGE.
        // Note that this stupid hack doesn't always work, but it mostly works, and it's better
        // than leaving everything alone, which mostly didn't work. But then again,
        // this code is specifically saying: "fuck up the scale", so if Adobe fixes their
        // bug in the future, this will cause broken behavior.
        _bitmap.bitmapData.draw(_video,
            new Matrix(_camera.width / 320, 0, 0, _camera.height / 240));
//        com.threerings.util.Log.testing("Camera stuff",
//            "camera.width", _camera.width, "camera.height", _camera.height,
//            "video.width", _video.width, "video.height", _video.height,
//            "videoWidth", _video.videoWidth, "videoHeight", _video.videoHeight,
//            "bitmap.width", _bitmap.width, "bitmap.height", _bitmap.height);

        if (_video.parent != null) {
            removeChild(_video);
            addChild(_bitmap);
        }
    }

    public function clearSnapshot () :void
    {
        if (_camera == null) {
            return;
        }
        if (_bitmap.parent != null) {
            removeChild(_bitmap);
        }
        if (_video.parent == null) {
            addChild(_video);
        }
    }

    public function getSnapshot () :BitmapData
    {
        if (_camera == null) {
            return null;
        }
        return _bitmap.bitmapData;
    }

    protected function attachVideo () :void
    {
        if (_video != null) {
            // shut down the old video
            _video.attachCamera(null);
        }

        _video = new Video(_camera.width, _camera.height);
        // the constructor args don't seem to do dick, so we set the values again...
        _video.width = _camera.width;
        _video.height = _camera.height;
        _video.attachCamera(_camera);
        addChild(_video);
        _bitmap = new Bitmap(new BitmapData(_camera.width, _camera.height, false));
    }

    protected var _camera :Camera;

    protected var _video :Video;

    protected var _bitmap :Bitmap;
}
}
