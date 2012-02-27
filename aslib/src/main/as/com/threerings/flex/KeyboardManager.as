//
// $Id$
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

package com.threerings.flex {

import flash.display.Stage;

import flash.events.KeyboardEvent;
import flash.events.MouseEvent;

import flash.ui.Keyboard;

import flash.utils.Dictionary;

import mx.core.Application;
import mx.controls.Button;


public class KeyboardManager
{
    public static function setShortcut (trigger :*, keyCode :uint, ... modifiers) :void
    {
        _triggers[trigger] = [ keyCode, modifiers ];
        ensureListening();
    }

    public static function clearShortcut (trigger :*) :void
    {
        delete _triggers[trigger];
    }

    protected static function ensureListening () :void
    {
        var stage :Stage = Application(Application.application).stage;
        stage.addEventListener(KeyboardEvent.KEY_DOWN, handleKeyDown);
    }

    protected static function handleKeyDown (event :KeyboardEvent) :void
    {
        for (var trigger :* in _triggers) {
            var info :Array = _triggers[trigger] as Array;
            var keyCode :uint = info[0];
            var modifiers :Array = info[1] as Array;
            if (event.keyCode == keyCode &&
//                    (event.altKey == (-1 != modifierCodes.indexOf(Keyboard.ALTERNATE))) &&
                    (event.ctrlKey == (-1 != modifiers.indexOf(Keyboard.CONTROL))) &&
                    (event.shiftKey == (-1 != modifiers.indexOf(Keyboard.SHIFT)))) {
                wasTriggered(trigger);
            }
        }
    }

    protected static function wasTriggered (trigger :*) :void
    {
        if (trigger is Button) {
            var button :Button = Button(trigger);
            if (button.stage != null && button.enabled) {
                button.dispatchEvent(new MouseEvent(MouseEvent.CLICK));
            }
        }
    }

    protected static const _triggers :Dictionary = new Dictionary(true);
}
}
