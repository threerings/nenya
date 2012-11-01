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

import java.awt.Color;
import java.awt.Font;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;

import com.samskivert.swing.Label;
import com.samskivert.util.Tuple;

import com.threerings.crowd.chat.data.ChatCodes;

/**
 * Contains all of the routines that might (or must) be customized by a system that makes use of
 * the comic chat system.
 */
public abstract class ChatLogic
{
    /** The default chat decay parameter. See {@link #getDisplayDurationIndex}. */
    public static final int DEFAULT_CHAT_DECAY = 1;

    /** The padding in each direction around the text to the edges of a chat 'bubble'. */
    public static final int PAD = 10;

    /** Type mode code for default chat type (speaking). */
    public static final int SPEAK = 0;

    /** Type mode code for shout chat type. */
    public static final int SHOUT = 1;

    /** Type mode code for emote chat type. */
    public static final int EMOTE = 2;

    /** Type mode code for think chat type. */
    public static final int THINK = 3;

    /** Type place code for default place chat (cluster, scene). */
    public static final int PLACE = 1 << 4;

    // 2 and 3 are skipped for legacy migration reasons

    /** Type code for a chat type that was used in some special context, like in a negotiation. */
    public static final int SPECIALIZED = 4 << 4;

    /** Our internal code for tell chat. */
    public static final int TELL = 5 << 4;

    /** Our internal code for tell feedback chat. */
    public static final int TELLFEEDBACK = 6 << 4;

    /** Our internal code for info system messges. */
    public static final int INFO = 7 << 4;

    /** Our internal code for feedback system messages. */
    public static final int FEEDBACK = 8 << 4;

    /** Our internal code for attention system messages. */
    public static final int ATTENTION = 9 << 4;

    /** Our internal code for any type of chat that is continued in a subtitle. */
    public static final int CONTINUATION = 10 << 4;

    /** Type place code for broadcast chat type. */
    public static final int BROADCAST = 11 << 4;

    /** Our internal code for a chat type we will ignore. */
    public static final int IGNORECHAT = -1;

    /**
     * Returns the message bundle used to translate default messages.
     */
    public abstract String getDefaultMessageBundle ();

    /**
     * Determines the format string and whether to use quotes based on the chat type.
     */
    public Tuple<String, Boolean> decodeFormat (int type, String format)
    {
        boolean quotes = true;
        switch (placeOf(type)) {
        // derived classes may wish to change the format here based on the place
        case PLACE:
            switch (modeOf(type)) {
            case EMOTE:
                quotes = false;
                break;
            }
            break;
        }
        return Tuple.newTuple(format, quotes);
    }

    /**
     * Decodes the main chat type given the supplied localtype provided by the chat system.
     */
    public int decodeType (String localtype)
    {
        if (ChatCodes.USER_CHAT_TYPE.equals(localtype)) {
            return TELL;
        } else if (ChatCodes.PLACE_CHAT_TYPE.equals(localtype)) {
            return PLACE;
        } else {
            return 0;
        }
    }

    /**
     * Adjust the chat type based on the mode of the chat message.
     */
    public int adjustTypeByMode (int mode, int type)
    {
        switch (mode) {
        case ChatCodes.DEFAULT_MODE:
            return type | SPEAK;
        case ChatCodes.EMOTE_MODE:
            return type | EMOTE;
        case ChatCodes.THINK_MODE:
            return type | THINK;
        case ChatCodes.SHOUT_MODE:
            return type | SHOUT;
        case ChatCodes.BROADCAST_MODE:
            return BROADCAST; // broadcast always looks like broadcast
        default:
            return type;
        }
    }

    /**
     * Get the font to use for the given bubble type.
     */
    public Font getFont (int type)
    {
        return DEFAULT_FONT;
    }

    /**
     * Creates a label for the specified text. Derived classes may wish to use specialized labels.
     */
    public Label createLabel (String text)
    {
        return new Label(text);
    }

    /**
     * Computes the chat glyph outline color from the chat message type.
     */
    public Color getOutlineColor (int type)
    {
        switch (type) {
        case BROADCAST:
            return BROADCAST_COLOR;
        case TELL:
            return TELL_COLOR;
        case TELLFEEDBACK:
            return TELLFEEDBACK_COLOR;
        case INFO:
            return INFO_COLOR;
        case FEEDBACK:
            return FEEDBACK_COLOR;
        case ATTENTION:
            return ATTENTION_COLOR;
        default:
            return Color.black;
        }
    }

    /**
     * Get the appropriate shape for the specified type of chat.
     *
     * @param r the rectangle bounding the chat label.
     * @param b the rectangle bounding the chat label and icon.
     */
    public Shape getSubtitleShape (int type, Rectangle r, Rectangle b)
    {
        int placeType = placeOf(type);
        switch (placeType) {
        case SPECIALIZED:
        case PLACE:
            return getPlaceSubtitleShape(type, r);

        case TELL: {
            // lightning box!
            int halfy = r.y + r.height/2;
            Polygon p = new Polygon();
            p.addPoint(r.x - PAD - 2, r.y);
            p.addPoint(r.x - PAD / 2 - 2, halfy);
            p.addPoint(r.x - PAD, halfy);
            p.addPoint(r.x - PAD / 2, r.y + r.height);
            p.addPoint(r.x + r.width + PAD + 2, r.y + r.height);
            p.addPoint(r.x + r.width + PAD / 2 + 2, halfy);
            p.addPoint(r.x + r.width + PAD, halfy);
            p.addPoint(r.x + r.width + PAD / 2, r.y);
            return p;
        }

        case TELLFEEDBACK: {
            // reverse-lightning box!
            int halfy = r.y + r.height/2;
            Polygon p = new Polygon();
            p.addPoint(r.x - PAD - 2, r.y + r.height);
            p.addPoint(r.x - PAD / 2 - 2, halfy);
            p.addPoint(r.x - PAD, halfy);
            p.addPoint(r.x - PAD / 2, r.y);
            p.addPoint(r.x + r.width + PAD + 2, r.y);
            p.addPoint(r.x + r.width + PAD / 2 + 2, halfy);
            p.addPoint(r.x + r.width + PAD, halfy);
            p.addPoint(r.x + r.width + PAD / 2, r.y + r.height);
            return p;
        }

        case FEEDBACK: {
            // slanted box subtitle
            Polygon p = new Polygon();
            p.addPoint(r.x - PAD / 2, r.y);
            p.addPoint(r.x + r.width + PAD, r.y);
            p.addPoint(r.x + r.width + PAD / 2, r.y + r.height);
            p.addPoint(r.x - PAD, r.y + r.height);
            return p;
        }

        case BROADCAST:
        case CONTINUATION:
        case INFO:
        case ATTENTION:
        default: {
            Rectangle grown = new Rectangle(r);
            grown.grow(PAD, 0);
            return new Area(grown);
        }
        }
    }

    /**
     * Get the spacing, in pixels, between the latest subtitle of the specified type and the
     * previous subtitle.
     */
    public int getSubtitleSpacing (int type)
    {
        switch (placeOf(type)) {
        // derived classes may wish to adjust subtitle spacing here based on chat type
        default:
            return 1;
        }
    }

    /**
     * A helper function for {@link #getSubtitleShape}.
     */
    public Shape getPlaceSubtitleShape (int type, Rectangle r)
    {
        switch (modeOf(type)) {
        default:
        case SPEAK: {
            // rounded rectangle subtitle
            Area a = new Area(r);
            a.add(new Area(new Ellipse2D.Float(r.x - PAD, r.y, PAD * 2, r.height)));
            a.add(new Area(new Ellipse2D.Float(r.x + r.width - PAD, r.y, PAD * 2, r.height)));
            return a;
        }

        case THINK: {
            r.grow(PAD / 2, 0);
            Area a = new Area(r);
            int dia = 8;
            int num = (int) Math.ceil(r.height / ((float) dia));
            int leftside = r.x - dia/2;
            int rightside = r.x + r.width - dia/2 - 1;

            int maxh = r.height - dia;
            for (int ii=0; ii < num; ii++) {
                int ypos = r.y + Math.min((r.height * ii) / num, maxh);
                a.add(new Area(new Ellipse2D.Float(leftside, ypos, dia, dia)));
                a.add(new Area(new Ellipse2D.Float(rightside, ypos, dia, dia)));
            }
            return a;
        }

        case SHOUT: {
            r.grow(PAD / 2, 0);
            Area a = new Area(r);
            Polygon left = new Polygon();
            Polygon right = new Polygon();

            int spikehei = 8;
            int num = (int) Math.ceil(r.height / ((float) spikehei));
            left.addPoint(r.x, r.y);
            left.addPoint(r.x - PAD, r.y + spikehei / 2);
            left.addPoint(r.x, r.y + spikehei);
            right.addPoint(r.x + r.width , r.y);
            right.addPoint(r.x + r.width + PAD, r.y + spikehei / 2);
            right.addPoint(r.x + r.width, r.y + spikehei);

            int ypos = 0;
            int maxpos = r.y + r.height - spikehei + 1;
            for (int ii=0; ii < num; ii++) {
                int newpos = Math.min((r.height * ii) / num, maxpos);
                left.translate(0, newpos - ypos);
                right.translate(0, newpos - ypos);
                a.add(new Area(left));
                a.add(new Area(right));
                ypos = newpos;
            }

            return a;
        }

        case EMOTE: {
            // a box that curves inward on the left and right
            r.grow(PAD, 0);
            Area a = new Area(r);
            a.subtract(new Area(new Ellipse2D.Float(r.x - PAD / 2, r.y, PAD, r.height)));
            a.subtract(new Area(new Ellipse2D.Float(r.x + r.width - PAD / 2, r.y, PAD, r.height)));
            return a;
        }
        }
    }

    /**
     * Returns metrics on how long chat messages should be displayed.
     */
    public long[] getDisplayDurations (int indexOffset)
    {
        return DISPLAY_DURATION_PARAMS[getDisplayDurationIndex() + indexOffset];
    }

    /**
     * Get the current display duration parameters: 0 = fast, 1 = medium, 2 = long.
     * See {@link #DISPLAY_DURATION_PARAMS}.
     */
    protected int getDisplayDurationIndex ()
    {
        return DEFAULT_CHAT_DECAY;
    }

    /**
     * Extracts the mode constant from the type value.
     */
    protected static int modeOf (int type)
    {
        return type & 0xF;
    }

    /**
     * Extract the place constant from the type value.
     */
    protected static int placeOf (int type)
    {
        return type & ~0xF;
    }

    /**
     * Times to display chat: { (time per character), (min time), (max time) }
     *
     * Groups 0/1/2 are short/medium/long for chat bubbles, and groups 1/2/3 are short/medium/long
     * for subtitles.
     */
    protected static final long[][] DISPLAY_DURATION_PARAMS = {
        { 125L, 10000L, 30000L },  // fastest durations
        { 200L, 15000L, 40000L },  // medium (default) durations
        { 275L, 20000L, 50000L },  // longest regular duration..
        { 350L, 25000L, 60000L }   // grampatime!
    };

    // used to color chat bubbles
    protected static final Color BROADCAST_COLOR = new Color(0x990000);
    protected static final Color FEEDBACK_COLOR = new Color(0x00AA00);
    protected static final Color TELL_COLOR = new Color(0x0000AA);
    protected static final Color TELLFEEDBACK_COLOR = new Color(0x00AAAA);
    protected static final Color INFO_COLOR = new Color(0xAAAA00);
    protected static final Color ATTENTION_COLOR = new Color(0xFF5000);

    /** Our default chat font. */
    protected static final Font DEFAULT_FONT = new Font("SansSerif", Font.PLAIN, 12);
}
