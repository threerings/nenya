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

package com.threerings.jme.chat;

import com.jme.renderer.ColorRGBA;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BTextArea;
import com.jmex.bui.BTextField;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.client.SpeakService;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.SystemMessage;
import com.threerings.crowd.chat.data.UserMessage;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.jme.JmeContext;

import static com.threerings.jme.Log.log;

/**
 * Displays chat messages and allows for their input.
 */
public class ChatView extends BContainer
    implements ChatDisplay
{
    public ChatView (JmeContext ctx, ChatDirector chatdtr)
    {
        _chatdtr = chatdtr;
        setLayoutManager(new BorderLayout(2, 2));
        add(_text = new BTextArea(), BorderLayout.CENTER);
        _incont = new BContainer(GroupLayout.makeHStretch());
        add(_incont, BorderLayout.SOUTH);
        _incont.add(_input = new BTextField());
        _input.addListener(_inlist);
    }

    public void willEnterPlace (PlaceObject plobj)
    {
        setSpeakService(plobj.speakService, null);
    }

    public void didLeavePlace (PlaceObject plobj)
    {
        clearSpeakService();
    }

    public void setSpeakService (SpeakService spsvc, String localType)
    {
        if (spsvc != null) {
            _spsvc = spsvc;
            _localType = localType;
            _chatdtr.addChatDisplay(this);
        }
    }

    public void clearSpeakService ()
    {
        if (_spsvc != null) {
            _chatdtr.removeChatDisplay(this);
            _spsvc = null;
            _localType = null;
        }
    }

    public void setChatButton (BButton button)
    {
        _inBtn = button;
        _incont.add(_inBtn, GroupLayout.FIXED);
        _inBtn.addListener(_inlist);
    }

    /**
     * Instructs our chat input field to request focus.
     */
    public void requestFocus ()
    {
        _input.requestFocus();
    }

    // documentation inherited from interface ChatDisplay
    public void clear ()
    {
        if (_input.hasFocus() || _inBtn.hasFocus()) {
            _text.clearText();
        }
    }

    // documentation inherited from interface ChatDisplay
    public boolean displayMessage (ChatMessage msg, boolean alreadyDisplayed)
    {
        // we may be restricted in the chat types we handle
        if (!handlesType(msg.localtype)) {
            return false;
        }

        if (msg instanceof UserMessage) {
            UserMessage umsg = (UserMessage) msg;
            if (ChatCodes.USER_CHAT_TYPE.equals(umsg.localtype)) {
                append("[" + umsg.speaker + " whispers] ", ColorRGBA.green);
                append(umsg.message + "\n");
            } else {
                append("<" + umsg.speaker + "> ", ColorRGBA.blue);
                append(umsg.message + "\n");
            }
            return true;

        } else if (msg instanceof SystemMessage) {
            append(msg.message + "\n", ColorRGBA.red);
            return true;

        } else {
            log.warning("Received unknown message type: " + msg + ".");
            return false;
        }
    }

    // documentation inherited
    public void setEnabled (boolean enabled)
    {
        _input.setEnabled(enabled);
    }

    protected void displayError (String message)
    {
        append(message + "\n", ColorRGBA.red);
    }

    protected void append (String text, ColorRGBA color)
    {
        _text.appendText(text, color);
    }

    protected void append (String text)
    {
        _text.appendText(text);
    }

    protected boolean handlesType (String localType)
    {
        return _localType == null || _localType.equals(localType);
    }

    protected boolean handleInput (String text)
    {
        if (text.length() == 0) {
            // no empty banter
            return false;
        }
        String msg = _chatdtr.requestChat(_spsvc, text, true);
        if (msg.equals(ChatCodes.SUCCESS)) {
            return true;
        } else {
            _chatdtr.displayFeedback(null, msg);
            return false;
        }
    }

    /** Used to trigger sending chat (on return key or button press). */
    protected ActionListener _inlist = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            if (handleInput(_input.getText())) {
                _input.setText("");
            }
        }
    };

    protected ChatDirector _chatdtr;
    protected SpeakService _spsvc;
    protected String _localType;

    protected BTextArea _text;
    protected BContainer _incont;
    protected BTextField _input;
    protected BButton _inBtn;
}
