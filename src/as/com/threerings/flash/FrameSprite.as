//
// $Id$
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

import flash.display.Sprite;

import flash.events.Event;

/**
 * Convenience superclass to use for sprites that need to update every frame.
 * (One must be very careful to remove all ENTER_FRAME listeners when not needed, as they
 * will prevent an object from being garbage collected!)
 */
public class FrameSprite extends Sprite
{
    /**
     * @param renderFrameUponAdding if true, the handleFrame() method
     *        is called whenever an ADDED_TO_STAGE event is received.
     */
    public function FrameSprite (renderFrameUponAdding :Boolean = true)
    {
        _renderOnAdd = renderFrameUponAdding;

        addEventListener(Event.ADDED_TO_STAGE, handleAdded);
        addEventListener(Event.REMOVED_FROM_STAGE, handleRemoved);
    }

    /**
     * Called when we're added to the stage.
     */
    protected function handleAdded (... ignored) :void
    {
        addEventListener(Event.ENTER_FRAME, handleFrame);
        if (_renderOnAdd) {
            handleFrame(); // update immediately
        }
    }

    /**
     * Called when we're added to the stage.
     */
    protected function handleRemoved (... ignored) :void
    {
        removeEventListener(Event.ENTER_FRAME, handleFrame);
    }

    /**
     * Called to update our visual appearance prior to each frame.
     */
    protected function handleFrame (... ignored) :void
    {
        // nothing here. Override in yor subclass.
    }

    /** Should we call handleFrame() when we get ADDED_TO_STAGE? */
    protected var _renderOnAdd :Boolean;
}

}
