//
// $Id$

package com.threerings.flash {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.Sprite;

import flash.media.Camera;
import flash.media.Video;

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

        _video = new Video(_camera.width, _camera.height);
        _bitmap = new Bitmap(new BitmapData(_camera.width, _camera.height, false));
        _video.attachCamera(_camera);
        addChild(_video);
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
        _camera.setMode(width, height, fps, favorArea);
        _video.width = _camera.width;
        _video.height = _camera.height;
    }

    public function takeSnapshot () :void
    {
        if (_camera == null) {
            return; // throw exception?
        }

        _bitmap.bitmapData.draw(_video);
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
        if (_video.parent == null) {
            removeChild(_bitmap);
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

    protected var _camera :Camera;

    protected var _video :Video;

    protected var _bitmap :Bitmap;
}
}
