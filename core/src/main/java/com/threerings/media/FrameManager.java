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

import java.applet.Applet;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Window;

import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.RepaintManager;

import com.samskivert.util.ListUtil;
import com.samskivert.util.RunAnywhere;
import com.samskivert.util.StringUtil;

import com.samskivert.swing.RuntimeAdjust;

import com.threerings.util.unsafe.Unsafe;

import com.threerings.media.timer.CalibratingTimer;
import com.threerings.media.timer.MediaTimer;
import com.threerings.media.timer.MillisTimer;
import com.threerings.media.util.TrailingAverage;

import static com.threerings.media.Log.log;

/**
 * Provides a central point from which the computation for each "frame" or tick can be dispatched.
 * This assumed that the application structures its activity around the rendering of each frame,
 * which is a common architecture for games. The animation and sprite support provided by other
 * classes in this package are structured for use in an application that uses a frame manager to
 * tick everything once per frame.
 *
 * <p> The frame manager goes through a simple two part procedure every frame:
 *
 * <ul>
 * <li> Ticking all of the frame participants: in {@link FrameParticipant#tick}, any processing
 * that need be performed during this frame should be performed. Care should be taken not to
 * execute code that will take unduly long, instead such processing should be broken up so that it
 * can be performed in small pieces every frame (or performed on a separate thread with the results
 * safely communicated back to the frame participants for incorporation into the rendering loop).
 *
 * <li> Painting the user interface hierarchy: the top-level component (the frame) is painted (via
 * a call to {@link JRootPane#paint}) into a flip buffer (if supported, an off-screen buffer if
 * not). Updates that were computed during the tick should be rendered in this call to paint. The
 * paint call will propagate down to all components in the UI hierarchy, some of which may be
 * {@link FrameParticipant}s and will have prepared themselves for their upcoming painting in the
 * previous call to {@link FrameParticipant#tick}. When the call to paint completes, the flip
 * buffer is flipped and the process starts all over again.
 * </ul>
 *
 * <p> The ticking and rendering takes place on the AWT thread so as to avoid the need for
 * complicated coordination between AWT event handler code and frame code. However, this means that
 * all AWT (and Swing) event handlers <em>must not</em> perform any complicated processing. After
 * each frame, control of the AWT thread is given back to the AWT which processes all pending AWT
 * events before giving the frame manager an opportunity to process the next frame. Thus the
 * convenience of everything running on the AWT thread comes with the price of requiring that AWT
 * event handlers not block or perform any intensive processing.  In general, this is a sensible
 * structure for an application anyhow, so this organization tends to be preferable to an
 * organization where the AWT and frame threads are separate and must tread lightly so as not to
 * collide.
 *
 * <p> Note: the way that <code>JScrollPane</code> goes about improving performance when scrolling
 * complicated contents cannot work with active rendering. If you use a <code>JScrollPane</code> in
 * an application that uses the frame manager, you should either use the provided {@link
 * SafeScrollPane} or set your scroll panes' viewports to <code>SIMPLE_SCROLL_MODE</code>.
 */
public abstract class FrameManager
{
    /**
     * Normally, the frame manager will repaint any component in a {@link JLayeredPane} layer
     * (popups, overlays, etc.) that overlaps a frame participant on every tick because the frame
     * participant <em>could</em> have changed underneath the overlay which would require that the
     * overlay be repainted. If the application knows that the frame participant beneath the
     * overlay will never change, it can have its overlay implement this interface and avoid the
     * expense of forcibly fully repainting the overlay on every frame.
     */
    public static interface SafeLayerComponent
    {
    }

    /**
     * Provides a bridge between either {@link ManagedJFrame} or {@link ManagedJApplet} and the
     * frame manager.
     */
    public static interface ManagedRoot
    {
        /** Configures the root with a reference to its frame manager. */
        public void init (FrameManager fmgr);

        /** Returns the window at the root of the UI hierarchy. */
        public Window getWindow ();

        /** Returns the top-level Swing pane. */
        public JRootPane getRootPane();
    }

    /**
     * Creates a frame manager that will try to use a high resolution timer for timing but will
     * fall back to {@link MillisTimer}, which is available on every platform, but returns
     * inaccurate time stamps on many platforms.
     *
     * @see #newInstance(ManagedRoot, MediaTimer)
     */
    public static FrameManager newInstance (ManagedRoot root)
    {
        return newInstance(root, createTimer());
    }

    /**
     * Attempts to create a high resolution timer, but if that isn't possible, uses a
     * System.currentTimeMillis based timer.
     */
    public static MediaTimer createTimer ()
    {
        MediaTimer timer = null;
        for (String timerClass : PERF_TIMERS) {
            try {
                timer = (MediaTimer)Class.forName(timerClass).newInstance();
                break;
            } catch (Throwable t) {
                // try the next one
            }
        }
        if (timer == null) {
            log.info("Can't use high performance timer, reverting to " +
                     "System.currentTimeMillis() based timer.");
            timer = new MillisTimer();
        }
        return timer;
    }

    /**
     * Constructs a frame manager that will do its rendering to the supplied root and use the
     * supplied media timer for timing information.
     */
    public static FrameManager newInstance (ManagedRoot root, MediaTimer timer)
    {
        FrameManager fmgr = (root instanceof ManagedJFrame && _useFlip.getValue()) ?
            new FlipFrameManager() : new BackFrameManager();
        fmgr.init(root, timer);
        return fmgr;
    }

    /**
     * Instructs the frame manager to target the specified number of frames per second. If the
     * computation and rendering for a frame are completed with time to spare, the frame manager
     * will wait until the proper time to begin processing for the next frame. If a frame takes
     * longer than its allotted time, the frame manager will immediately begin processing on the
     * next frame.
     */
    public void setTargetFrameRate (int fps)
    {
        // compute the number of milliseconds per frame
        _millisPerFrame = 1000/fps;
    }

    /**
     * Registers a frame participant. The participant will be given the opportunity to do
     * processing and rendering on each frame.
     */
    public void registerFrameParticipant (FrameParticipant participant)
    {
        Object[] nparts = ListUtil.testAndAddRef(_participants, participant);
        if (nparts == null) {
            log.warning("Refusing to add duplicate frame participant! " + participant);
        } else {
            _participants = nparts;
        }
    }

    /**
     * Returns true if the specified participant is registered.
     */
    public boolean isRegisteredFrameParticipant (FrameParticipant participant)
    {
        return ListUtil.containsRef(_participants, participant);
    }

    /**
     * Removes a frame participant.
     */
    public void removeFrameParticipant (FrameParticipant participant)
    {
        ListUtil.clearRef(_participants, participant);
    }

    /**
     * Returns a millisecond granularity time stamp using the {@link MediaTimer} with which this
     * frame manager was configured. <em>Note:</em> this should only be called from the AWT
     * thread.
     */
    public long getTimeStamp ()
    {
        return _timer.getElapsedMillis();
    }

    /**
     * Returns the highest drift ratio our timer has seen. 1.0 means no drift (and is what we
     * return if we're not using a CalibratingTimer)
     */
    public float getMaxTimerDriftRatio ()
    {
        if (_timer instanceof CalibratingTimer) {
            return ((CalibratingTimer)_timer).getMaxDriftRatio();
        } else {
            return 1.0F;
        }
    }

    /**
     * Clears out the maximum drift our timer remembers seeing.
     */
    public void clearMaxTimerDriftRatio ()
    {
        if (_timer instanceof CalibratingTimer) {
            ((CalibratingTimer)_timer).clearMaxDriftRatio();
        }
    }

    /**
     * Returns an overlay that can be used to render sprites and animations on top of the entire
     * frame. This is lazily created the first time this method is called and the overlay will
     * remain a frame participant until {@link #clearMediaOverlay} is called. Be sure to
     * coordinate access to the overlay in your application as there is only one overlay in
     * existence at any time, and attempts to use an overlay after it has been cleared will fail.
     */
    public MediaOverlay getMediaOverlay ()
    {
        if (_overlay == null) {
            _overlay = new MediaOverlay(this);
        }
        return _overlay;
    }

    /**
     * Returns the managed root on which this frame manager is operating.
     */
    public ManagedRoot getManagedRoot ()
    {
        return _root;
    }

    /**
     * Clears out any media overlay that is in use.
     */
    public void clearMediaOverlay ()
    {
        if (_overlay != null) {
            _overlay = null;
        }
    }

    /**
     * Starts up the per-frame tick
     */
    public void start ()
    {
        if (_ticker == null) {
            _ticker = new Ticker();
            _ticker.start();
            _lastTickStamp = 0;
        }
    }

    /**
     * Stops the per-frame tick.
     */
    public synchronized void stop ()
    {
        if (_ticker != null) {
            _ticker.cancel();
            _ticker = null;
        }
    }

    /**
     * Returns true if the tick interval is be running (not necessarily at that instant, but in
     * general).
     */
    public synchronized boolean isRunning ()
    {
        return (_ticker != null);
    }

    /**
     * Returns the number of ticks executed in the last second.
     */
    public int getPerfTicks ()
    {
        return Math.round(_fps[1]);
    }

    /**
     * Returns the number of ticks requested in the last second.
     */
    public int getPerfTries ()
    {
        return Math.round(_fps[0]);
    }

    /**
     * Returns debug performance metrics.
     */
    public TrailingAverage[] getPerfMetrics ()
    {
        if (_metrics == null) {
            _metrics = new TrailingAverage[] {
                new TrailingAverage(150), new TrailingAverage(150), new TrailingAverage(150)
            };
        }
        return _metrics;
    }

    /**
     * Returns the root component for the supplied component or null if it is not part of a rooted
     * hierarchy or if any parent along the way is found to be hidden or without a peer. Along the
     * way, it adjusts the supplied component-relative rectangle to be relative to the returned
     * root component.
     */
    public static Component getRoot (Component comp, Rectangle rect)
    {
        for (Component c = comp; c != null; c = c.getParent()) {
            if (!c.isVisible() || !c.isDisplayable()) {
                return null;
            }
            if (c instanceof Window || c instanceof Applet) {
                return c;
            }
            rect.x += c.getX();
            rect.y += c.getY();
        }
        return null;
    }

    /**
     * Initializes this frame manager and prepares it for operation.
     */
    protected void init (ManagedRoot root, MediaTimer timer)
    {
        _window = root.getWindow();
        _root = root;
        _root.init(this);
        _timer = timer;

        // set up our custom repaint manager
        _repainter = new ActiveRepaintManager(
            (_root instanceof Component) ? (Component)_root : _window);
        RepaintManager.setCurrentManager(_repainter);

        // turn off double buffering for the whole business because we handle repaints
        _repainter.setDoubleBufferingEnabled(false);
    }

    /**
     * Called to perform the frame processing and rendering.
     */
    protected void tick (long tickStamp)
    {
        long start = 0L, paint = 0L;
        if (_perfDebug.getValue()) {
            start = paint = _timer.getElapsedMicros();
        }

        // if our frame is not showing (or is impossibly sized), don't try rendering anything
        if (_window.isShowing() && _window.getWidth() > 0 && _window.getHeight() > 0) {
            // tick our participants
            tickParticipants(tickStamp);
            paint = _timer.getElapsedMicros();
            // repaint our participants and components
            paint(tickStamp);
        }

        if (_perfDebug.getValue()) {
            long end = _timer.getElapsedMicros();
            getPerfMetrics()[1].record((int)(paint-start)/100);
            getPerfMetrics()[2].record((int)(end-paint)/100);
        }
    }

    /**
     * Called once per frame to invoke {@link FrameParticipant#tick} on all of our frame
     * participants.
     */
    protected void tickParticipants (long tickStamp)
    {
        long gap = tickStamp - _lastTickStamp;
        if (_lastTickStamp != 0 && gap > (HANG_DEBUG ? HANG_GAP : BIG_GAP)) {
            log.debug("Long tick delay [delay=" + gap + "ms].");
        }
        _lastTickStamp = tickStamp;

        // validate any invalid components
        try {
            _repainter.validateComponents();
        } catch (Throwable t) {
            log.warning("Failure validating components.", t);
        }

        // tick all of our frame participants
        for (Object participant : _participants) {
            FrameParticipant part = (FrameParticipant)participant;
            if (part == null) {
                continue;
            }

            try {
                long start = 0L;
                if (HANG_DEBUG) {
                    start = System.currentTimeMillis();
                }

                part.tick(tickStamp);

                if (HANG_DEBUG) {
                    long delay = (System.currentTimeMillis() - start);
                    if (delay > HANG_GAP) {
                        log.info("Whoa nelly! Ticker took a long time",
                            "part", part, "time", delay + "ms");
                    }
                }

            } catch (Throwable t) {
                log.warning("Frame participant choked during tick",
                    "part", StringUtil.safeToString(part), t);
            }
        }

        // if we have an overlay, tick that too
        if (_overlay != null) {
            _overlay.tick(tickStamp);
        }
    }

    /**
     * Called once per frame to invoke {@link Component#paint} on all of our frame participants'
     * components and all dirty components managed by our {@link ActiveRepaintManager}.
     */
    protected abstract void paint (long tickStamp);

    /**
     * Returns a graphics context with which to layout its media objects. The returned context must
     * be disposed when layout is complete and must not be retained across frame ticks. Used by the
     * {@link MediaOverlay}.
     */
    protected abstract Graphics2D createGraphics ();

    /**
     * Paints our frame participants and any dirty components via the repaint manager.
     *
     * @return true if anything was painted, false if not.
     */
    protected boolean paint (Graphics2D gfx)
    {
        // paint our frame participants (which want to be handled specially)
        int painted = 0;
        for (Object participant : _participants) {
            FrameParticipant part = (FrameParticipant)participant;
            if (part == null) {
                continue;
            }

            Component pcomp = part.getComponent();
            if (pcomp == null || !part.needsPaint()) {
                continue;
            }

            long start = 0L;
            if (HANG_DEBUG) {
                start = System.currentTimeMillis();
            }

            // get the bounds of this component
            pcomp.getBounds(_tbounds);

            // the bounds adjustment we're about to call will add in the components initial bounds
            // offsets, so we remove them here
            _tbounds.setLocation(0, 0);

            // convert them into top-level coordinates; also note that if this component does not
            // have a valid or visible root, we don't want to paint it either
            if (getRoot(pcomp, _tbounds) == null) {
                continue;
            }

            try {
                // render this participant; we don't set the clip because frame participants are
                // expected to handle clipping themselves; otherwise we might pointlessly set the
                // clip here, creating a few Rectangle objects in the process, only to have the
                // frame participant immediately set the clip to something more sensible
                gfx.translate(_tbounds.x, _tbounds.y);
                pcomp.paint(gfx);
                gfx.translate(-_tbounds.x, -_tbounds.y);
                painted++;

            } catch (Throwable t) {
                String ptos = StringUtil.safeToString(part);
                log.warning("Frame participant choked during paint [part=" + ptos + "].", t);
            }

            // render any components in our layered pane that are not in the default layer
            _clipped[0] = false;
            renderLayers(gfx, pcomp, _tbounds, _clipped, _tbounds);

            if (HANG_DEBUG) {
                long delay = (System.currentTimeMillis() - start);
                if (delay > HANG_GAP) {
                    log.warning("Whoa nelly! Painter took a long time",
                        "part", part, "time", delay + "ms");
                }
            }
        }

        // if we have a media overlay, give that a chance to propagate its dirty regions to the
        // active repaint manager for areas it dirtied during this tick
        if (_overlay != null) {
            _overlay.propagateDirtyRegions(_repainter, _root.getRootPane());
        }

        // repaint any widgets that have declared they need to be repainted since the last tick
        boolean pcomp = _repainter.paintComponents(gfx, this);

        // if we have a media overlay, give it a chance to paint on top of everything
        if (_overlay != null) {
            pcomp |= _overlay.paint(gfx);
        }

        // let the caller know if anybody painted anything
        return ((painted > 0) || pcomp);
    }

    /**
     * Called by the {@link ManagedJFrame} when our window was hidden and reexposed.
     */
    protected abstract void restoreFromBack (Rectangle dirty);

    /**
     * Renders all components in all {@link JLayeredPane} layers that intersect the supplied
     * bounds.
     */
    protected void renderLayers (Graphics2D g, Component pcomp, Rectangle bounds,
        boolean[] clipped, Rectangle dirty)
    {
        JLayeredPane lpane = JLayeredPane.getLayeredPaneAbove(pcomp);
        if (lpane != null) {
            renderLayer(g, bounds, lpane, clipped, JLayeredPane.PALETTE_LAYER);
            renderLayer(g, bounds, lpane, clipped, JLayeredPane.MODAL_LAYER);
            renderLayer(g, bounds, lpane, clipped, JLayeredPane.POPUP_LAYER);
            renderLayer(g, bounds, lpane, clipped, JLayeredPane.DRAG_LAYER);
        }

        // if we have a MediaOverlay, let it know that any sprites in this region need to be
        // repainted as the components beneath them have just been redrawn
        if (_overlay != null) {
            _overlay.addDirtyRegion(dirty);
        }
    }

    /**
     * Renders all components in the specified layer of the supplied layered pane that intersect
     * the supplied bounds.
     */
    protected void renderLayer (Graphics2D g, Rectangle bounds, JLayeredPane pane,
                                boolean[] clipped, Integer layer)
    {
        // stop now if there are no components in that layer
        int ccount = pane.getComponentCountInLayer(layer.intValue());
        if (ccount == 0) {
            return;
        }

        // render them up
        Component[] comps = pane.getComponentsInLayer(layer.intValue());
        for (int ii = 0; ii < ccount; ii++) {
            Component comp = comps[ii];
            if (!comp.isVisible() || comp instanceof SafeLayerComponent) {
                continue;
            }

            // if this overlay does not intersect the component we just rendered, we don't need to
            // repaint it
            Rectangle compBounds = new Rectangle(0, 0, comp.getWidth(), comp.getHeight());
            getRoot(comp, compBounds);
            if (!compBounds.intersects(bounds)) {
                continue;
            }

            // if the clipping region has not yet been set during this render pass, the time has
            // come to do so
            if (!clipped[0]) {
                g.setClip(bounds);
                clipped[0] = true;
            }

            // translate into the components coordinate system and render
            g.translate(compBounds.x, compBounds.y);
            try {
                comp.paint(g);
            } catch (Exception e) {
                log.warning("Component choked while rendering.", e);
            }
            g.translate(-compBounds.x, -compBounds.y);
        }
    }

    /** Used to effect periodic calls to {@link FrameManager#tick}. */
    protected class Ticker extends Thread
    {
        public Ticker () {
            super("FrameManagerTicker");
        }

        @Override
        public void run () {
            log.info("Frame manager ticker running", "sleepGran", _sleepGranularity.getValue());
            while (_running) {
                long start = 0L;
                if (_perfDebug.getValue()) {
                    start = _timer.getElapsedMicros();
                }
                Unsafe.sleep(_sleepGranularity.getValue());

                long woke = _timer.getElapsedMicros();
                if (start > 0L) {
                    getPerfMetrics()[0].record((int)(woke-start)/100);
                    int elapsed = (int)(woke-start);
                    if (elapsed > _sleepGranularity.getValue()*1500) {
                        log.warning("Long tick", "elapsed", elapsed + "us");
                    }
                }

                // work around sketchy bug on WinXP that causes the clock to leap into the past
                // from time to time
                if (woke < _lastAttempt) {
                    log.warning("Zoiks! We've leapt into the past, coping as best we can",
                        "dt", (woke - _lastAttempt));
                    _lastAttempt = woke;
                }

                if (woke - _lastAttempt >= _millisPerFrame * 1000) {
                    _lastAttempt = woke;
                    if (testAndSet()) {
                        EventQueue.invokeLater(_awtTicker);
                    }
                    // else: drop the frame
                }
            }
        }

        public void cancel () {
            _running = false;
        }

        protected final synchronized boolean testAndSet () {
            _tries++;
            if (!_ticking) {
                _ticking = true;
                return true;
            }
            return false;
        }

        protected final synchronized void clearTicking (long elapsed) {
            if (++_ticks == 100) {
                long time = (elapsed - _lastTick);
                _fps[0] = _tries * 1000f / time;
                _fps[1] = _ticks * 1000f / time;
                _lastTick = elapsed;
                _ticks = _tries = 0;
            }
            _ticking = false;
        }

        /** Used to invoke the call to {@link FrameManager#tick} on the AWT event queue thread. */
        protected Runnable _awtTicker = new Runnable () {
            public void run () {
                long elapsed = _timer.getElapsedMillis();
                try {
                    tick(elapsed);
                } finally {
                    clearTicking(elapsed);
                }
            }
        };

        /** Used to stick a fork in our ticker when desired. */
        protected transient boolean _running = true;

        /** Used to detect when we need to drop frames. */
        protected boolean _ticking;

        /** The time at which we last attempted to tick. */
        protected long _lastAttempt;

        /** Used to compute metrics. */
        protected int _tries, _ticks, _time;

        /** Used to compute metrics. */
        protected long _lastTick;
    }

    /** The window into which we do our rendering. */
    protected Window _window;

    /** Provides access to our Swing bits. */
    protected ManagedRoot _root;

    /** Used to obtain timing measurements. */
    protected MediaTimer _timer;

    /** Our custom repaint manager. */
    protected ActiveRepaintManager _repainter;

    /** If active, an overlay that will be rendering sprites and animations on top of the frame. */
    protected MediaOverlay _overlay;

    /** The number of milliseconds per frame (14 by default, which gives an fps of ~71). */
    protected long _millisPerFrame = 14;

    /** Used to track big delays in calls to our tick method. */
    protected long _lastTickStamp;

    /** The thread that dispatches our frame ticks. */
    protected Ticker _ticker;

    /** Used to track and report frames per second. */
    protected float[] _fps = new float[2];

    /** Used to track performance metrics. */
    protected TrailingAverage[] _metrics;

    /** A temporary bounds rectangle used to avoid lots of object creation. */
    protected Rectangle _tbounds = new Rectangle();

    /** Used to lazily set the clip when painting popups and other "layered" components. */
    protected boolean[] _clipped = new boolean[1];

    /** The entities that are ticked each frame. */
    protected Object[] _participants = new Object[4];

    /** If we don't get ticked for 500ms, that's worth complaining about. */
    protected static final long BIG_GAP = 500L;

    /** If we don't get ticked for 100ms and we're hang debugging, complain. */
    protected static final long HANG_GAP = 100L;

    /** Enable this to log warnings when ticking or painting takes too long. */
    protected static final boolean HANG_DEBUG = false;

    /** A debug hook that toggles debug rendering of sprite paths. */
    protected static RuntimeAdjust.BooleanAdjust _useFlip = new RuntimeAdjust.BooleanAdjust(
        "When active a flip-buffer will be used to manage our rendering, otherwise a " +
        "volatile back buffer is used [requires restart]", "narya.media.frame",
        // back buffer rendering doesn't work on the Mac, so we default to flip buffer on that
        // platform; we still allow it to be toggled so that we can easily test things when
        // they release new JVMs
        MediaPrefs.config, RunAnywhere.isMacOS());

    /** Allows us to tweak the sleep granularity. */
    protected static RuntimeAdjust.IntAdjust _sleepGranularity = new RuntimeAdjust.IntAdjust(
        "The number of milliseconds slept before checking to see if it's time to queue up a " +
        "new frame tick.", "narya.media.sleep_gran",
        MediaPrefs.config, RunAnywhere.isWindows() ? 10 : 7);

    /** A debug hook that toggles FPS rendering. */
    protected static RuntimeAdjust.BooleanAdjust _perfDebug = new RuntimeAdjust.BooleanAdjust(
        "Toggles frames per second and dirty regions per tick rendering.",
        "narya.media.fps_display", MediaPrefs.config, false);

    /** The name of the high-performance timer class we attempt to load. */
    protected static final String[] PERF_TIMERS = {
        "com.threerings.media.timer.PerfTimer",
        "com.threerings.media.timer.NanoTimer",
    };
}
