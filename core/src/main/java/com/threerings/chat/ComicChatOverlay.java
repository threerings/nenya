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

import java.util.List;
import java.util.Iterator;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JScrollBar;

import com.google.common.collect.Lists;

import com.samskivert.swing.Label;
import com.samskivert.swing.util.SwingUtil;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.media.image.ColorUtil;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.UserMessage;
import com.threerings.crowd.util.CrowdContext;

import static com.threerings.NenyaLog.log;

/**
 * Implements comic chat in the yohoho client.
 */
public class ComicChatOverlay extends SubtitleChatOverlay
{
    /**
     * Construct a comic chat overlay.
     *
     * @param subtitleHeight the amount of vertical space to use for subtitles.
     */
    public ComicChatOverlay (CrowdContext ctx, ChatLogic logic, JScrollBar historyBar,
                             int subtitleHeight)
    {
        super(ctx, logic, historyBar, subtitleHeight);
    }

    @Override
    public void newPlaceEntered (InfoProvider provider)
    {
        _newPlacePoint = _ctx.getChatDirector().getHistory().size();
        super.newPlaceEntered(provider);

        // and clear place-oriented bubbles
        clearBubbles(false);
    }

    @Override
    public void layout ()
    {
        clearBubbles(true); // these will get repopulated from the history
        super.layout();
    }

    @Override
    public void removed ()
    {
        // we do this before calling super because we want our target to
        // be around for the bubble clearing
        clearBubbles(true);

        super.removed();
    }

    @Override
    public void clear ()
    {
        super.clear();

        clearBubbles(true);
    }

    @Override
    public void viewDidScroll (int dx, int dy)
    {
        super.viewDidScroll(dx, dy);
        viewDidScroll(_bubbles, dx, dy);
    }

    @Override
    public void setDimmed (boolean dimmed)
    {
        super.setDimmed(dimmed);
        updateDimmed(_bubbles);
    }

    @Override
    public void speakerDeparted (Name speaker)
    {
        for (Iterator<BubbleGlyph> iter = _bubbles.iterator(); iter.hasNext();) {
            BubbleGlyph rec = iter.next();
            if (rec.isSpeaker(speaker)) {
                _target.abortAnimation(rec);
                iter.remove();
            }
        }
    }

    @Override
    public void historyUpdated (int adjustment)
    {
        _newPlacePoint -= adjustment;
        super.historyUpdated(adjustment);
    }

    /**
     * Clear chat bubbles, either all of them or just the place-oriented ones.
     */
    protected void clearBubbles (boolean all)
    {
        for (Iterator<BubbleGlyph> iter = _bubbles.iterator(); iter.hasNext();) {
            ChatGlyph rec = iter.next();
            if (all || isPlaceOrientedType(rec.getType())) {
                _target.abortAnimation(rec);
                iter.remove();
            }
        }
    }

    @Override
    protected boolean shouldShowFromHistory (ChatMessage msg, int index)
    {
        // only show if the message was received since we last entered
        // a new place, or if it's place-less chat.
        return ((index >= _newPlacePoint) ||
                (! isPlaceOrientedType(getType(msg, false))));
    }

    @Override
    protected boolean isApprovedLocalType (String localtype)
    {
        if (ChatCodes.PLACE_CHAT_TYPE.equals(localtype) ||
            ChatCodes.USER_CHAT_TYPE.equals(localtype)) {
            return true;
        }
        log.debug("Ignoring non-standard system/feedback chat", "localtype", localtype);
        return false;
    }

    /**
     * Is the type of chat place-oriented.
     */
    protected boolean isPlaceOrientedType (int type)
    {
        return (ChatLogic.placeOf(type)) == ChatLogic.PLACE;
    }

    @Override
    protected void displayMessage (ChatMessage message, int type, Graphics2D layoutGfx)
    {
        // we might need to modify the textual part with translations,
        // but we can't do that to the message object, since other chatdisplays also get it.
        String text = message.message;
        switch (ChatLogic.placeOf(type)) {
        case ChatLogic.INFO:
        case ChatLogic.ATTENTION:
            if (createBubble(layoutGfx, type, message.timestamp, text, null, null)) {
                return; // EXIT;
            }
            // if the bubble didn't fit (unlikely), make it a subtitle
            break;

        case ChatLogic.PLACE: {
            UserMessage msg = (UserMessage) message;
            Point speakerloc = _provider.getSpeaker(msg.speaker);
            if (speakerloc == null) {
                log.warning("ChatOverlay.InfoProvider doesn't know the speaker!",
                    "speaker", msg.speaker, "type", type);
                return;
            }

            // emotes won't actually have tails, but we do want them to appear near the pirate
            if (ChatLogic.modeOf(type) == ChatLogic.EMOTE) {
                text = xlate(
                    MessageBundle.tcompose("m.emote_format", msg.getSpeakerDisplayName())) +
                    " " + text;
            }

            // try to add all the text as a bubble, but if it doesn't
            // fit, add some of it and 'continue' the rest in a subtitle.
            String leftover = text;
            for (int ii = 1; ii < 7; ii++) {
                String bubtext = splitNear(text, text.length() / ii);
                if (createBubble(layoutGfx, type, msg.timestamp,
                    bubtext + ((ii > 1) ? "..." : ""), msg.speaker, speakerloc)) {

                    leftover = text.substring(bubtext.length());
                    break;
                }
            }

            if (leftover.length() > 0 && !isHistoryMode()) {
                String ltext = MessageBundle.tcompose("m.continue_format", msg.speaker);
                ltext = xlate(ltext) + " \"" + leftover + "\"";
                addSubtitle(createSubtitle(layoutGfx, ChatLogic.CONTINUATION,
                    message.timestamp, null, 0, ltext, true));
            }
            return; // EXIT
        }
        }

        super.displayMessage(message, type, layoutGfx);
    }

    /**
     * Split the text at the space nearest the specified location.
     */
    protected String splitNear (String text, int pos)
    {
        if (pos >= text.length()) {
            return text;
        }

        int forward = text.indexOf(' ', pos);
        int backward = text.lastIndexOf(' ', pos);

        int newpos = (Math.abs(pos - forward) < Math.abs(pos - backward)) ? forward : backward;

        // if we couldn't find a decent place to split, just do it wherever
        if (newpos == -1) {
            newpos = pos;

        } else {
            // actually split the space onto the first part
            newpos++;
        }
        return text.substring(0, newpos);
    }

    /**
     * Create a chat bubble with the specified type and text.
     *
     * @param speakerloc if non-null, specifies that a tail should be added which points to that
     * location.
     * @return true if we successfully laid out the bubble
     */
    protected boolean createBubble (
        Graphics2D gfx, int type, long timestamp, String text, Name speaker, Point speakerloc)
    {
        Label label = layoutText(gfx, _logic.getFont(type), text);
        label.setAlignment(Label.CENTER);
        gfx.dispose();

        // get the size of the new bubble
        Rectangle r = getBubbleSize(type, label.getSize());

        // get the user's old bubbles.
        List<BubbleGlyph> oldbubs = getAndExpireBubbles(speaker);
        int numold = oldbubs.size();

        Rectangle placer, bigR = null;
        if (numold == 0) {
            placer = new Rectangle(r);
            positionRectIdeally(placer, type, speakerloc);

        } else {
            // get a big rectangle encompassing the old and new
            bigR = getRectWithOlds(r, oldbubs);
            placer = new Rectangle(bigR);

            positionRectIdeally(placer, type, speakerloc);
            // we actually try to place midway between ideal and old
            // and adjust up half the height of the new boy
            placer.setLocation((placer.x + bigR.x) / 2,
                               (placer.y + (bigR.y - (r.height / 2))) / 2);
        }

        // then look for a place nearby where it will fit
        // (making sure we only put it in the area above the subtitles)
        Rectangle vbounds = new Rectangle(_target.getViewBounds());
        vbounds.height -= _subtitleHeight;
        if (!SwingUtil.positionRect(placer, vbounds, getAvoidList(speaker))) {
            // we couldn't fit the bubble!
            return false;
        }

        // now 'placer' is positioned reasonably.
        if (0 == numold) {
            r.setLocation(placer.x, placer.y);

        } else {
            int dx = placer.x - bigR.x;
            int dy = placer.y - bigR.y;
            for (int ii=0; ii < numold; ii++) {
                BubbleGlyph bub = oldbubs.get(ii);
                bub.removeTail();
                Rectangle ob = bub.getBubbleBounds();
                // recenter the translated bub within placer's width..
                int xadjust = dx - (ob.x - bigR.x) +
                    (placer.width - ob.width) / 2;
                bub.translate(xadjust, dy);
            }

            // and position 'r' in the right place relative to 'placer'
            r.setLocation(placer.x + (placer.width - r.width) / 2,
                          placer.y + placer.height - r.height);
        }

        Shape shape = getBubbleShape(type, r);
        Shape full = shape;

        // if we have a tail, the full area should include that.
        if (speakerloc != null) {
            Area area = new Area(getTail(type, r, speakerloc));
            area.add(new Area(shape));
            full = area;
        }

        // finally, add the bubble
        long lifetime = getChatExpire(timestamp, label.getText())-timestamp;
        BubbleGlyph newbub = new BubbleGlyph(
            this, type, lifetime, full, label, adjustLabel(type, r.getLocation()), shape,
            speaker, _logic.getOutlineColor(type));
        newbub.setDim(_dimmed);
        _bubbles.add(newbub);
        _target.addAnimation(newbub);

        // and we need to dirty all the bubbles because they'll all be painted in slightly
        // different colors
        int numbubs = _bubbles.size();
        for (int ii=0; ii < numbubs; ii++) {
            _bubbles.get(ii).setAgeLevel(numbubs - ii - 1);
        }

        return true; // success!
    }

    /**
     * Calculate the size of the chat bubble based on the dimensions of the label and the type of
     * chat. It will be turned into a shape later, but we manipulate it for a while as just a
     * rectangle (which are easier to move about and do intersection tests with, and besides the
     * Shape interface has no way to translate).
     */
    protected Rectangle getBubbleSize (int type, Dimension d)
    {
        switch (ChatLogic.modeOf(type)) {
        case ChatLogic.SHOUT:
        case ChatLogic.THINK:
        case ChatLogic.EMOTE:
            // extra room for these two monsters
            return new Rectangle(d.width + (PAD * 4), d.height + (PAD * 4));

        default:
            return new Rectangle(d.width + (PAD * 2), d.height + (PAD * 2));
        }
    }

    /**
     * Position the label based on the type.
     */
    protected Point adjustLabel (int type, Point labelpos)
    {
        switch (ChatLogic.modeOf(type)) {
        case ChatLogic.SHOUT:
        case ChatLogic.EMOTE:
        case ChatLogic.THINK:
            labelpos.translate(PAD * 2, PAD * 2);
            break;

        default:
            labelpos.translate(PAD, PAD);
            break;
        }

        return labelpos;
    }

    /**
     * Position the rectangle in its ideal location given the type and speaker positon (which may
     * be null).
     */
    protected void positionRectIdeally (Rectangle r, int type, Point speaker)
    {
        if (speaker != null) {
            // center it on top of the speaker (it'll be moved..)
            r.setLocation(speaker.x - (r.width / 2),
                          speaker.y - (r.height / 2));
            return;
        }

        // otherwise we have different areas for different types
        Rectangle vbounds = _target.getViewBounds();
        switch (ChatLogic.placeOf(type)) {
        case ChatLogic.INFO:
        case ChatLogic.ATTENTION:
            // upper left
            r.setLocation(vbounds.x + BUBBLE_SPACING,
                          vbounds.y + BUBBLE_SPACING);
            return;

        case ChatLogic.PLACE:
            log.warning("Got to a place where I shouldn't get!");
            break; // fall through
        }

        // put it in the center..
        log.debug("Unhandled chat type in getLocation()", "type", type);
        r.setLocation((vbounds.width - r.width) / 2,
                      (vbounds.height - r.height) / 2);
    }

    /**
     * Get a rectangle based on the old bubbles, but with room for the new one.
     */
    protected Rectangle getRectWithOlds (Rectangle r, List<BubbleGlyph> oldbubs)
    {
        int n = oldbubs.size();
        // if no old bubs, just return the new one.
        if (n == 0) {
            return r;
        }

        // otherwise, encompass all the oldies
        Rectangle bigR = null;
        for (int ii=0; ii < n; ii++) {
            BubbleGlyph bub = oldbubs.get(ii);
            if (ii == 0) {
                bigR = bub.getBubbleBounds();
            } else {
                bigR = bigR.union(bub.getBubbleBounds());
            }
        }

        // and add space for the new boy
        bigR.width = Math.max(bigR.width, r.width);
        bigR.height += r.height;

        return bigR;
    }

    /**
     * Get the appropriate shape for the specified type of chat.
     */
    protected Shape getBubbleShape (int type, Rectangle r)
    {
        switch (ChatLogic.placeOf(type)) {
        case ChatLogic.INFO:
        case ChatLogic.ATTENTION:
            // boring rectangle wrapped in an Area for translation
            return new Area(r);
        }

        switch (ChatLogic.modeOf(type)) {
        case ChatLogic.SPEAK:
            // a rounded rectangle balloon, put in an Area so that it's
            // translatable
            return new Area(new RoundRectangle2D.Float(
                r.x, r.y, r.width, r.height, PAD * 4, PAD * 4));

        case ChatLogic.SHOUT: {
            // spikey balloon
            Polygon left = new Polygon(), right = new Polygon();
            Polygon top = new Polygon(), bot = new Polygon();

            int x = r.x + PAD;
            int y = r.y + PAD;
            int wid = r.width - PAD * 2;
            int hei = r.height - PAD * 2;
            Area a = new Area(new Rectangle(x, y, wid, hei));
            int spikebase = 10;
            int cornbase = spikebase*3/4;

            // configure spikes to the left and right sides
            left.addPoint(x, y);
            left.addPoint(x - PAD, y + spikebase/2);
            left.addPoint(x, y + spikebase);
            right.addPoint(x + wid, y);
            right.addPoint(x + wid + PAD, y + spikebase/2);
            right.addPoint(x + wid, y + spikebase);

            // add the left and right side spikes
            int ypos = 0;
            int ahei = hei - cornbase;
            int maxpos = ahei - spikebase + 1;
            int numvert = (int) Math.ceil(ahei / ((float) spikebase));
            for (int ii=0; ii < numvert; ii++) {
                int newpos = cornbase/2 +
                    Math.min((ahei * ii) / numvert, maxpos);
                left.translate(0, newpos - ypos);
                right.translate(0, newpos - ypos);
                a.add(new Area(left));
                a.add(new Area(right));
                ypos = newpos;
            }

            // configure spikes for the top and bottom
            top.addPoint(x, y);
            top.addPoint(x + spikebase/2, y - PAD);
            top.addPoint(x + spikebase, y);
            bot.addPoint(x, y + hei);
            bot.addPoint(x + spikebase/2, y + hei + PAD);
            bot.addPoint(x + spikebase, y + hei);

            // add top and bottom spikes
            int xpos = 0;
            int awid = wid - cornbase;
            maxpos = awid - spikebase + 1;
            int numhorz = (int) Math.ceil(awid / ((float) spikebase));
            for (int ii=0; ii < numhorz; ii++) {
                int newpos = cornbase/2 +
                    Math.min((awid * ii) / numhorz, maxpos);
                top.translate(newpos - xpos, 0);
                bot.translate(newpos - xpos, 0);
                a.add(new Area(top));
                a.add(new Area(bot));
                xpos = newpos;
            }

            // and lets also add corner spikes
            Polygon corner = new Polygon();
            corner.addPoint(x, y + cornbase);
            corner.addPoint(x - PAD + 2, y - PAD + 2);
            corner.addPoint(x + cornbase, y);
            a.add(new Area(corner));

            corner.reset();
            corner.addPoint(x + wid - cornbase, y);
            corner.addPoint(x + wid + PAD - 2, y - PAD + 2);
            corner.addPoint(x + wid, y + cornbase);
            a.add(new Area(corner));

            corner.reset();
            corner.addPoint(x + wid, y + hei - cornbase);
            corner.addPoint(x + wid + PAD - 2, y + hei + PAD - 2);
            corner.addPoint(x + wid - cornbase, y + hei);
            a.add(new Area(corner));

            corner.reset();
            corner.addPoint(x + cornbase, y + hei);
            corner.addPoint(x - PAD + 2, y + hei + PAD - 2);
            corner.addPoint(x, y + hei - cornbase);
            a.add(new Area(corner));
            // grunt work!

            return a;
        }

        case ChatLogic.EMOTE: {
            // a box that curves inward on all sides
            Area a = new Area(r);
            a.subtract(new Area(new Ellipse2D.Float(r.x, r.y - PAD, r.width, PAD * 2)));
            a.subtract(new Area(new Ellipse2D.Float(r.x, r.y + r.height - PAD, r.width, PAD * 2)));
            a.subtract(new Area(new Ellipse2D.Float(r.x - PAD, r.y, PAD * 2, r.height)));
            a.subtract(new Area(new Ellipse2D.Float(r.x + r.width - PAD, r.y, PAD * 2, r.height)));
            return a;
        }

        case ChatLogic.THINK: {
            // cloudy balloon!
            int x = r.x + PAD;
            int y = r.y + PAD;
            int wid = r.width - PAD * 2;
            int hei = r.height - PAD * 2;
            Area a = new Area(new Rectangle(x, y, wid, hei));

            // small circles on the left and right
            int dia = 12;
            int numvert = (int) Math.ceil(hei / ((float) dia));
            int leftside = x - dia/2;
            int rightside =  x + wid - (dia/2) - 1;
            int maxh = hei - dia;
            for (int ii=0; ii < numvert; ii++) {
                int ypos = y + Math.min((hei * ii) / numvert, maxh);
                a.add(new Area(new Ellipse2D.Float(leftside, ypos, dia, dia)));
                a.add(new Area(new Ellipse2D.Float(rightside, ypos, dia, dia)));
            }

            // larger ovals on the top and bottom
            dia = 16;
            int numhorz = (int) Math.ceil(wid / ((float) dia));
            int topside = y - dia/3;
            int botside = y + hei - (dia/3) - 1;
            int maxw = wid - dia;
            for (int ii=0; ii < numhorz; ii++) {
                int xpos = x + Math.min((wid * ii) / numhorz, maxw);
                a.add(new Area(new Ellipse2D.Float(xpos, topside, dia, dia*2/3)));
                a.add(new Area(new Ellipse2D.Float(xpos, botside, dia, dia*2/3)));
            }

            return a;
        }
        }

        // fall back to subtitle shape
        return _logic.getSubtitleShape(type, r, r);
    }

    /**
     * Create a tail to the specified rectangular area from the speaker point.
     */
    protected Shape getTail (int type, Rectangle r, Point speaker)
    {
        // emotes don't actually have tails
        if (ChatLogic.modeOf(type) == ChatLogic.EMOTE) {
            return new Area(); // empty shape
        }

        int midx = r.x + (r.width / 2);
        int midy = r.y + (r.height / 2);

        // we actually want to start about SPEAKER_DISTANCE away from the
        // speaker
        int xx = speaker.x - midx;
        int yy = speaker.y - midy;
        float dist = (float) Math.sqrt(xx * xx + yy * yy);
        float perc = (dist - SPEAKER_DISTANCE) / dist;

        if (ChatLogic.modeOf(type) == ChatLogic.THINK) {
            int steps = Math.max((int) (dist / SPEAKER_DISTANCE), 2);
            float step = perc / steps;
            Area a = new Area();
            for (int ii=0; ii < steps; ii++, perc -= step) {
                int radius = Math.min(SPEAKER_DISTANCE / 2 - 1, ii + 2);
                a.add(new Area(new Ellipse2D.Float(
                  (int) ((1 - perc) * midx + perc * speaker.x) + perc * radius,
                  (int) ((1 - perc) * midy + perc * speaker.y) + perc * radius,
                  radius * 2, radius * 2)));
            }

            return a;
        }

        // ELSE draw a triangular tail shape
        Polygon p = new Polygon();
        p.addPoint((int) ((1 - perc) * midx + perc * speaker.x),
                   (int) ((1 - perc) * midy + perc * speaker.y));

        if (Math.abs(speaker.x - midx) > Math.abs(speaker.y - midy)) {
            int x;
            if (midx > speaker.x) {
                x = r.x + PAD;
            } else {
                x = r.x + r.width - PAD;
            }
            p.addPoint(x, midy - (TAIL_WIDTH / 2));
            p.addPoint(x, midy + (TAIL_WIDTH / 2));

        } else {
            int y;
            if (midy > speaker.y) {
                y = r.y + PAD;
            } else {
                y = r.y + r.height - PAD;
            }
            p.addPoint(midx - (TAIL_WIDTH / 2), y);
            p.addPoint(midx + (TAIL_WIDTH / 2), y);
        }

        return p;
    }

    /**
     * Expire a bubble, if necessary, and return the old bubbles for the specified speaker.
     */
    protected List<BubbleGlyph> getAndExpireBubbles (Name speaker)
    {
        int num = _bubbles.size();

        // first, get all the old bubbles belonging to the user
        List<BubbleGlyph> oldbubs = Lists.newArrayList();
        if (speaker != null) {
            for (int ii=0; ii < num; ii++) {
                BubbleGlyph bub = _bubbles.get(ii);
                if (bub.isSpeaker(speaker)) {
                    oldbubs.add(bub);
                }
            }
        }

        // see if we need to expire this user's oldest bubble
        if (oldbubs.size() >= MAX_BUBBLES_PER_USER) {
            BubbleGlyph bub = oldbubs.remove(0);
            _bubbles.remove(bub);
            _target.abortAnimation(bub);

            // or some other old bubble
        } else if (num >= MAX_BUBBLES) {
            _target.abortAnimation(_bubbles.remove(0));
        }

        // return the speaker's old bubbles
        return oldbubs;
    }

    @Override
    protected void glyphExpired (ChatGlyph glyph)
    {
        super.glyphExpired(glyph);
        _bubbles.remove(glyph);
    }

    /**
     * Get a label formatted as close to the golden ratio as possible for the specified text and
     * given the standard padding we use on all bubbles.
     */
    protected Label layoutText (Graphics2D gfx, Font font, String text)
    {
        Label label = _logic.createLabel(text);
        label.setFont(font);

        // layout in one line
        Rectangle vbounds = _target.getViewBounds();
        label.setTargetWidth(vbounds.width - PAD * 2);
        label.layout(gfx);
        Dimension d = label.getSize();

        // if the label is wide enough, try to split the text into multiple
        // lines
        if (d.width > MINIMUM_SPLIT_WIDTH) {
            int targetheight = getGoldenLabelHeight(d);
            if (targetheight > 1) {
                label.setTargetHeight(targetheight * d.height);
                label.layout(gfx);
            }
        }
        return label;
    }

    /**
     * Given the specified label dimensions, attempt to find the height that will give us the
     * width/height ratio that is closest to the golden ratio.
     */
    protected int getGoldenLabelHeight (Dimension d)
    {
        // compute the ratio of the one line (addin' the paddin')
        double lastratio = ((double) d.width + (PAD * 2)) /
                           ((double) d.height + (PAD * 2));

        // now try increasing the # of lines and seeing if we get closer to the golden ratio
        for (int lines=2; true; lines++) {
            double ratio = ((double) (d.width / lines) + (PAD * 2)) /
                               ((double) (d.height * lines) + (PAD * 2));
            if (Math.abs(ratio - GOLDEN) < Math.abs(lastratio - GOLDEN)) {
                // we're getting closer
                lastratio = ratio;
            } else {
                // we're getting further away, the last one was the one we want
                return lines - 1;
            }
        }
    }

    /**
     * Return a list of rectangular areas that we should avoid while laying out a bubble for the
     * specified speaker.
     */
    protected List<Shape> getAvoidList (Name speaker)
    {
        List<Shape> avoid = Lists.newArrayList();
        if (_provider == null) {
            return avoid;
        }

        // for now we don't accept low-priority avoids
        _provider.getAvoidables(speaker, avoid, null);

        // add the existing chatbub non-tail areas from other speakers
        for (BubbleGlyph bub : _bubbles) {
            if (!bub.isSpeaker(speaker)) {
                avoid.add(bub.getBubbleTerritory());
            }
        }

        return avoid;
    }

    @Override
    protected int getDisplayDurationOffset ()
    {
        return 0; // we don't do any funny hackery, unlike our super class
    }

    /**
     * A glyph of a particlar chat bubble
     */
    protected static class BubbleGlyph extends ChatGlyph
    {
        /**
         * Construct a chat bubble glyph.
         *
         * @param sansTail the chat bubble shape without the tail.
         */
        public BubbleGlyph (
            SubtitleChatOverlay owner, int type, long lifetime, Shape shape, Label label,
            Point labelpos, Shape sansTail, Name speaker, Color outline) {
            super(owner, type, lifetime, shape.getBounds(), shape, null, null,
                label, labelpos, outline);

            _sansTail = sansTail;
            _speaker = speaker;
        }

        public void setAgeLevel (int agelevel) {
            _agelevel = agelevel;
            invalidate();
        }

        @Override
        public void viewDidScroll (int dx, int dy) {
            // only system info and attention messages remain fixed, all others scroll
            if ((_type == ChatLogic.INFO) || (_type == ChatLogic.ATTENTION)) {
                translate(dx, dy);
            }
        }

        @Override
        protected Color getBackground () {
            if (_background == Color.WHITE) {
                return BACKGROUNDS[_agelevel];
            } else {
                return _background;
            }
        }

        /**
         * Get the screen real estate that this bubble has reserved and doesn't want to let any
         * other bubbles take.
         */
        public Shape getBubbleTerritory () {
            Rectangle bounds = getBubbleBounds();
            bounds.grow(BUBBLE_SPACING, BUBBLE_SPACING);
            return bounds;
        }

        /**
         * Get the bounds of this bubble, sans tail space.
         */
        public Rectangle getBubbleBounds () {
            return _sansTail.getBounds();
        }

        /**
         * Is the specified player the speaker of this bubble?
         */
        public boolean isSpeaker (Name player) {
            return (_speaker != null) && _speaker.equals(player);
        }

        /**
         * Remove the tail on this bubble, if any.
         */
        public void removeTail () {
            invalidate();
            _shape = _sansTail;
            _bounds = _shape.getBounds();
            jiggleBounds();
            invalidate();
        }

        /** The shape of this chat bubble, without the tail. */
        protected Shape _sansTail;

        /** The name of the speaker. */
        protected Name _speaker;

        /** The age level of the bubble, used to pick the background color. */
        protected int _agelevel = 0;
    }

    /** The place in our history at which we last entered a new place. */
    protected int _newPlacePoint = 0;

    /** The currently displayed bubble areas. */
    protected List<BubbleGlyph> _bubbles = Lists.newArrayList();

    /** The minimum width of a bubble's label before we consider splitting lines. */
    protected static final int MINIMUM_SPLIT_WIDTH = 90;

    /** The golden ratio. */
    protected static final double GOLDEN = (1.0d + Math.sqrt(5.0d)) / 2.0d;

    /** The space we force between adjacent bubbles. */
    protected static final int BUBBLE_SPACING = 15;

    /** The distance to stay from the speaker. */
    protected static final int SPEAKER_DISTANCE = 20;

    /** The width of the end of the tail. */
    protected static final int TAIL_WIDTH = 12;

    /** The maximum number of bubbles to show. */
    protected static final int MAX_BUBBLES = 8;

    /** The maximum number of bubbles to show per user. */
    protected static final int MAX_BUBBLES_PER_USER = 3;

    /** The background colors to use when drawing bubbles. */
    protected static final Color[] BACKGROUNDS = new Color[MAX_BUBBLES];
    static {
        Color yellowy = new Color(0xdd, 0xdd, 0x6a);
        Color blackish = new Color(0xcccccc);

        float steps = (MAX_BUBBLES - 1) / 2;
        for (int ii=0; ii < MAX_BUBBLES / 2; ii++) {
            BACKGROUNDS[ii] = ColorUtil.blend(Color.white, yellowy, (steps - ii) / steps);
        }
        for (int ii= MAX_BUBBLES / 2; ii < MAX_BUBBLES; ii++) {
            BACKGROUNDS[ii] = ColorUtil.blend(blackish, yellowy, (ii - steps) / steps);
        }
    }
}
