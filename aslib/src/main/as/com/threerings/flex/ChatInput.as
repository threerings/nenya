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

import flash.events.FocusEvent;

import flash.text.TextField;

import mx.controls.TextInput;

import com.threerings.text.TextFieldUtil;

/**
 * The class name of an image to use as the input prompt.
 */
[Style(name="prompt", type="Class")]

/**
 * A special TextInput for entering Chat. One of these is used in ChatControl.
 *
 * A standard TextInput has the stupid behavior of selecting all the text when it receives
 * focus. Disable that so that we can receive focus and people can type and we don't blow away
 * whatever they had before.
 */
public class ChatInput extends TextInput
{
    public function ChatInput ()
    {
        width = 147;
        showPrompt(true);
    }

    override public function stylesInitialized () :void
    {
        super.stylesInitialized();

        checkShowPrompt();
    }

    override public function styleChanged (styleProp :String) :void
    {
        super.styleChanged(styleProp);

        if (styleProp == "prompt") {
            checkShowPrompt();
        }
    }

    protected function checkShowPrompt () :void
    {
        showPrompt(focusManager == null || focusManager.getFocus() != this);
    }

    protected function showPrompt (show :Boolean) :void
    {
        setStyle("backgroundImage", (show && ("" == text)) ? getStyle("prompt") : undefined);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        // For some reason, in embeds, during certain circumstances, it's really hard to focus
        // chat. This makes that work better. The problem was *not* testable locally, only
        // on embeds in production. (I don't know why.)
        TextFieldUtil.setFocusable(TextField(textField));
    }

    override protected function focusInHandler (event :FocusEvent) :void
    {
        var oldValue :Boolean = textField.selectable;
        textField.selectable = false;
        try {
            super.focusInHandler(event);

        } finally {
            textField.selectable = oldValue;
        }
        showPrompt(false);
    }

    override protected function focusOutHandler (event :FocusEvent) :void
    {
        super.focusOutHandler(event);
        showPrompt(true);
    }
}
}
