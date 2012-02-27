//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
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

import flash.geom.Rectangle;

import com.threerings.media.tile.Tile;
import com.threerings.media.tile.TileSet;
import com.threerings.util.Map;
import com.threerings.util.Maps;

public class TileSetFrameImage
    implements ActionFrames
{
    public function TileSetFrameImage (set :TileSet, actseq :ActionSequence,
         dx :int = 0, dy :int = 0)
    {
        _set = set;
        _actseq = actseq;
        _dx = dx;
        _dy = dy;

        // compute these now to avoid pointless recomputation later
        _ocount = actseq.orients.length;
        _fcount = set.getTileCount() / _ocount;

        // create our mapping from orientation to animation sequence index
        for (var ii :int = 0; ii < _ocount; ii++) {
            _orients.put(actseq.orients[ii], ii);
        }
    }

    public function getFrames (orient :int, callback :Function) :void
    {
        var frames :Array = new Array(_fcount);
        getFramesFromTiles(frames, 0, orient, callback);
    }

    protected function getFramesFromTiles (frames :Array, idx :int, orient :int,
        callback :Function) :void
    {
        if (idx >= frames.length) {
            callback(new MultiFrameBitmap(frames, _actseq.framesPerSecond));
        } else {
            var tile :Tile = getTile(orient, idx);
            tile.notifyOnLoad(function() :void {
                frames[idx] = Bitmap(tile.getImage());
                getFramesFromTiles(frames, idx + 1, orient, callback);
            });
        }
    }

    // documentation inherited from interface
    public function getOrientationCount () :int
    {
        return _ocount;
    }

    protected function getTileIndex (orient :int, index :int) :int
    {
        return _orients.get(orient) * _fcount + index;
    }

    protected function getTile (orient :int, index :int) :Tile
    {
        return _set.getTile(getTileIndex(orient, index));
    }

    // documentation inherited from interface
    public function getXOrigin (orient :int, index :int) :int
    {
        return _actseq.origin.x;
    }

    // documentation inherited from interface
    public function getYOrigin (orient :int, index :int) :int
    {
        return _actseq.origin.y;
    }

    // documentation inherited from interface
    public function cloneColorized (zations :Array) :ActionFrames
    {
        return new TileSetFrameImage(_set.cloneWithZations(zations), _actseq);
    }

    // documentation inherited from interface
    public function cloneTranslated (dx :int, dy :int) :ActionFrames
    {
        return new TileSetFrameImage(_set, _actseq, dx, dy);
    }

    /** The tileset from which we obtain our frame images. */
    protected var _set :TileSet;

    /** The action sequence for which we're providing frame images. */
    protected var _actseq :ActionSequence;

    /** A translation to apply to the images. */
    protected var _dx :int;
    protected var _dy :int;

    /** Frame and orientation counts. */
    protected var _fcount :int;
    protected var _ocount :int;

    /** A mapping from orientation code to animation sequence index. */
    protected var _orients :Map = Maps.newMapOf(int);
}
}