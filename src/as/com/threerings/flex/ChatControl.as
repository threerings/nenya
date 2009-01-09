//
// $Id$

package com.threerings.flex {

import flash.display.DisplayObjectContainer;

import flash.events.Event;
import flash.events.FocusEvent;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.events.TextEvent;

import flash.ui.Keyboard;

import mx.containers.HBox;

import mx.core.Application;

import mx.controls.TextInput;

import mx.events.FlexEvent;

import com.threerings.util.ArrayUtil;
import com.threerings.util.StringUtil;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandMenu;

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
        // don't call super (and the only time these aren't around is during construction)
        if (_txt != null) {
            _txt.enabled = en;
            _but.enabled = en;
        }
    }

    override public function get enabled () :Boolean
    {
        return _txt.enabled;
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
    public function setChatColor (color :int) :void
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

        // if there was no error, clear the entry area in prep for the next entry event
        _txt.text = "";
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
            ArrayUtil.removeAll(_controls, this);
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
