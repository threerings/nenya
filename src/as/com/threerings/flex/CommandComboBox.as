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

import flash.events.Event;

import mx.controls.ComboBox;
import mx.events.ListEvent;

import com.threerings.util.CommandEvent;

/**
 * A combo box that dispatches a command (or calls a function) when it is toggled. NOTE: Unlike the
 * other Command* controls, CommandComboBox does not guarantee that the CommandEvent is dispatched
 * after notifying all other listeners.
 */
public class CommandComboBox extends ComboBox
{
    /**
     * The field of the selectedItem object used as the argument to the
     * command or function. If null, the entire selectedItem is passed as the argument.
     */
    public var argField :String;

    /**
     * Create a command combobox.
     *
     * @param cmdOrFn either a String, which will be the CommandEvent command to dispatch,
     *        or a function, which will be called when changed.
     */
    public function CommandComboBox (cmdOrFn :* = null, argField :String = "data")
    {
        CommandButton.validateCmd(cmdOrFn);
        _cmdOrFn = cmdOrFn;
        this.argField = argField;

        addEventListener(ListEvent.CHANGE, handleChange);
    }

    /**
     * Set the command and argument to be issued when this button is pressed.
     */
    public function setCommand (cmd :String) :void
    {
        _cmdOrFn = cmd;
    }

    /**
     * Set a callback function to call when the button is pressed.
     */
    public function setCallback (fn :Function) :void
    {
        _cmdOrFn = fn;
    }

    /**
     * Handy function to lookup an option based on argField and set it as the selected item.
     * This probably only works for Array dataProviders.
     */
    public function setSelectedValue (value :Object) :void
    {
        for (var ii :int = 0; ii < dataProvider.length; ++ii) {
            var v :Object = (this.argField != null) ?
                dataProvider[ii][this.argField] : dataProvider[ii];
            if (v == value) {
                this.selectedIndex = ii;
                return;
            }
        }

        // Assign to the prompt
        this.selectedIndex = -1;
    }

    /**
     * The value that will be passed to the command or function based on argField and the
     * current selected item.
     */
    public function getSelectedValue () :Object
    {
        return (this.argField != null) ? this.selectedItem[this.argField] : this.selectedItem;
    }

    protected function handleChange (event :ListEvent) :void
    {
        if (this.selectedIndex != -1) {
            CommandEvent.dispatch(this, _cmdOrFn, getSelectedValue());
        }
    }

    /** The command (String) to submit, or the function (Function) to call
     * when changed,  */
    protected var _cmdOrFn :Object;
}
}
