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

package com.threerings.flex {

import flash.events.MouseEvent;

import mx.controls.LinkButton;

import com.threerings.util.CommandEvent;

/**
 * A CommandLinkButton looks like a link but dispatches a Controller command
 * when clicked.
 */
public class CommandLinkButton extends LinkButton
{
    public function CommandLinkButton (cmd :String = null, arg :Object = null)
    {
        setCommand(cmd, arg);
    }

    /**
     * Set the command and argument to be issued when this button is pressed.
     */
    public function setCommand (cmd :String, arg :Object = null) :void
    {
        _cmdOrFn = cmd;
        _arg = arg;
    }

    /**
     * Set a callback function to call when the button is pressed.
     */
    public function setCallback (fn :Function, arg :Object = null) :void
    {
        _cmdOrFn = fn;
        _arg = arg;
    }

    override protected function clickHandler (event :MouseEvent) :void
    {
        super.clickHandler(event);

        if (enabled) {
            // stop the click event
            event.stopImmediatePropagation();

            // dispatch the command event
            CommandEvent.dispatch(this, _cmdOrFn, _arg);
        }
    }

    /** The command (String) to submit, or the function (Function) to call
     * when clicked,  */
    protected var _cmdOrFn :Object;

    /** Any argument that accompanies our command. */
    protected var _arg :Object;
}
}
