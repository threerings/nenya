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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import com.google.common.collect.Lists;
import com.samskivert.util.RunAnywhere;
import static com.threerings.media.Log.log;
import com.threerings.media.image.ImageUtil;
import com.threerings.media.image.Mirage;
import com.threerings.media.util.MathUtil;
import com.threerings.media.util.Pathable;

/**
 * Extends the base media panel with the notion of a virtual coordinate system. All entities in
 * the virtual media panel have virtual coordinates and the virtual media panel displays a window
 * into that virtual view. The panel can be made to scroll by adjusting the view offset slightly
 * at the start of each tick and it will efficiently copy the unmodified view data and generate
 * repaint requests for the exposed regions.
 */
public class VirtualMediaPanel extends MediaPanel
{
    /** The code for the pathable following mode wherein we keep the view centered on the
     * pathable's location. */
    public static final byte CENTER_ON_PATHABLE = 0;

    /** The code for the pathable following mode wherein we ensure that the marked pathable is
     * always kept within the visible bounds of the view. */
    public static final byte ENCLOSE_PATHABLE = 1;

    /** The code for the pathable following mode wherein we set the upper-left corner of the view
     * to the coordinates of the pathable. */
    public static final byte TRACK_PATHABLE = 2;

    /**
     * Constructs a virtual media panel.
     */
    public VirtualMediaPanel (FrameManager framemgr)
    {
        super(framemgr);

        addZoomListener((oldZoom, newZoom) -> {
            _vbounds = _zoomManager.rescaleBounds(_vbounds, getWidth(), getHeight());
            _metamgr.getRegionManager().addDirtyRegion(_vbounds);
        });
    }

    /**
     * Set a background image to tile the background of the media panel.
     */
    public void setBackground (Mirage background)
    {
        _background = background;
    }

    /**
     * Sets the upper-left coordinate of the view port in virtual coordinates. The view will be as
     * efficient as possible about repainting itself to achieve this new virtual location (meaning
     * that if we need only to move one pixel to the left, it will use {@link Graphics#copyArea}
     * to move our rendered view over one pixel and generate a dirty region for the exposed area).
     * The new location will not take effect until the view is {@link MediaPanel#tick}ed, so only
     * the last call to this method during a tick will have any effect.
     */
    public void setViewLocation (int x, int y)
    {
        // make a note of our new x and y offsets
        _nx = x;
        _ny = y;
    }

    /**
     * Returns the bounds of the viewport in virtual coordinates. The
     * returned rectangle must <em>not</em> be modified.
     */
    @Override
    public Rectangle getViewBounds ()
    {
        return _vbounds;
    }

    /**
     * Adds an entity that will be informed when the view scrolls.
     */
    public void addViewTracker (ViewTracker tracker)
    {
        _trackers.add(tracker);
    }

    /**
     * Removes an entity from the view trackers list.
     */
    public void removeViewTracker (ViewTracker tracker)
    {
        _trackers.remove(tracker);
    }

    /**
     * Instructs the view to follow the supplied pathable; ensuring that the view's coordinates
     * are adjusted according to the follow mode.
     *
     * @param pable the pathable to follow.
     * @param followMode the strategy for keeping the pathable in view.
     */
    public void setFollowsPathable (Pathable pable, byte followMode)
    {
        _fmode = followMode;
        _fpath = pable;
        trackPathable(); // immediately update our location
    }

    /**
     * Clears out the pathable that was being enclosed or followed due to a previous call to
     * {@link #setFollowsPathable}.
     */
    public void clearPathable ()
    {
        _fpath = null;
        _fmode = (byte) -1;
    }

    /**
     * We overload this to translate mouse events into the proper coordinates before they are
     * dispatched to any of the mouse listeners.
     */
    @Override
    protected void processMouseEvent (MouseEvent event)
    {
        event.translatePoint(_nx, _ny);
        _zoomManager.adjustMouseEvent(event, _vbounds);
        super.processMouseEvent(event);
    }

    /**
     * We overload this to translate mouse events into the proper coordinates before they are
     * dispatched to any of the mouse listeners.
     */
    @Override
    protected void processMouseMotionEvent (MouseEvent event)
    {
        event.translatePoint(_nx, _ny);
        _zoomManager.adjustMouseEvent(event, _vbounds);
        super.processMouseMotionEvent(event);
    }

    /**
     * We overload this to translate mouse events into the proper coordinates before they are
     * dispatched to any of the mouse listeners.
     */
    @Override
    protected void processMouseWheelEvent (MouseWheelEvent event)
    {
        event.translatePoint(_nx, _ny);
        _zoomManager.adjustMouseEvent(event, _vbounds);
        super.processMouseWheelEvent(event);
    }

    @Override
    protected void dirtyScreenRect (Rectangle rect)
    {
        // translate the screen rect into happy coordinates
        rect.translate(_nx, _ny);
        rect = _zoomManager.scaleOnCenter(rect);
        _metamgr.getRegionManager().addDirtyRegion(rect);
    }

    @Override
    public void doLayout ()
    {
        super.doLayout();

        // we need to obtain our absolute screen coordinates to work
        // around the Windows copyArea() bug
        findRootBounds();
    }

    @Override
    public void setBounds (int x, int y, int width, int height)
    {
        super.setBounds(x, y, width, height);

        // keep track of the size of the viewport
        _vbounds.width = getWidth();
        _vbounds.height = getHeight();
        _vbounds = _zoomManager.scaleOnCenter(_vbounds);

        // we need to obtain our absolute screen coordinates to work
        // around the Windows copyArea() bug
        findRootBounds();
    }

    @Override
    protected void addObscurerDirtyRegion (Rectangle region)
    {
        // Adjust for any scrolling we're currently doing.
        super.addObscurerDirtyRegion(
                new Rectangle(region.x - (int) _dx, region.y - (int) _dy, region.width, region.height));
    }

    /**
     * Determines the absolute screen coordinates at which this panel is located and stores them
     * for reference later when rendering. This is necessary in order to work around the Windows
     * <code>copyArea()</code> bug.
     */
    protected void findRootBounds ()
    {
        _abounds.setLocation(0, 0);
        FrameManager.getRoot(this, _abounds);
    }

    @Override
    protected void didTick (long tickStamp)
    {
        super.didTick(tickStamp);
        adjustBoundsCenter();
    }

    protected void adjustBoundsCenter ()
    {
        int width = _vbounds.width, height = _vbounds.height;

        // adjusts our view location to track any pathable we might be tracking
        trackPathable();

        // our offset is from the center of the view panel, so we need to adjust
        // coordinates to account for zoom.
        Point viewCenter = ZoomManager.center(_vbounds);
        Point panelCenter = new Point(getWidth() / 2, getHeight() / 2);
        int offsetX = viewCenter.x - panelCenter.x;
        int offsetY = viewCenter.y - panelCenter.y;

        // if we have a new target location, we'll need to generate dirty
        // regions for the area exposed by the scrolling
        if (_nx != offsetX || _ny != offsetY) {
            // determine how far we'll be moving on this tick
            int dx = _nx - offsetX, dy = _ny - offsetY;

//             log.info("Scrolling into place [n=(" + _nx + ", " + _ny +
//                      "), t=(" + _vbounds.x + ", " + _vbounds.y +
//                      "), d=(" + dx + ", " + dy +
//                      "), width=" + width + ", height=" + height + "].");

            _dx += dx * getZoomLevel();
            _dy += dy * getZoomLevel();

            // now go ahead and update our location so that changes in between here and the call
            // to paint() for this tick don't booch everything
            _vbounds.x += dx;
            _vbounds.y += dy;

            // these are used to prevent the vertical strip from
            // overlapping the horizontal strip
            int sy = _vbounds.y, shei = height;

            // and add invalid rectangles for the exposed areas
            if (dy > 0) {
                shei = Math.max(shei - dy, 0);
                _metamgr.getRegionManager().invalidateRegion(_vbounds.x, _vbounds.y + height - dy, width, dy);
            } else if (dy < 0) {
                sy -= dy;
                _metamgr.getRegionManager().invalidateRegion(_vbounds.x, _vbounds.y, width, -dy);
            }
            if (dx > 0) {
                // zoom rounding errors make it so we need to account for potential off-by-one errors
                int dx2 = dx + 1;
                _metamgr.getRegionManager().invalidateRegion(_vbounds.x + width - dx2, sy, dx2, shei);
            } else if (dx < 0) {
                _metamgr.getRegionManager().invalidateRegion(_vbounds.x, sy, -dx, shei);
            }

            addObscurerDirtyRegions(false);

            // let derived classes react if they so desire
            viewLocationDidChange(dx, dy);
        }
    }

    /**
     * Called during our tick when we have adjusted the view location. The {@link #_vbounds} will
     * already have been updated to reflect our new view coordinates.
     *
     * @param dx the delta scrolled in the x direction (in pixels).
     * @param dy the delta scrolled in the y direction (in pixels).
     */
    protected void viewLocationDidChange (int dx, int dy)
    {
        // inform our view trackers
        for (int ii = 0, ll = _trackers.size(); ii < ll; ii++) {
            _trackers.get(ii).viewLocationDidChange(dx, dy);
        }

        // pass the word on to our sprite/anim managers via the meta manager
        _metamgr.viewLocationDidChange(dx, dy);
    }

    /**
     * Implements the standard pathable tracking support. Derived classes may wish to override
     * this if they desire custom tracking functionality.
     */
    protected void trackPathable ()
    {
        // if we're tracking a pathable, adjust our view coordinates
        if (_fpath == null) {
            return;
        }

        int width = _vbounds.width, height = _vbounds.height;
        int nx = _nx, ny = _ny;

        // figure out where to move
        switch (_fmode) {
        case TRACK_PATHABLE:
            nx = _fpath.getX();
            ny = _fpath.getY();
            break;

        case CENTER_ON_PATHABLE:
            nx = _fpath.getX() - width/2;
            ny = _fpath.getY() - height/2;
            break;

        case ENCLOSE_PATHABLE:
            Rectangle bounds = _fpath.getBounds();
            if (nx > bounds.x) {
                nx = bounds.x;
            } else if (nx + width < bounds.x + bounds.width) {
                nx = bounds.x + bounds.width - width;
            }
            if (ny > bounds.y) {
                ny = bounds.y;
            } else if (ny + height < bounds.y + bounds.height) {
                ny = bounds.y + bounds.height - height;
            }
            break;

        default:
            log.warning("Eh? Set to invalid pathable mode", "mode", _fmode);
            break;
        }

//         Log.info("Tracking pathable [mode=" + _fmode +
//                  ", pable=" + _fpath + ", nx=" + nx + ", ny=" + ny + "].");

        setViewLocation(nx, ny);
    }

    @Override
    protected void paint (Graphics2D gfx, Rectangle[] dirty)
    {
        int dx = (int) Math.round(_dx);
        int dy = (int) Math.round(_dy);
        if (dx != 0 || dy != 0) {
            int width = getWidth(), height = getHeight();
            int cx = (dx > 0) ? dx : 0;
            int cy = (dy > 0) ? dy : 0;

            // set the clip to the bounds of the component (we can't assume the clip is anything
            // sensible upon entry to paint() because the frame manager expects us to set our own
            // clip)
            gfx.setClip(0, 0, width, height);

            // on windows, attempting to call copyArea() on a translated graphics context results
            // in boochness; so we have to untranslate the graphics context, do our copyArea() and
            // then translate it back
            if (RunAnywhere.isWindows()) {
                gfx.translate(-_abounds.x, -_abounds.y);
                gfx.copyArea(_abounds.x + cx, _abounds.y + cy,
                        width - Math.abs(dx),
                        height - Math.abs(dy), -dx, -dy);
                gfx.translate(_abounds.x, _abounds.y);
            } else if (RunAnywhere.isMacOS()) {
                try {
                    gfx.copyArea(cx, cy,
                            width - Math.abs(dx),
                            height - Math.abs(dy), -dx, -dy);
                } catch (Exception e) {
                    // HACK when it throws an exception trying to do the copy area, just repaint
                    // everything
                    dirty = new Rectangle[] { new Rectangle(_vbounds) };
                }
            } else {
                gfx.copyArea(cx, cy,
                        width - Math.abs(dx),
                        height - Math.abs(dy), -dx, -dy);
            }

            // and clear out our scroll deltas
            _dx -= dx;
            _dy -= dy;
        }

        AffineTransform originalTransform = gfx.getTransform();

        int centerX = _vbounds.x + _vbounds.width / 2;
        int centerY = _vbounds.y + _vbounds.height / 2;

        // if we zoom too quickly, the zoomManager scale may not be in sync with the paint tick
        // so we check based on the actual view bounds to be safe
        double scale = getWidth() / (double) _vbounds.width;

        // translate into happy space
        gfx.translate(getWidth() / 2, getHeight() / 2);
        gfx.scale(scale, scale);
        gfx.translate(-centerX, -centerY);

        // now do the actual painting
        super.paint(gfx, dirty);

        // translate back out of happy space
        gfx.setTransform(originalTransform);
    }

    @Override
    protected void constrainToBounds (Rectangle dirty)
    {
        SwingUtilities.computeIntersection(
                _vbounds.x, _vbounds.y, _vbounds.width, _vbounds.height, dirty);
    }

    @Override
    protected void paintBehind (Graphics2D gfx, Rectangle dirtyRect)
    {
        // if we have a background image specified, tile it!
        if (_background != null) {
            // make sure it's aligned
            int iw = _background.getWidth();
            int ih = _background.getHeight();
            int lowx = iw * MathUtil.floorDiv(dirtyRect.x, iw);
            int lowy = ih * MathUtil.floorDiv(dirtyRect.y, ih);
            ImageUtil.tileImage(gfx, _background, lowx, lowy,
                dirtyRect.width + (dirtyRect.x - lowx),
                dirtyRect.height + (dirtyRect.y - lowy));
        }
    }

    protected void enableZoom() {
        addMouseWheelListener(_zoomManager.createMouseWheelListener());
    }

    protected void enableZoom(double min, double max) {
        enableZoom();
        setZoomConstraints(min, max);
    }

    protected void setZoomConstraints(double min, double max) {
        _zoomManager.setMinZoomLevel(min);
        _zoomManager.setMaxZoomLevel(max);
    }

    protected void setZoomLevel(double zoom) {
        _zoomManager.setZoomLevel(zoom);
    }

    protected double getZoomLevel() {
        return _zoomManager.getZoomLevel();
    }

    protected void addZoomListener(ZoomManager.ZoomListener listener) {
        _zoomManager.addZoomListener(listener);
    }

    protected void removeZoomListener(ZoomManager.ZoomListener listener) {
        _zoomManager.removeZoomListener(listener);
    }

    protected ZoomManager _zoomManager = new ZoomManager();

    /** Our viewport bounds in virtual coordinates. */
    protected Rectangle _vbounds = new Rectangle();

    /** Our target offsets to be effected on the next tick. */
    protected int _nx, _ny;

    /**
     * Our scroll offsets in real pixels.
     */
    protected double _dx, _dy;

    /** Our tiling background image. */
    protected Mirage _background;

    /** The mode we're using when following a pathable. */
    protected byte _fmode = -1;

    /** The pathable being followed. */
    protected Pathable _fpath;

    /** We need to know our absolute coordinates in order to work around
     * the Windows copyArea() bug. */
    protected Rectangle _abounds = new Rectangle();

    /** A list of entities to be informed when the view scrolls. */
    protected ArrayList<ViewTracker> _trackers = Lists.newArrayList();
}
