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

package com.threerings.cast {

import flash.display.Bitmap;
import flash.display.Sprite;

import flash.geom.Point;

import flash.events.Event;

import com.threerings.media.Tickable;
import com.threerings.util.StringUtil;

public class MultiFrameBitmap extends Sprite
    implements Tickable
{
    public function MultiFrameBitmap (frames :Array, fps :Number)
    {
        _frames = frames;
        _bitmap = new Bitmap();
        _fps = fps;
        _frameRate = fps / 1000.0;
        addChild(_bitmap);
        setFrame(0);
    }

    public function tick (tickStamp :int) :void
    {
        if (_start == 0) {
            _start = tickStamp;
        }

        var elapsedTime :int = (tickStamp - _start);
        var frameIndex :int = Math.floor(elapsedTime * _frameRate) % _frames.length;
        setFrame(frameIndex);
    }

    protected function setFrame (index :int) :void
    {
        if (_curFrameIndex != index) {
            _bitmap.bitmapData = Bitmap(_frames[index]).bitmapData;
            _curFrameIndex = index;
        }
    }

    public function getFrame (index :int) :Bitmap
    {
        return _frames[index];
    }

    public function getFrameCount () :int
    {
        return _frames.length;
    }

    public function hitTest (stageX :int, stageY :int) :Boolean
    {
        if (_curFrameIndex == -1) {
            return false;
        } else {
            if (!_bitmap.hitTestPoint(stageX, stageY, true)) {
                // Doesn't even hit the bounds...
                return false;
            }
            // Check the actual pixels...
            var pt :Point = _bitmap.globalToLocal(new Point(stageX, stageY));
            return _bitmap.bitmapData.hitTest(new Point(0, 0), 0, pt);
        }
    }

    public function clone () :MultiFrameBitmap
    {
        return new MultiFrameBitmap(_frames, _fps);
    }

    protected var _frames :Array;

    protected var _bitmap :Bitmap;

    protected var _start :int;

    protected var _frameRate :Number;

    protected var _fps :Number;

    protected var _curFrameIndex :int = -1;
}
}
