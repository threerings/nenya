//
// $Id: ChatControl.as 5606 2007-08-22 01:38:59Z mdb $

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
        ctx :CrowdContext, sendButtonLabel :String = "send", height :Number = NaN)
    {
        _ctx = ctx;
        _chatDtr = _ctx.getChatDirector();

        this.height = height;
        styleName = "chatControl";

        addChild(_txt = new ChatInput());
        _txt.addEventListener(FlexEvent.ENTER, sendChat, false, 0, true);

        _but = new CommandButton();
        _but.label = sendButtonLabel;
        _but.setCallback(sendChat);
        addChild(_but);

        if (!isNaN(height)) {
            _txt.height = height;
            _but.height = height;
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
     * Enables or disables our chat input.
     */
    public function setEnabled (enabled :Boolean) :void
    {
        _txt.enabled = enabled;
        _but.enabled = enabled;
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

    override public function parentChanged (p :DisplayObjectContainer) :void
    {
        super.parentChanged(p);

        if (p != null) {
            // set up any already-configured text
            _txt.text = _curLine;

            // request focus
            callLater(function () :void {
                _txt.setFocus();
            });
            _controls.push(this);

        } else {
            _curLine = _txt.text;
            ArrayUtil.removeAll(_controls, this);
        }
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
    }

    /** Our client-side context. */
    protected var _ctx :CrowdContext;

    /** The director to which we are sending chat requests. */
    protected var _chatDtr :ChatDirector;

    /** The place where the user may enter chat. */
    protected var _txt :ChatInput;

    /** The button for sending chat. */
    protected var _but :CommandButton;

    /** An array of the currently shown-controls. */
    protected static var _controls :Array = [];

    /** The preserved current line of text when traversing history or carried between instances of
     * ChatControl. */
    protected static var _curLine :String;
}
}
