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

package com.threerings.util;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import com.samskivert.util.Interval;
import com.samskivert.util.RunQueue;

import static com.threerings.NenyaLog.log;

/**
 * Used to track user idleness in an AWT application.
 */
public abstract class IdleTracker
{
    /**
     * Creates an idle tracker that will report idleness (via {@link
     * #idledOut}) after <code>toIdleTime</code> milliseconds have elapsed.
     * After an additional <code>toAbandonTime</code> milliseconds have
     * elapsed, we will report that the user has {@link #abandonedShip}.
     */
    public IdleTracker (long toIdleTime, long toAbandonTime)
    {
        _toIdleTime = toIdleTime;
        _toAbandonTime = toAbandonTime;

        // initialize our last event time
        _lastEvent = getTimeStamp();
    }

    public void start (KeyboardManager keymgr, RunQueue rqueue)
    {
        start(keymgr, null, rqueue);
    }

    public void start (KeyboardManager keymgr, Window root, RunQueue rqueue)
    {
        EventListener listener = new EventListener();
        try {
            // try to add a global event listener
            Toolkit.getDefaultToolkit().addAWTEventListener(
                listener, EVENT_MASK);
        } catch (SecurityException se) {
            // fall back to listening to our main window
            if (root != null) {
                root.addKeyListener(listener);
                root.addMouseListener(listener);
                root.addMouseMotionListener(listener);
            }
        }

        // and tie into the keyboard manager if one is provided
        if (keymgr != null) {
            keymgr.registerKeyObserver(new KeyboardManager.KeyObserver() {
                public void handleKeyEvent (
                    int id, int keyCode, long timestamp) {
                    handleUserActivity();
                }
            });
        }

        // register an interval to periodically check our last activity time
        new Interval(rqueue) {
            @Override
            public void expired () {
                checkIdle();
            }
        }.schedule(_toIdleTime/3, true);
    }

    /**
     * Called when the client has been idle for {@link #_toIdleTime}
     * milliseconds.
     */
    protected abstract void idledOut ();

    /**
     * Called when the client becomes non-idle after we have previously
     * reported their idleness.
     */
    protected abstract void idledIn ();

    /**
     * Called when the client has been idle for {@link #_toIdleTime} plus
     * {@link #_toAbandonTime} milliseconds.
     */
    protected abstract void abandonedShip ();

    /**
     * This should return a timestamp. We would use {@link
     * System#currentTimeMillis} except that on Windows that sometimes does
     * strange things like leap forward in time causing immediate idleness.
     */
    protected abstract long getTimeStamp ();

    /**
     * Called with any keyboard or mouse events performed on the frame so
     * as to note user activity as it pertains to tracking the client idle
     * state.
     */
    protected void handleUserActivity ()
    {
        // note the time of the last user action
        _lastEvent = getTimeStamp();

        // idle-in if appropriate
        if (_state != ACTIVE) {
            _state = ACTIVE;
            idledIn();
        }
    }

    /**
     * Checks the last user event time and posts a command to idle them
     * out if they've been inactive for too long, or log them out if
     * they've been idle for too long.
     */
    protected void checkIdle ()
    {
        long now = getTimeStamp();

        switch (_state) {
        case ACTIVE:
            // check whether they've idled out
            if (now >= (_lastEvent + _toIdleTime)) {
                log.info("User idle for " + (now-_lastEvent) + "ms.");
                _state = IDLE;
                idledOut();
            }
            break;

        case IDLE:
            // check whether they've been idle for too long
            if (now >= (_lastEvent + _toIdleTime + _toAbandonTime)) {
                log.info("User idle for " + (now-_lastEvent) + "ms. " +
                         "Abandoning ship.");
                _state = ABANDONED;
                abandonedShip();
            }
            break;
        }
    }

    protected class EventListener
        implements AWTEventListener, KeyListener, MouseListener,
                   MouseMotionListener
    {
        public void eventDispatched (AWTEvent event) {
            handleUserActivity();
        }

        public void keyTyped (KeyEvent e) {
            eventDispatched(e);
        }
        public void keyPressed (KeyEvent e) {
            eventDispatched(e);
        }
        public void keyReleased (KeyEvent e) {
            eventDispatched(e);
        }

        public void mouseClicked (MouseEvent e) {
            eventDispatched(e);
        }
        public void mousePressed (MouseEvent e) {
            eventDispatched(e);
        }
        public void mouseReleased (MouseEvent e) {
            eventDispatched(e);
        }
        public void mouseEntered (MouseEvent e) {
            eventDispatched(e);
        }
        public void mouseExited (MouseEvent e) {
            eventDispatched(e);
        }

        public void mouseDragged (MouseEvent e) {
            eventDispatched(e);
        }
        public void mouseMoved (MouseEvent e) {
            eventDispatched(e);
        }
    }

//     /** The user's current state. */
//     protected static enum State { ACTIVE, IDLE, ABANDONED };

    /** The duration after which we declare the user to be idle. */
    protected long _toIdleTime;

    /** The duration after which we declare the user to have abandoned ship. */
    protected long _toAbandonTime;

    /** The time of the last mouse or keyboard event; used to track
     * whether the user is idle. */
    protected long _lastEvent;

    /** Whether the user is currently active, idle or abandoned. */
    protected int _state = ACTIVE;

    protected static final int ACTIVE = 0;
    protected static final int IDLE = 1;
    protected static final int ABANDONED = 2;

    // we want to observe all mouse and keyboard events
    protected static final long EVENT_MASK =
        AWTEvent.MOUSE_EVENT_MASK |
        AWTEvent.MOUSE_MOTION_EVENT_MASK |
        AWTEvent.MOUSE_WHEEL_EVENT_MASK |
        AWTEvent.KEY_EVENT_MASK;
}
