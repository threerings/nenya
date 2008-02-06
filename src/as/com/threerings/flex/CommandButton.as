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

import mx.controls.Button;

import com.threerings.util.CommandEvent;

/**
 * A command button simply dispatches a Controller command (with an optional
 * argument) when it is clicked.
 */
public class CommandButton extends Button
{
    /**
     * Create a command button.
     *
     * @param label the label text for the button.
     * @param cmdOrFn either a String, which will be the CommandEvent command to dispatch,
     *        or a function, which will be called when clicked.
     * @param arg the argument for the CommentEvent or the function. If the arg is an Array
     *        then those parameters are used for calling the function.
     */
    public function CommandButton (label :String = null, cmdOrFn :* = null, arg :Object = null)
    {
        if (cmdOrFn != null && !(cmdOrFn is String) && !(cmdOrFn is Function)) {
            // runtime errors suck, but this is actionscript
            throw new Error("cmdOrFn must be a String or Function.");
        }
        this.label = label;
        _cmdOrFn = cmdOrFn;
        _arg = arg;
        buttonMode = true;
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
            // if the arg is null and we're a toggle button, dispatch our state
            var arg :Object = _arg;
            if (arg == null && toggle) {
                arg = selected;
            }
            CommandEvent.dispatch(this, _cmdOrFn, arg);
        }
    }

    /** The command (String) to submit, or the function (Function) to call
     * when clicked,  */
    protected var _cmdOrFn :Object;

    /** The argument that accompanies our command. */
    protected var _arg :Object;
}
}
