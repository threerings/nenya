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

import java.util.Iterator;
import java.util.List;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.swing.BoundedRangeModel;
import javax.swing.Icon;
import javax.swing.JScrollBar;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.google.common.collect.Lists;

import com.samskivert.swing.Label;
import com.samskivert.util.Tuple;

import com.threerings.media.VirtualMediaPanel;
import com.threerings.util.MessageBundle;

import com.threerings.crowd.chat.client.HistoryList;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.SystemMessage;
import com.threerings.crowd.chat.data.TellFeedbackMessage;
import com.threerings.crowd.chat.data.UserMessage;
import com.threerings.crowd.util.CrowdContext;

import static com.threerings.NenyaLog.log;

/**
 * Implements subtitle chat.
 */
public class SubtitleChatOverlay extends ChatOverlay
    implements ChangeListener, HistoryList.Observer
{
    /**
     * Construct a subtitle chat overlay.
     *
     * @param subtitleHeight the height of the subtitle area.
     */
    public SubtitleChatOverlay (CrowdContext ctx, ChatLogic logic, JScrollBar bar,
                                int subtitleHeight)
    {
        this(ctx, logic, bar, subtitleHeight, false, 8, 8);
    }

    /**
     * Construct a comic chat overlay using all the available space for subtitling.
     */
    public SubtitleChatOverlay (CrowdContext ctx, ChatLogic logic, JScrollBar bar)
    {
        this(ctx, logic, bar, false);
    }

    public SubtitleChatOverlay (CrowdContext ctx, ChatLogic logic, JScrollBar bar, boolean full)
    {
        this(ctx, logic, bar, 0, true, full ? 8 : 3, full ? 8 : 4);
    }

    public SubtitleChatOverlay (CrowdContext ctx, ChatLogic logic, JScrollBar bar, boolean full,
                                boolean overrideHistory)
    {
        this(ctx, logic, bar, full);
        _overrideHistory = overrideHistory;
    }

    // from interface HistoryList.Observer
    public void historyUpdated (int adjustment)
    {
        if (adjustment != 0) {
            for (int ii = 0, nn = _showingHistory.size(); ii < nn; ii++) {
                ChatGlyph cg = _showingHistory.get(ii);
                cg.histIndex -= adjustment;
            }
            // some history entries were deleted, we need to re-figure the history scrollbar action
            resetHistoryOffset();
        }

        if (isLaidOut() && isHistoryMode()) {
            int val = _historyModel.getValue();
            updateHistBar(val - adjustment);

            // only repaint if we need to
            if ((val != _historyModel.getValue()) || (adjustment != 0) || !_histOffsetFinal) {
                figureCurrentHistory();
            }
        }
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent e)
    {
        // the scrollbar has changed.
        if (!_settingBar) {
            figureCurrentHistory();
        }
    }

    // documentation inherited from superinterface ChatDisplay
    public void clear ()
    {
        clearGlyphs(_subtitles);
    }

    // documentation inherited from superinterface ChatDisplay
    public boolean displayMessage (ChatMessage message, boolean alreadyDisplayed)
    {
        // nothing doing if we've not been laid out
        if (!isLaidOut()) {
            return false;
        }

        // possibly display it now
        Graphics2D gfx = getTargetGraphics();
        if (gfx != null) {
            displayMessage(message, gfx); // display it
            gfx.dispose(); // clean up
            return true;
        }
        return false;
    }

    @Override
    public void viewDidScroll (int dx, int dy)
    {
        super.viewDidScroll(dx, dy);
        viewDidScroll(_subtitles, dx, dy);
        viewDidScroll(_showingHistory, dx, dy);
    }

    @Override
    public void added (VirtualMediaPanel target)
    {
        super.added(target);

        _history.addObserver(this);

        if (_overrideHistory) {
            setHistoryEnabled(true);
            return;
        }

        // derived classes may want to override this method and set up chat history mode based on
        // whatever preference storage mechanism they use
    }

    @Override
    public void layout ()
    {
        // sanity check
        if (_target == null) {
            log.warning(this + " laid out without target?", new Exception());
            return;
        }

        Rectangle vbounds = _target.getViewBounds();
        if ((vbounds.height < 1) || (vbounds.width < 1)) {
            return; // fuck that!
        }

        clearGlyphs(_subtitles); // we'll re-populate from the history

        if (_subtitlesFill) {
            _subtitleHeight = vbounds.height;
        }

        // make a guess as to the extent of the history (how many avg sized subtitles will fit in
        // the subtitle area)
        _historyExtent = ((_subtitleHeight - _subtitleYSpacing) / SUBTITLE_HEIGHT_GUESS);
        _scrollbar.setBlockIncrement(_historyExtent);

        // show messages that were born recently enough to be shown.
        long now = System.currentTimeMillis();
        // find the first message to display
        int histSize = _history.size();
        int index = histSize - 1;
        for ( ; index >= 0; index--) {
            ChatMessage msg = _history.get(index);
            _lastExpire = 0L;
            if (now > getChatExpire(msg.timestamp, msg.message)) {
                break;
            }
        }
        // now that we've found the message that's one too old, increment the index so that it
        // points to the first message we should display
        index++;
        _lastExpire = 0L;

        // now dispatch from that point
        Graphics2D gfx = getTargetGraphics();
        for ( ; index < histSize; index++) {
            ChatMessage msg = _history.get(index);
            if (shouldShowFromHistory(msg, index)) {
                displayMessage(msg, gfx);
            }
        }

        // and clean up
        gfx.dispose();

        // make a note that we're laid out
        _laidout = true;

        // reset the history offset..
        resetHistoryOffset();

        // finally, if we're in history mode, we should figure that out too
        if (isHistoryMode()) {
            updateHistBar(histSize - 1);
            figureCurrentHistory();
        }
    }

    @Override
    public void setDimmed (boolean dimmed)
    {
        super.setDimmed(dimmed);
        updateDimmed(_subtitles);
        updateDimmed(_showingHistory);
    }

    @Override
    public void removed ()
    {
        // we need to do this before super so that our target is still around
        clearGlyphs(_subtitles);
        clearGlyphs(_showingHistory);

        super.removed();

        _history.removeObserver(this);

        // clear out our history so that when we are once again added, we activate it and go
        // through the motions of refiguring everything
        setHistoryEnabled(false);

        // make a note that we'll need to lay ourselves out before we do anything fun next time
        _laidout = false;
    }

    /**
     * Shared chained constructor.
     */
    protected SubtitleChatOverlay (CrowdContext ctx, ChatLogic logic, JScrollBar bar, int height,
                                   boolean fill, int xspace, int yspace)
    {
        super(ctx, logic);

        _scrollbar = bar;
        _subtitleHeight = height;
        _subtitlesFill = fill;
        _subtitleXSpacing = xspace;
        _subtitleYSpacing = yspace;
        _history = ctx.getChatDirector().getHistory();
    }

    /**
     * Update the chat glyphs in the specified list to be set to the current dimmed setting.
     */
    protected void updateDimmed (List<? extends ChatGlyph> glyphs)
    {
        for (ChatGlyph glyph : glyphs) {
            glyph.setDim(_dimmed);
        }
    }

    /**
     * Are we currently in history mode?
     */
    protected boolean isHistoryMode ()
    {
        return (_historyModel != null);
    }

    /**
     * Return the current Graphics context of our target, or null if not applicable.
     */
    protected Graphics2D getTargetGraphics ()
    {
        // this may return null even if target is not null.
        return (_target == null) ? null : (Graphics2D)_target.getGraphics();
    }

    /**
     * Configures us for display of chat history or not.
     */
    protected void setHistoryEnabled (boolean historyEnabled)
    {
        if (historyEnabled && _historyModel == null) {
            _historyModel = _scrollbar.getModel();
            _historyModel.addChangeListener(this);
            resetHistoryOffset();

            // out with the subtitles, we'll be displaying history
            clearGlyphs(_subtitles);

            // "scroll" down to the latest history entry
            updateHistBar(_history.size() - 1);

            // refigure our history
            figureCurrentHistory();

        } else if (!historyEnabled && _historyModel != null) {
            _historyModel.removeChangeListener(this);
            _historyModel = null;

            // out with the history, we'll be displaying subtitles
            clearGlyphs(_showingHistory);
        }
    }

    /**
     * Helper function for informing glyphs of the scrolled view.
     */
    protected void viewDidScroll (List<? extends ChatGlyph> glyphs, int dx, int dy)
    {
        for (ChatGlyph glyph : glyphs) {
            glyph.viewDidScroll(dx, dy);
        }
    }

    /**
     * Update the history scrollbar with the specified value.
     */
    protected void updateHistBar (int val)
    {
        // we may need to figure out the new history offset amount..
        if (!_histOffsetFinal && _history.size() > _histOffset) {
            Graphics2D gfx = getTargetGraphics();
            if (gfx != null) {
                figureHistoryOffset(gfx);
                gfx.dispose();
            }
        }

        // then figure out the new value and range
        int oldval = Math.max(_histOffset, val);
        int newmaxval = Math.max(0, _history.size() - 1);
        int newval = (oldval >= newmaxval - 1) ? newmaxval : oldval;

        // and set it, which MAY generate a change event, but we want to ignore it so we use the
        // _settingBar flag
        _settingBar = true;
        _historyModel.setRangeProperties(newval, _historyExtent, _histOffset,
                                         newmaxval + _historyExtent,
                                         _historyModel.getValueIsAdjusting());
        _settingBar = false;
    }

    /**
     * Reset the history offset so that it will be recalculated next time it is needed.
     */
    protected void resetHistoryOffset ()
    {
        _histOffsetFinal = false;
        _histOffset = 0;
    }

    /**
     * Figure out how many of the first history elements fit in our bounds such that we can set the
     * bounds on the scrollbar correctly such that the scrolling to the smallest value just barely
     * puts the first element onscreen.
     */
    protected void figureHistoryOffset (Graphics2D gfx)
    {
        if (!isLaidOut()) {
            return;
        }

        int hei = _subtitleYSpacing;
        int hsize = _history.size();
        for (int ii = 0; ii < hsize; ii++) {
            ChatGlyph rec = getHistorySubtitle(ii, gfx);
            Rectangle r = rec.getBounds();
            hei += r.height;

            // oop, we passed it, it was the last one
            if (hei >= _subtitleHeight) {
                _histOffset = Math.max(0, ii - 1);
                _histOffsetFinal = true;
                return;
            }

            hei += getHistorySubtitleSpacing(ii);
        }

        // basically, this means there isn't yet enough history to fill the first 'page' of the
        // history scrollback, so we set the offset to the max value, but we do not set
        // _histOffsetFinal to be true so that this will be recalculated
        _histOffset = hsize - 1;
    }

    /**
     * Figure out which ChatMessages in the history should currently appear in the showing history.
     */
    protected void figureCurrentHistory ()
    {
        int first = _historyModel.getValue();
        int count = 0;
        Graphics2D gfx = null;

        if (isLaidOut() && !_history.isEmpty()) {
            gfx = getTargetGraphics();
            if (gfx == null) {
                log.warning("Can't figure current history, no graphics.");
                return;
            }

            // start from the bottom..
            Rectangle vbounds = _target.getViewBounds();
            int ypos = vbounds.height - _subtitleYSpacing;

            for (int ii = first; ii >= 0; ii--, count++) {
                ChatGlyph rec = getHistorySubtitle(ii, gfx);

                // see if it will fit
                Rectangle r = rec.getBounds();
                ypos -= r.height;
                if ((count != 0) && ypos <= (vbounds.height - _subtitleHeight)) {
                    break; // don't add that one..
                }

                // position it
                rec.setLocation(vbounds.x + _subtitleXSpacing, vbounds.y + ypos);
                // add space for the next
                ypos -= getHistorySubtitleSpacing(ii);
            }
        }

        // finally, because we've been adding to the _showingHistory here (via getHistorySubtitle)
        // and in figureHistoryOffset (possibly called prior to this method) we now need to prune
        // out the ChatGlyphs that aren't actually needed and make sure the ones that are are
        // positioned on the screen correctly
        for (Iterator<ChatGlyph> itr = _showingHistory.iterator(); itr.hasNext(); ) {
            ChatGlyph cg = itr.next();
            boolean managed = (_target != null) && _target.isManaged(cg);
            if (cg.histIndex <= first && cg.histIndex > (first - count)) {
                // it should be showing
                if (!managed) {
                    _target.addAnimation(cg);
                }

            } else {
                // it shouldn't be showing
                if (managed) {
                    _target.abortAnimation(cg);
                }
                itr.remove();
            }
        }

        if (gfx != null) {
            gfx.dispose();
        }
    }

    /**
     * Get the glyph for the specified history index, creating if necessary.
     */
    protected ChatGlyph getHistorySubtitle (int index, Graphics2D layoutGfx)
    {
        // do a brute search (over a small set) for an already-created subtitle
        for (int ii = 0, nn = _showingHistory.size(); ii < nn; ii++) {
            ChatGlyph cg = _showingHistory.get(ii);
            if (cg.histIndex == index) {
                return cg;
            }
        }

        // it looks like we have to create a new one: expensive!
        ChatGlyph cg = createHistorySubtitle(index, layoutGfx);
        cg.histIndex = index;
        cg.setDim(_dimmed);
        _showingHistory.add(cg);
        return cg;
    }

    /**
     * Creates a subtitle for display in the history panel.
     *
     * @param index the index of the message in the history list
     */
    protected ChatGlyph createHistorySubtitle (int index, Graphics2D layoutGfx)
    {
        ChatMessage message = _history.get(index);
        int type = getType(message, true);
        return createSubtitle(message, type, layoutGfx, false);
    }

    /**
     * Determines the amount of spacing to put after a history subtitle.
     *
     * @param index the index of the message in the history list
     */
    protected int getHistorySubtitleSpacing (int index)
    {
        ChatMessage message = _history.get(index);
        return _logic.getSubtitleSpacing(getType(message, true));
    }

    protected boolean isLaidOut ()
    {
        return isShowing() && _laidout;
    }

    /**
     * We're looking through history to figure out what messages we should be showing, should we
     * show the following?
     */
    protected boolean shouldShowFromHistory (ChatMessage msg, int index)
    {
        return true; // yes by default.
    }

    /**
     * Clears out the supplied list of chat glyphs.
     */
    protected void clearGlyphs (List<ChatGlyph> glyphs)
    {
        if (_target != null) {
            for (int ii = 0, nn = glyphs.size(); ii < nn; ii++) {
                ChatGlyph rec = glyphs.get(ii);
                _target.abortAnimation(rec);
            }

        } else if (!glyphs.isEmpty()) {
            log.warning("No target to abort chat animations");
        }
        glyphs.clear();
    }

    /**
     * Display the specified message now, unless we are to ignore it.
     */
    protected void displayMessage (ChatMessage message, Graphics2D gfx)
    {
        // get the non-history message type...
        int type = getType(message, false);
        if (type != ChatLogic.IGNORECHAT) {
            // display it now
            displayMessage(message, type, gfx);
        }
    }

    /**
     * Display the message after we've decided which type it is.
     */
    protected void displayMessage (ChatMessage message, int type, Graphics2D layoutGfx)
    {
        // if we're in history mode, this will show up in the history and we'll rebuild our
        // subtitle list if and when history goes away
        if (isHistoryMode()) {
            return;
        }
        addSubtitle(createSubtitle(message, type, layoutGfx, true));
    }

    /**
     * Add a subtitle for display now.
     */
    protected void addSubtitle (ChatGlyph rec)
    {
        // scroll up the old subtitles
        Rectangle r = rec.getBounds();
        scrollUpSubtitles(-r.height - _logic.getSubtitleSpacing(rec.getType()));

        // put this one in place
        Rectangle vbounds = _target.getViewBounds();
        rec.setLocation(vbounds.x + _subtitleXSpacing,
                        vbounds.y + vbounds.height - _subtitleYSpacing - r.height);

        // add it to our list and to our media panel
        rec.setDim(_dimmed);
        _subtitles.add(rec);
        _target.addAnimation(rec);
    }

    /**
     * Create a subtitle, but don't do anything funny with it.
     */
    protected ChatGlyph createSubtitle (ChatMessage message, int type, Graphics2D layoutGfx,
                                        boolean expires)
    {
        // we might need to modify the textual part with translations, but we can't do that to the
        // message object, since other chatdisplays also get it.
        String text = message.message;
        Tuple<String, Boolean> finfo = _logic.decodeFormat(type, message.getFormat());
        String format = finfo.left;
        boolean quotes = finfo.right;

        // now format the text
        if (format != null) {
            if (quotes) {
                text = "\"" + text + "\"";
            }
            text = " " + text;
            text = xlate(MessageBundle.tcompose(
                             format, ((UserMessage) message).getSpeakerDisplayName())) + text;
        }

        return createSubtitle(layoutGfx, type, message.timestamp, null, 0, text, expires);
    }

    /**
     * Create a subtitle- a line of text that goes on the bottom.
     */
    protected ChatGlyph createSubtitle (Graphics2D gfx, int type, long timestamp, Icon icon,
                                        int indent, String text, boolean expires)
    {
        Dimension is = new Dimension();
        if (icon != null) {
            is.setSize(icon.getIconWidth(), icon.getIconHeight());
        }

        Rectangle vbounds = _target.getViewBounds();
        Label label = _logic.createLabel(text);
        label.setFont(_logic.getFont(type));
        int paddedIconWidth = (icon == null) ? 0 : is.width + ICON_PADDING;
        label.setTargetWidth(
            vbounds.width - indent - paddedIconWidth -
            2 * (_subtitleXSpacing + Math.max(UIManager.getInt("ScrollBar.width"), PAD)));
        label.layout(gfx);
        gfx.dispose();

        Dimension ls = label.getSize();
        Rectangle r = new Rectangle(0, 0, ls.width + indent + paddedIconWidth,
                                    Math.max(is.height, ls.height));
        r.grow(0, 1);
        Point iconpos = r.getLocation();
        iconpos.translate(indent, r.height - is.height - 1);
        Point labelpos = r.getLocation();
        labelpos.translate(indent + paddedIconWidth, 1);
        Rectangle lr = new Rectangle(r.x + indent + paddedIconWidth, r.y,
                                     r.width - indent - paddedIconWidth, ls.height + 2);

        // last a really long time if we're not supposed to expire
        long lifetime = Integer.MAX_VALUE;
        if (expires) {
            lifetime = getChatExpire(timestamp, label.getText()) - timestamp;
        }
        Shape shape = _logic.getSubtitleShape(type, lr, r);
        return new ChatGlyph(this, type, lifetime, r.union(shape.getBounds()), shape, icon,
                             iconpos, label, labelpos, _logic.getOutlineColor(type));
    }

    /**
     * Get the expire time for the specified chat.
     */
    protected long getChatExpire (long timestamp, String text)
    {
        long[] durations = _logic.getDisplayDurations(getDisplayDurationOffset());
        // start the computation from the maximum of the timestamp or our last expire time
        long start = Math.max(timestamp, _lastExpire);
        // set the next expire to a time proportional to the text length
        _lastExpire = start + Math.min(text.length() * durations[0], durations[2]);
        // but don't let it be longer than the maximum display time
        _lastExpire = Math.min(timestamp + durations[2], _lastExpire);
        // and be sure to pop up the returned time so that it is above the min
        return Math.max(timestamp + durations[1], _lastExpire);
    }

    /**
     * A hack to allow subtitle chat to display longer and comic chat to display for a normal
     * duration.
     */
    protected int getDisplayDurationOffset ()
    {
        // the subtitle view adds one to bump up to the next longer display duration because we
        // want subtitles to stick around longer
        return 1;
    }

    /**
     * Called by a chat glyph when it has determined that it is expired.
     */
    protected void glyphExpired (ChatGlyph glyph)
    {
        _subtitles.remove(glyph);
    }

    /**
     * Convert the Message class/localtype/mode into our internal type code.
     */
    protected int getType (ChatMessage message, boolean history)
    {
        String localtype = message.localtype;

        if (message instanceof TellFeedbackMessage) {
            if (((TellFeedbackMessage)message).isFailure()) {
                return ChatLogic.FEEDBACK;
            }
            return (history || isApprovedLocalType(localtype)) ?
                ChatLogic.TELLFEEDBACK : ChatLogic.IGNORECHAT;

        } else if (message instanceof UserMessage) {
            int type = _logic.decodeType(localtype);
            if (type != 0) {
                // factor in the mode
                return _logic.adjustTypeByMode(((UserMessage) message).mode, type);
            }
            // if we're showing from history, include specialized chat messages
            if (history) {
                return ChatLogic.SPECIALIZED;
            }
            // otherwise fall through and IGNORECHAT

        } else if (message instanceof SystemMessage) {
            if (history || isApprovedLocalType(localtype)) {
                switch (((SystemMessage) message).attentionLevel) {
                case SystemMessage.INFO:
                    return ChatLogic.INFO;
                case SystemMessage.FEEDBACK:
                    return ChatLogic.FEEDBACK;
                case SystemMessage.ATTENTION:
                    return ChatLogic.ATTENTION;
                default:
                    log.warning("Unknown attention level for system message", "msg", message);
                }
            }
            return ChatLogic.IGNORECHAT;
        }

        log.warning("Skipping received message of unknown type", "msg", message);
        return ChatLogic.IGNORECHAT;
    }

    /**
     * Check to see if we want to display the specified localtype.
     */
    protected boolean isApprovedLocalType (String localtype)
    {
        return true; // we show everything, the ComicChat is a little more picky
    }

    /**
     * Scroll all the subtitles up by the specified amount.
     */
    protected void scrollUpSubtitles (int dy)
    {
        // dirty and move all the old glyphs
        Rectangle vbounds = _target.getViewBounds();
        int miny = vbounds.y + vbounds.height - _subtitleHeight;
        for (Iterator<ChatGlyph> iter = _subtitles.iterator(); iter.hasNext();) {
            ChatGlyph sub = iter.next();
            sub.translate(0, dy);
            if (sub.getBounds().y <= miny) {
                iter.remove();
                _target.abortAnimation(sub);
            }
        }
    }

    /** List of existing messages from our chat director. */
    protected HistoryList _history;

    /** If set, show history no matter what the client prefs say. */
    protected boolean _overrideHistory;

    /** The currently displayed subtitles O' history. */
    protected List<ChatGlyph> _showingHistory = Lists.newArrayList();

    /** Our history scrollbar. */
    protected JScrollBar _scrollbar;

    /** Tracks whether or not we've been laid out. */
    protected boolean _laidout;

    /** If we're in history mode, this will be non-null and will notify
     * us of our historical positioning. */
    protected BoundedRangeModel _historyModel = null;

    /** The currently displayed subtitle areas. */
    protected List<ChatGlyph> _subtitles = Lists.newArrayList();

    /** The amount of vertical space to use for subtitles. */
    protected int _subtitleHeight;

    /** If true, subtitles should fill all available height. */
    protected boolean _subtitlesFill;

    /** The amount of space we want around the subtitles. */
    protected int _subtitleXSpacing;
    protected int _subtitleYSpacing;

    /** The unbounded expire time of the last chat glyph displayed. */
    protected long _lastExpire;

    /** If the history offset we've figured is all figured out or needs to be refigured. */
    protected boolean _histOffsetFinal = false;

    /** If true, we're the ones updating the history scrollbar and change
     * events should be ignored. */
    boolean _settingBar = false;

    /** The history offset (from 0) such that the history lines (0, _histOffset - 1) will all fit
     * onscreen if the lowest scrollbar position is _histOffset. */
    protected int _histOffset = 0;

    /** A guess of how many history lines fit onscreen at a time. */
    protected int _historyExtent;

    /** A guess as to the height of a subtitle (plus spacing). */
    protected static final int SUBTITLE_HEIGHT_GUESS = 16;

    /** The amount of space to insert between the icon and the text. */
    protected static final int ICON_PADDING = 4;

    /** The padding in each direction around the text to the edges of a chat 'bubble'. */
    protected static final int PAD = ChatLogic.PAD;
}
