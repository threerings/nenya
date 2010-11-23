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

package com.threerings.cast {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.DisplayObject;

import flash.geom.Point;
import flash.geom.Rectangle;

import com.threerings.util.ArrayUtil;
import com.threerings.util.Hashable;
import com.threerings.util.Integer;
import com.threerings.util.Map;
import com.threerings.util.Maps;
import com.threerings.util.RandomUtil;
import com.threerings.util.StringUtil;

/**
 * An implementation of the {@link ActionFrames} interface that is used
 * to lazily create composited character frames when they are requested.
 */
public class CompositedActionFrames
    implements ActionFrames, Hashable
{
    /**
     * Constructs a set of composited action frames with the supplied
     * source frames and colorization configuration. The actual component
     * frame images will not be composited until they are requested.
     */
    public function CompositedActionFrames (frameCache :Map, action :ActionSequence, sources :Array)
    {
        // sanity check
        if (sources == null || sources.length == 0) {
            var errmsg :String = "Requested to composite invalid set of source " +
                "frames! [action=" + action +
                ", sources=" + StringUtil.toString(sources) + "].";
            throw new Error(errmsg);
        }

        _frameCache = frameCache;
        _sources = sources;
        _action = action;

        // the sources must all have the same orientation count, so we
        // just use the first

        _orientCount = _sources[0].frames.getOrientationCount();
    }

    // documentation inherited from interface
    public function getOrientationCount () :int
    {
        return _orientCount;
    }

    // documentation inherited from interface
    public function getFrames (orient :int, callback :Function) :void
    {
        var key :CompositedFramesKey = new CompositedFramesKey(this, orient);
        var disp :MultiFrameBitmap = _frameCache.get(key);
        if (disp == null) {
            var listeners :Array = _pending.get(key);
            if (listeners != null) {
                listeners.push(callback);
            } else {
                listeners = [callback];
                _pending.put(key, listeners);

                createFrames(orient, function(disp :MultiFrameBitmap) :void {
                    _frameCache.put(key, disp);
                    for each (var func :Function in listeners) {
                        func(disp.clone());
                    }
                });
            }
        } else {
            callback(disp.clone());
        }
    }

    // documentation inherited from interface
    public function getXOrigin (orient :int, frameIdx :int) :int
    {
        return 0;
    }

    // documentation inherited from interface
    public function getYOrigin (orient :int, frameIdx :int) :int
    {
        return 0;
    }

    // documentation inherited from interface
    public function cloneColorized (zations :Array) :ActionFrames
    {
        throw new Error("Unsupported: CompositedActionFrames.cloneColorized()");
    }

    // documentation inherited from interface
    public function cloneTranslated (dx :int, dy :int) :ActionFrames
    {
        var tsources :Array = new Array(_sources.length);
        for (var ii :int = 0; ii < _sources.length; ii++) {
            tsources[ii] = new ComponentFrames(
                _sources[ii].ccomp, _sources[ii].frames.cloneTranslated(dx, dy));
        }
        return new CompositedActionFrames(_frameCache, _action, tsources);
    }

    /**
     * Creates our underlying multi-frame image for a particular orientation.
     */
    protected function createFrames (orient :int, callback :Function) :void
    {
        ArrayUtil.stableSort(_sources, function(cf1 :ComponentFrames, cf2 :ComponentFrames) :int {
            return (cf1.ccomp.getRenderPriority(_action.name, orient) -
                cf2.ccomp.getRenderPriority(_action.name, orient));
        });

        var idx :int = ArrayUtil.indexOf(_action.orients, orient);

        var frameCt :int = _action.tileset.getTileCounts()[idx];
        var width :int = _action.tileset.getWidths()[idx];
        var height :int = _action.tileset.getHeights()[idx];
        var frameBitmaps :Array = new Array(frameCt);
        for (var ff :int = 0; ff < frameCt; ff++) {
            frameBitmaps[ff] = new Bitmap(new BitmapData(width, height, true, 0x00000000));
        }

        var region :Rectangle = new Rectangle(0, 0, width, height);
        var pt :Point = new Point(0, 0);
        compositeFrames(frameBitmaps, _sources, 0, orient, region, pt, frameCt, callback);
    }

    protected function compositeFrames (frameBitmaps :Array, sources :Array, idx :int, orient :int,
        region :Rectangle, pt :Point, frameCt :int, callback :Function) :void
    {
        if (idx >= sources.length) {
            callback(new MultiFrameBitmap(frameBitmaps, _action.framesPerSecond));
            return;
        }

        _sources[idx].frames.getFrames(orient, function (compBitmaps :MultiFrameBitmap) :void {
            for (var ii :int = 0; ii < frameCt; ii++) {
                frameBitmaps[ii].bitmapData.draw(compBitmaps.getFrame(ii).bitmapData);
            }
            compositeFrames(frameBitmaps, sources, idx+1, orient, region, pt, frameCt, callback);
        });
    }

    public function equals (other :Object) :Boolean
    {
        return this === other;
    }

    public function hashCode () :int
    {
        return _randHashCode;
    }

    /** Used to cache our composited action frame images. */
    protected var _frameCache :Map;

    protected static var _pending :Map = Maps.newMapOf(CompositedFramesKey);

    /** The action for which we're compositing frames. */
    protected var _action :ActionSequence;

    /** The number of orientations. */
    protected var _orientCount :int;

    /** Our source components and action frames. */
    protected var _sources :Array;

    /** We keep a hash code to use to identify us nearly-uniquely from the CompositedFramesKey. */
    protected var _randHashCode :int = int(Math.random() * Integer.MAX_VALUE);
}
}