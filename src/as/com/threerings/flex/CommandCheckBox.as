//
// $Id: CommandButton.as 408 2008-02-06 01:03:55Z ray $
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

import mx.controls.CheckBox;

import com.threerings.util.CommandEvent;

/**
 * A checkbox that dispatches a command (or calls a function) when it is toggled.
 */
public class CommandCheckBox extends CheckBox
{
    /**
     * Create a command button.
     *
     * @param label the label text for the button.
     * @param cmdOrFn either a String, which will be the CommandEvent command to dispatch,
     *        or a function, which will be called when clicked.
     * @param arg the argument for the CommentEvent or the function. If the arg is an Array
     *        then those parameters are used for calling the function.
     * Note that if arg is null, the actual argument passed will be the selected state of the
     * button.
     */
    public function CommandCheckBox (label :String = null, cmdOrFn :* = null, arg :Object = null)
    {
        CommandButton.validateCmd(cmdOrFn);
        this.label = label;
        _cmdOrFn = cmdOrFn;
        _arg = arg;
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
        CommandButton.processCommandClick(this, event, _cmdOrFn, _arg);
    }

    /** The command (String) to submit, or the function (Function) to call
     * when clicked,  */
    protected var _cmdOrFn :Object;

    /** The argument that accompanies our command. */
    protected var _arg :Object;
}
}
