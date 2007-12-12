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

package com.threerings.flash
{

import flash.display.SimpleButton;
import flash.display.DisplayObject;

/**
 * A SimpleButton subclass that includes a disabledState.
 * DisablingButtons don't dispatch mouse events when disabled.
 */
public class DisablingButton extends SimpleButton
{
    public function DisablingButton (
       upState :DisplayObject = null,
       overState :DisplayObject = null,
       downState :DisplayObject = null,
       hitTestState :DisplayObject = null,
       disabledState :DisplayObject = null)
    {
        super(null, overState, downState, hitTestState);

        _disabledState = disabledState;
        _upState = upState;

        updateState();
    }

    /**
     * Returns the DisplayObject that will be displayed when the button is disabled.
     */
    public function get disabledState () :DisplayObject
    {
        return _disabledState;
    }

    /**
     * Sets the DisplayObject that will be displayed when the button is disabled.
     */
    public function set disabledState (newState :DisplayObject) :void
    {
        _disabledState = newState;
        updateState();
    }

    // from SimpleButton
    override public function set upState (newState :DisplayObject) :void
    {
        _upState = newState;
        updateState();
    }

    // from SimpleButton
    override public function get upState () :DisplayObject
    {
        return _upState;
    }

    // from SimpleButton
    override public function set enabled (val :Boolean) :void
    {
        super.enabled = val;

        updateState();
    }

    override public function get mouseEnabled() :Boolean
    {
        return _mouseEnabled;
    }

    override public function set mouseEnabled (enabled :Boolean) :void
    {
        _mouseEnabled = enabled;

        updateState();
    }

    protected function updateState () :void
    {
        super.upState = ((null != _disabledState && !super.enabled) ? _disabledState : _upState);

        // mouseEnabled is always false when the button is disabled
        super.mouseEnabled = (super.enabled ? _mouseEnabled : false);
    }

    protected var _disabledState :DisplayObject;
    protected var _upState :DisplayObject;
    protected var _mouseEnabled :Boolean = true;
}

}
