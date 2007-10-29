//
// $Id: AnimationImpl.as 263 2007-06-14 18:20:16Z dhoover $
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2007 Three Rings Design, Inc., All Rights Reserved
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

package com.threerings.flash {

import flash.display.DisplayObject;

/**
 * An Animation that linearly transitions the alpha attribute of a given display object from
 * one value to another and optionally executes a callback function when the transition is
 * complete.
 */
public class AlphaFade extends AnimationImpl
{
    /**
     * Constructs a new AlphaFade instance. The alpha values should lie in [0, 1] and the
     * duration is measured in milliseconds.
     */
    public function AlphaFade (disp :DisplayObject, from :Number = 0, to :Number = 1,
                               duration :Number = 1, done :Function = null)
    {
        _disp = disp;
        _from = from;
        _to = to;
        _duration = duration;
        _done = done;
    }

    override public function updateAnimation (elapsed :Number) :void
    {
        if (elapsed < _duration) {
            _disp.alpha = _from + ((_to - _from) * elapsed) / _duration;
            return;
        }

        _disp.alpha = _to;
        stopAnimation();
        if (_done != null) {
            _done();
        }
    }

    protected var _disp :DisplayObject;
    protected var _from :Number;
    protected var _to :Number;
    protected var _duration :Number;
    protected var _done :Function;
    
}
}
