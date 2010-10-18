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

package com.threerings.jme.effect;

import com.jme.scene.Node;
import com.jme.system.DisplaySystem;

import com.jmex.bui.BWindow;

import com.threerings.jme.util.LinearTimeFunction;
import com.threerings.jme.util.TimeFunction;

/**
 * Slides a window onto the center of the screen from offscreen or offscreen
 * from the center of the screen.
 */
public class WindowSlider extends Node
{
    // slides from the specified location into the center of the screen
    public static final int FROM_TOP = 0;
    public static final int FROM_RIGHT = 1;

    // slides from the center of the screen off screen in the specified direction
    public static final int TO_TOP = 2;
    public static final int TO_RIGHT = 3;

    // slides down from the specified location stopping when whole window is visible (window
    // remains "stuck" to the edge from which it slid in)
    public static final int FROM_TOP_STICKY = 4;

    /**
     * Creates a window slider with the specified mode and window that will
     * slide the window either onto or off of the screen in the specified
     * number of seconds.
     *
     * @param dx an offset applied to the starting or destination position
     * along the x axis (starting for sliding off, destination for sliding on).
     * @param dy an offset applied along the y axis.
     */
    public WindowSlider (BWindow window, int mode, float duration,
                         int dx, int dy)
    {
        super("slider");

        DisplaySystem ds = DisplaySystem.getDisplaySystem();
        int swidth = ds.getWidth(), sheight = ds.getHeight();
        int wwidth = window.getWidth(), wheight = window.getHeight();

        int start = 0, end = 0;
        switch (mode) {
        case FROM_TOP:
            start = sheight+wheight;
            end = (sheight-wheight)/2 + dy;
            window.setLocation((swidth-wwidth)/2 + dx, start);
            break;

        case FROM_RIGHT:
            start = swidth+wwidth;
            end = (swidth-wwidth)/2 + dx;
            window.setLocation(start, (sheight-wheight)/2 + dy);
            break;

        case TO_TOP:
            start = (sheight-wheight)/2 + dy;
            end = sheight+wheight;
            window.setLocation((swidth-wwidth)/2 + dx, start);
            break;

        case TO_RIGHT:
            start = (swidth-wwidth)/2 + dx;
            end = swidth+wwidth;
            window.setLocation(start, (sheight-wheight)/2 + dy);
            break;

        case FROM_TOP_STICKY:
            start = sheight+wheight;
            end = sheight-wheight + dy;
            window.setLocation((swidth-wwidth)/2 + dx, start);
        }

        _mode = mode;
        _window = window;
        _tfunc = new LinearTimeFunction(start, end, duration);
    }

    /**
     * Allows some number of ticks to be skipped to give the window that is
     * being slid a chance to be layed out before we start keeping track of
     * time. The layout may be expensive and cause the frame rate to drop for a
     * frame or two, thus booching our smooth sliding onto the screen.
     */
    public void setSkipTicks (int skipTicks)
    {
        _skipTicks = skipTicks;
    }

    // documentation inherited
    public void updateGeometricState (float time, boolean initiator)
    {
        super.updateGeometricState(time, initiator);

        // skip ticks as long as we need to
        if (_skipTicks-- > 0) {
            return;
        }

        int winx, winy;
        if (_mode % 2 == 1) {
            winx = (int)_tfunc.getValue(time);
            winy = _window.getY();
        } else {
            winx = _window.getX();
            winy = (int)_tfunc.getValue(time);
        }
        _window.setLocation(winx, winy);

        if (_tfunc.isComplete()) {
            slideComplete();
        }
    }

    /**
     * Called (only once) when we have reached the end of our slide.
     * Automatically detaches this effect from the hierarchy.
     */
    protected void slideComplete ()
    {
        getParent().detachChild(this);
    }

    protected int _mode;
    protected BWindow _window;
    protected TimeFunction _tfunc;

    // skip two frames by default as that generally handles the normal window
    // layout process
    protected int _skipTicks = 2;
}
