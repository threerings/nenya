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

package com.threerings.media;

import java.util.Comparator;
import java.util.List;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;

import com.google.common.collect.Lists;

import com.samskivert.util.ObserverList;
import com.samskivert.util.SortableArrayList;
import com.samskivert.util.Tuple;
import com.samskivert.util.ObserverList.ObserverOp;

import static com.threerings.media.Log.log;

/**
 * Manages, ticks, and paints {@link AbstractMedia}.
 */
public abstract class AbstractMediaManager
    implements MediaConstants
{
    /**
     * Provides this media manager with its host and region manager.
     */
    public void init (MediaHost host, RegionManager remgr)
    {
        _host = host;
        _remgr = remgr;
    }

    /**
     * Returns region manager in use by this manager.
     */
    public RegionManager getRegionManager ()
    {
        return _remgr;
    }

    /**
     * Creates a graphics that can be used to compute metrics or whatever else a media might need.
     * The caller should call {@link Graphics#dispose} on the returned object when it is done
     * with it.
     */
    public Graphics2D createGraphics ()
    {
        return _host.createGraphics();
    }

    /**
     * Must be called every frame so that the media can be properly updated.
     */
    public void tick (long tickStamp)
    {
        _tickStamp = tickStamp;
        tickAllMedia(tickStamp);
        dispatchNotifications();
        // we clear our tick stamp when we're about to be painted, this lets us handle situations
        // when yet more media is slipped in between our being ticked and our being painted
    }

    /**
     * This will always be called prior to the {@link #paint} calls for a particular tick. Because
     * it is possible that there will be no dirty regions and thus no calls to {@link #paint} this
     * method exists so that the media manager can guarantee that it will be notified when all
     * ticking is complete and the painting phase has begun.
     */
    public void willPaint ()
    {
        // now that we're done ticking, we can safely clear this
        _tickStamp = 0;
    }

    /**
     * Renders all registered media in the given layer that intersect the supplied clipping
     * rectangle to the given graphics context.
     *
     * @param layer the layer to render; one of {@link #FRONT}, {@link #BACK}, or {@link #ALL}.
     * The front layer contains all animations with a positive render order; the back layer
     * contains all animations with a negative render order; all, both.
     */
    public void paint (Graphics2D gfx, int layer, Shape clip)
    {
        for (int ii = 0, nn = _media.size(); ii < nn; ii++) {
            AbstractMedia media = _media.get(ii);
            int order = media.getRenderOrder();
            try {
                if (((layer == ALL) || (layer == FRONT && order >= 0) ||
                     (layer == BACK && order < 0)) && clip.intersects(media.getBounds())) {
                    media.paint(gfx);
                }

            } catch (Exception e) {
                log.warning("Failed to render media", "media", media, e);
            }
        }
    }

    /**
     * If the manager is paused for some length of time, it should be fast forwarded by the
     * appropriate number of milliseconds.  This allows media to smoothly pick up where they left
     * off rather than abruptly jumping into the future, thinking that some outrageous amount of
     * time passed since their last tick.
     */
    public void fastForward (long timeDelta)
    {
        if (_tickStamp > 0) {
            log.warning("Egads! Asked to fastForward() during a tick.", new Exception());
        }

        for (int ii = 0, nn = _media.size(); ii < nn; ii++) {
            _media.get(ii).fastForward(timeDelta);
        }
    }

    /**
     * Returns true if the specified media is being managed by this media manager.
     */
    public boolean isManaged (AbstractMedia media)
    {
        return _media.contains(media);
    }

    /**
     * Called by a {@link VirtualMediaPanel} when the view that contains our media is scrolled.
     *
     * @param dx the scrolled distance in the x direction (in pixels).
     * @param dy the scrolled distance in the y direction (in pixels).
     */
    public void viewLocationDidChange (int dx, int dy)
    {
        // let our media know
        for (int ii = 0, ll = _media.size(); ii < ll; ii++) {
            _media.get(ii).viewLocationDidChange(dx, dy);
        }
    }

    /**
     * Called by a {@link AbstractMedia} when its render order has changed.
     */
    public void renderOrderDidChange (AbstractMedia media)
    {
        if (_tickStamp > 0) {
            log.warning("Egads! Render order changed during a tick.", new Exception());
        }

        _media.remove(media);
        _media.insertSorted(media, RENDER_ORDER);
    }

    /**
     * Calls {@link AbstractMedia#tick} on all media to give them a chance to move about, change
     * their look, generate dirty regions, and so forth.
     */
    protected void tickAllMedia (long tickStamp)
    {
        // we use _tickpos so that it can be adjusted if media is added or removed during the tick
        // dispatch
        for (_tickpos = 0; _tickpos < _media.size(); _tickpos++) {
            tickMedia(_media.get(_tickpos), tickStamp);
        }
        _tickpos = -1;
    }

    /**
     * Inserts the specified media into this manager, return true on success.
     */
    protected boolean insertMedia (AbstractMedia media)
    {
        if (_media.contains(media)) {
            log.warning("Attempt to insert media more than once [media=" + media + "].",
                        new Exception());
            return false;
        }

        media.init(this);
        int ipos = _media.insertSorted(media, RENDER_ORDER);

        // if we've started our tick but have not yet painted our media, we need to take care that
        // this newly added media will be ticked before our upcoming render
        if (_tickStamp > 0L) {
            if (_tickpos == -1) {
                // if we're done with our own call to tick(), we need to tick this new media
                tickMedia(media, _tickStamp);
            } else if (ipos <= _tickpos) {
                // otherwise, we're in the middle of our call to tick() and we only need to tick
                // this guy if he's being inserted before our current tick position (if he's
                // inserted after our current position, we'll get to him as part of this tick
                // iteration)
                _tickpos++;
                tickMedia(media, _tickStamp);
            }
        }

        return true;
    }

    /** A helper function used to call {@link AbstractMedia#tick}. */
    protected final void tickMedia (AbstractMedia media, long tickStamp)
    {
        if (media._firstTick == 0L) {
            media.willStart(tickStamp);
        }
        media.tick(tickStamp);
    }

    /**
     * Removes the specified media from this manager, return true on success.
     */
    protected boolean removeMedia (AbstractMedia media)
    {
        int mpos = _media.indexOf(media);
        if (mpos != -1) {
            _media.remove(mpos);
            media.invalidate();
            media.shutdown();
            // if we're in the middle of ticking, we need to adjust the _tickpos if necessary
            if (mpos <= _tickpos) {
                _tickpos--;
            }
            return true;
        }
        log.warning("Attempt to remove media that wasn't inserted [media=" + media + "].");
        return false;
    }

    /**
     * Clears all media from the manager and calls {@link AbstractMedia#shutdown} on each. This
     * does not invalidate the media's vacated bounds; it is assumed that it will be ok.
     */
    protected void clearMedia ()
    {
        if (_tickStamp > 0) {
            log.warning("Egads! Requested to clearMedia() during a tick.", new Exception());
        }

        for (int ii = _media.size() - 1; ii >= 0; ii--) {
            _media.remove(ii).shutdown();
        }
    }

    /**
     * Queues the notification for dispatching after we've ticked all the media.
     */
    public void queueNotification (ObserverList<Object> observers, ObserverOp<Object> event)
    {
        _notify.add(new Tuple<ObserverList<Object>,ObserverOp<Object>>(observers, event));
    }

    /** Type safety jockeying. */
    protected abstract SortableArrayList<? extends AbstractMedia> createMediaList ();

    /**
     * Dispatches all queued media notifications.
     */
    protected void dispatchNotifications ()
    {
        for (int ii = 0, nn = _notify.size(); ii < nn; ii++) {
            Tuple<ObserverList<Object>,ObserverOp<Object>> tuple = _notify.get(ii);
            tuple.left.apply(tuple.right);
        }
        _notify.clear();
    }

    /** Used to sort media by render order. */
    protected static final Comparator<AbstractMedia> RENDER_ORDER =
        new Comparator<AbstractMedia>() {
        public int compare (AbstractMedia am1, AbstractMedia am2) {
            return am1.renderCompareTo(am2);
        }
    };

    /** The media host we're working with. */
    protected MediaHost _host;

    /** The region manager. */
    protected RegionManager _remgr;

    /** List of observers to notify at the end of the tick. */
    protected List<Tuple<ObserverList<Object>, ObserverOp<Object>>> _notify = Lists.newArrayList();

    /** Our render-order sorted list of media. */
    @SuppressWarnings("unchecked") protected SortableArrayList<AbstractMedia> _media =
        (SortableArrayList<AbstractMedia>)createMediaList();

    /** The position in our media list that we're ticking (while in the middle of a call to {@link
     * #tick}) otherwise -1. */
    protected int _tickpos = -1;

    /** The tick stamp if the manager is in the midst of a call to {@link #tick}, else 0. */
    protected long _tickStamp;
}
