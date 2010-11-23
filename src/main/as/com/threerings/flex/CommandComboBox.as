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

package com.threerings.flex {

import flash.events.Event;

import mx.controls.ComboBox;
import mx.events.ListEvent;

import com.threerings.util.CommandEvent;
import com.threerings.util.Util;

/**
 * A combo box that dispatches a command (or calls a callback) when an item is selected.
 * The argument will be the 'data' value of the selected item.
 * NOTE: Unlike the other Command* controls, CommandComboBox allows a null cmd/callback
 * to be specified.
 */
public class CommandComboBox extends ComboBox
{
    /** The field of the selectedItem object used as the 'data'. If this property is null,
     * then the item is the data. */
    public var dataField :String = "data";

    /**
     * Create a command combobox.
     *
     * @param cmdOrFn either a String, which will be the CommandEvent command to dispatch,
     *        or a function, which will be called when changed.
     */
    public function CommandComboBox (cmdOrFn :* = null)
    {
        CommandButton.validateCmd(cmdOrFn);
        _cmdOrFn = cmdOrFn;

        addEventListener(ListEvent.CHANGE, handleChange, false, int.MIN_VALUE);
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
     * Set the selectedItem based on the data field.
     */
    public function set selectedData (data :Object) :void
    {
        for (var ii :int = 0; ii < dataProvider.length; ++ii) {
            if (Util.equals(data, itemToData(dataProvider[ii]))) {
                this.selectedIndex = ii;
                return;
            }
        }

        // not found, clear the selection
        this.selectedIndex = -1;
    }

    /**
     * The value that will be passed to the command or function based on dataField and the
     * current selected item.
     */
    public function get selectedData () :Object
    {
        return itemToData(this.selectedItem);
    }

    /**
     * Extract the data from the specified item. This can be expanded in the future
     * if we provide a 'dataFunction'.
     */
    protected function itemToData (item :Object) :Object
    {
        if (item != null && dataField != null) {
            try {
                return item[dataField];
            } catch (re :ReferenceError) {
                // fallback to just returning the item
            }
        }
        return item;
    }

    protected function handleChange (event :ListEvent) :void
    {
        if (_cmdOrFn != null && this.selectedIndex != -1) {
            CommandEvent.dispatch(this, _cmdOrFn, selectedData);
        }
    }

    /** The command (String) to submit, or the function (Function) to call
     * when changed,  */
    protected var _cmdOrFn :Object;
}
}
