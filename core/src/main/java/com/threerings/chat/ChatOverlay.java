//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// https://github.com/threerings/nenya
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

package com.threerings.chat;

import java.awt.Point;
import java.awt.Shape;
import java.util.List;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.util.CrowdContext;
import com.threerings.media.VirtualMediaPanel;

import static com.threerings.NenyaLog.log;

/**
 * An abstract class that represents a chat display that can be overlayed upon another component.
 */
public abstract class ChatOverlay
    implements ChatDisplay
{
    /**
     * An interface for providing information about what is under the overlay.
     */
    public interface InfoProvider
    {
        /**
         * Get a list of Shape objects that we should attempt to avoid when laying out the chat.
         * The ChatOverlay will not modify these shape objects. The easy thing to do would be to
         * just return java.awt.Rectangle objects.
         *
         * @param speaker The username of the speaking player, or null.
         * @param high Add to this list shapes that should never be drawn on.
         *
         * @param low If non-null, add to this list shapes that can be drawn on if needed.
         */
        public void getAvoidables (Name speaker, List<Shape> high, List<Shape> low);

        /**
         * Get a point which is approximately the origin of the speaker, or null if unknown.
         */
        public Point getSpeaker (Name speaker);
    }

    /**
     * Causes the chat overlay to make itself visible or invisible.
     */
    public void setVisible (boolean visible)
    {
        // derived classes will want to do the right thing here
    }

    /**
     * Set the dimmed mode of the currently displaying glyphs.
     */
    public void setDimmed (boolean dimmed)
    {
        _dimmed = dimmed;
    }

    /**
     * Indicates that the target component was added to the widget hier. Should be called when we
     * wish to start displaying chat.
     */
    public void added (VirtualMediaPanel target)
    {
        _target = target;
    }

    /**
     * Layout the chat overlay inside the previously configured target component. Should be called
     * if our component changes size.
     */
    public abstract void layout ();

    /**
     * Indicates that the target component was removed from the widget hier. Should be called when
     * we no longer wish to paint chat.
     */
    public void removed ()
    {
        _target = null;
    }

    /**
     * Callback from the target that the place has changed and we are to now talk to the new info
     * provider.
     */
    public void newPlaceEntered (InfoProvider provider)
    {
        _provider = provider;
    }

    /**
     * A callback indicating that we've left the place and should stop talking to a particular
     * infoprovider.
     */
    public void placeExited ()
    {
        _provider = null;
    }

    /**
     * Returns the media panel on which this chat overlay is operating.
     */
    public VirtualMediaPanel getTarget ()
    {
        return _target;
    }

    /**
     * Should be called when a speaker departs the chat area to allow the overlay to clean up.
     */
    public void speakerDeparted (Name speaker)
    {
        // The regular overlay doesn't care about speakers leaving.
    }

    /**
     * Called if our containing media panel scrolled its view.
     */
    public void viewDidScroll (int dx, int dy)
    {
    }

    /**
     * Construct a chat overlay.
     */
    protected ChatOverlay (CrowdContext ctx, ChatLogic logic)
    {
        _ctx = ctx;
        _logic = logic;
    }

    /**
     * Returns true if this chat overlay is showing and should therefore update its display
     * accordingly.
     */
    protected boolean isShowing ()
    {
        return (_target != null) && _target.isShowing();
    }

    /**
     * Translates a string using the general client message bundle.
     */
    protected String xlate (String message)
    {
        return xlate(_logic.getDefaultMessageBundle(), message);
    }

    /**
     * Translates a string using the specified bundle.
     */
    protected String xlate (String bundle, String message)
    {
        if (bundle != null) {
            MessageBundle msgb = _ctx.getMessageManager().getBundle(bundle);
            if (msgb == null) {
                log.warning("No message bundle available to translate message",
                            "bundle", bundle, "message", message);
            } else {
                message = msgb.xlate(message);
            }
        }
        return message;
    }

    /** The light of our life. */
    protected CrowdContext _ctx;

    /** Contains all of our customizations. */
    protected ChatLogic _logic;

    /** The component in which we are being displayed. */
    protected VirtualMediaPanel _target;

    /** The source of hints to how we layout the overlay. */
    protected InfoProvider _provider;

    /** Whether the chat glyphs are dimmed or not. */
    protected boolean _dimmed;
}
