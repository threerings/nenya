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

import flash.display.DisplayObjectContainer;

import flash.events.Event;

import mx.controls.TextArea;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.UserMessage;
import com.threerings.crowd.util.CrowdContext;

/**
 * A very simple chat display.
 */
public class ChatDisplayBox extends TextArea
    implements ChatDisplay
{
    public function ChatDisplayBox (ctx :CrowdContext)
    {
        _ctx = ctx;
        this.editable = false;

        // TODO
        width = 400;
        height = 150;
    }

    // documentation inherited from interface ChatDisplay
    public function clear () :void
    {
        this.htmlText = "";
    }

    // documentation inherited from interface ChatDisplay
    public function displayMessage (
        msg :ChatMessage, alreadyDisplayed :Boolean) :Boolean
    {
        if (!_scrollBot) {
            _scrollBot = (verticalScrollPosition == maxVerticalScrollPosition);
        }

        // display the message
        if (msg is UserMessage) {
            this.htmlText += "<font color=\"red\">&lt;" +
                (msg as UserMessage).speaker + "&gt;</font> ";
        }
        this.htmlText += msg.message;
        return true;
    }

    override public function parentChanged (p :DisplayObjectContainer) :void
    {
        super.parentChanged(p);

        var chatdir :ChatDirector = _ctx.getChatDirector();
        if (p != null) {
            chatdir.addChatDisplay(this);
        } else {
            chatdir.removeChatDisplay(this);
        }
    }

    // documentation inherited
    override protected function updateDisplayList (uw :Number, uh :Number) :void
    {
        super.updateDisplayList(uw, uh);

        if (_scrollBot) {
            verticalScrollPosition = maxVerticalScrollPosition;
            _scrollBot = false;
        }
    }

    /** The giver of life. */
    protected var _ctx :CrowdContext;

    protected var _scrollBot :Boolean;
}
}
