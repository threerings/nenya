//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2011 Three Rings Design, Inc., All Rights Reserved
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

package com.threerings.flex {

import flash.display.Loader;
import flash.display.Sprite;

import mx.core.IFlexAsset;
import mx.core.IFlexDisplayObject;
import mx.core.IInvalidating;

import com.threerings.util.MultiLoader;

/**
 * An asset that can be used in Flex, that's loaded via a Loader.
 */
public class LoadedAsset extends Sprite
    implements IFlexAsset, IFlexDisplayObject
{
    /**
     * @param source a String (url) or URLRequest, or Class or a ByteArray.
     */
    public function LoadedAsset (source :Object) :void
    {
        MultiLoader.getLoaders(source, handleLoaded);
    }

    override public function get height () :Number
    {
        return (_loader == null) ? _measHeight : super.height;
    }

    override public function set height (value :Number) :void
    {
        if (_loader == null) {
            _reqHeight = value;
        } else {
            _loader.height = value;
        }
    }

    override public function get width () :Number
    {
        return (_loader == null) ? _measWidth : super.width;
    }

    override public function set width (value :Number) :void
    {
        if (_loader == null) {
            _reqWidth = value;
        } else {
            _loader.width = value;
        }
    }

    // from IFlexDisplayObject
    public function get measuredHeight () :Number
    {
        return _measHeight;
    }

    // from IFlexDisplayObject
    public function get measuredWidth () :Number
    {
        return _measWidth;
    }

    // from IFlexDisplayObject
    public function move (x :Number, y :Number) :void
    {
        this.x = x;
        this.y = y;
    }

    // from IFlexDisplayObject
    public function setActualSize (newWidth :Number, newHeight :Number) :void
    {
        width = newWidth;
        height = newHeight;
    }

    protected function handleLoaded (loader :Loader) :void
    {
        _loader = loader;
        addChild(_loader);

        // Note our measured size
        _measWidth = loader.width;
        _measHeight = loader.height;

        // set any size that was set while we were being loaded
        if (!isNaN(_reqWidth)) {
            loader.width = _reqWidth;
        }
        if (!isNaN(_reqHeight)) {
            loader.height = _reqHeight;
        }

        // if we have a parent that knows about sizes, tell it we now know our size
        if (parent is IInvalidating) {
            IInvalidating(parent).invalidateSize();
        }
    }

    protected var _loader :Loader;

    // initial / base sizes
    protected var _measWidth :Number = 0;
    protected var _measHeight :Number = 0;

    // requested sizes, during loading
    protected var _reqWidth :Number;
    protected var _reqHeight :Number;
}
}
