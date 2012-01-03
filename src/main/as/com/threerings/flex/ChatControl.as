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

import flash.display.DisplayObjectContainer;

import flash.events.Event;
import flash.events.FocusEvent;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;

import flash.ui.Keyboard;

import mx.containers.HBox;

import mx.core.Application;

import mx.controls.TextInput;

import mx.events.FlexEvent;

import com.threerings.util.Arrays;
import com.threerings.util.DelayUtil;
import com.threerings.util.StringUtil;

import com.threerings.flex.CommandButton;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.crowd.util.CrowdContext;

/**
 * The chat control widget.
 */
public class ChatControl extends HBox
{
    /**
     * Request focus for the oldest ChatControl.
     */
    public static function grabFocus () :void
    {
        if (_controls.length > 0) {
            (_controls[0] as ChatControl).setFocus();
        }
    }

    public function ChatControl (
        ctx :CrowdContext, sendButtonLabel :String = "send",
        height :Number = NaN, controlHeight :Number = NaN)
    {
        _ctx = ctx;
        _chatDtr = _ctx.getChatDirector();

        this.height = height;
        styleName = "chatControl";

        addChild(_txt = new ChatInput());
        _txt.addEventListener(FlexEvent.ENTER, sendChat, false, 0, true);
        _txt.addEventListener(KeyboardEvent.KEY_UP, handleKeyUp);

        _but = new CommandButton(sendButtonLabel, sendChat);
        addChild(_but);

        if (!isNaN(controlHeight)) {
            _txt.height = controlHeight;
            _but.height = controlHeight;
        }

        addEventListener(Event.ADDED_TO_STAGE, handleAddRemove);
        addEventListener(Event.REMOVED_FROM_STAGE, handleAddRemove);
    }

    /**
     * Provides access to the text field we use to accept chat.
     */
    public function get chatInput () :ChatInput
    {
        return _txt;
    }

    /**
     * Provides access to the send button.
     */
    public function get sendButton () :CommandButton
    {
        return _but;
    }

    override public function set enabled (en :Boolean) :void
    {
        super.enabled = en;
        if (_txt != null) {
            _txt.enabled = en;
            _but.enabled = en;
        }
    }

    /**
     * Request focus to this chat control.
     */
    override public function setFocus () :void
    {
        _txt.setFocus();
    }

    /**
     * Configures the chat director to which we should send our chat. Pass null to restore our
     * default chat director.
     */
    public function setChatDirector (chatDtr :ChatDirector) :void
    {
        _chatDtr = (chatDtr == null) ? _ctx.getChatDirector() : chatDtr;
    }

    /**
     * Configures the background color of the text entry area.
     */
    public function setChatColor (color :uint) :void
    {
        _txt.setStyle("backgroundColor", color);
    }

    /**
     * Handles FlexEvent.ENTER and the action from the send button.
     */
    protected function sendChat (... ignored) :void
    {
        var message :String = StringUtil.trim(_txt.text);
        if ("" == message) {
            return;
        }

        var result :String = _chatDtr.requestChat(null, message, true);
        if (result != ChatCodes.SUCCESS) {
            _chatDtr.displayFeedback(null, result);
            return;
        }

        // If there was no error, clear the entry area in prep for the next entry event
        DelayUtil.delayFrame(function () : void {
            // WORKAROUND
            // Originally we cleared the text immediately, but with flex 3.2 this broke
            // for *some* people. Weird! We're called from the event dispatcher for the
            // enter key, so it's possible that the default action is booching it?
            // In any case, this could possibly be removed in the future by the ambitious.
            // Note also: Flex's built-in callLater() doesn't work, but DelayUtil does. WTF?!?
            _txt.text = "";
        });
        _histidx = -1;
    }

    protected function scrollHistory (next :Boolean) :void
    {
        var size :int = _chatDtr.getCommandHistorySize();
        if ((_histidx == -1) || (_histidx == size)) {
            _curLine = _txt.text;
            _histidx = size;
        }

        _histidx = (next) ? Math.min(_histidx + 1, size)
                          : Math.max(_histidx - 1, 0);
        var text :String = (_histidx == size) ? _curLine : _chatDtr.getCommandHistory(_histidx);
        _txt.text = text;
        _txt.setSelection(text.length, text.length);
    }

    /**
     * Handles Event.ADDED_TO_STAGE and Event.REMOVED_FROM_STAGE.
     */
    protected function handleAddRemove (event :Event) :void
    {
        if (event.type == Event.ADDED_TO_STAGE) {
            // set up any already-configured text
            _txt.text = _curLine;
            _histidx = -1;

            // request focus
            callLater(_txt.setFocus);
            _controls.push(this);

        } else {
            _curLine = _txt.text;
            Arrays.removeAll(_controls, this);
        }
    }

    protected function handleKeyUp (event :KeyboardEvent) :void
    {
        switch (event.keyCode) {
        case Keyboard.UP: scrollHistory(false); break;
        case Keyboard.DOWN: scrollHistory(true); break;
        }
    }

    /** Our client-side context. */
    protected var _ctx :CrowdContext;

    /** The director to which we are sending chat requests. */
    protected var _chatDtr :ChatDirector;

    /** The place where the user may enter chat. */
    protected var _txt :ChatInput;

    /** The current index in the chat command history. */
    protected var _histidx :int = -1;

    /** The button for sending chat. */
    protected var _but :CommandButton;

    /** An array of the currently shown-controls. */
    protected static var _controls :Array = [];

    /** The preserved current line of text when traversing history or carried between instances of
     * ChatControl. */
    protected static var _curLine :String;
}
}
