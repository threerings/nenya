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

package com.threerings.miso.client;

import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.Icon;

/**
 * Draws something to indicate a fascinating, clickable object in a scene.
 */
public interface SceneObjectIndicator
{
    /**
     * Returns the bounds of the indicator when drawn.
     */
    public Rectangle getBounds ();

    /**
     * Positions the indicator in the scene in relation to <code>key</code>
     */
    public void layout (Graphics2D gfx, SceneObject key, Rectangle viewBounds);

    /**
     * Returns whether the indicator has already been laid out (and thus doesn't need to be again)
     */
    public boolean isLaidOut ();

    /**
     * Called when the indicator is removed from the scene.
     */
    public void removed ();

    /**
     * Paints the indicator in the scene. Always called after
     * {@link #layout(Graphics2D, SceneObject, Rectangle)}
     */
    public void paint (Graphics2D gfx);

    /**
     * Updates the Icon and text for the indicator.
     */
    public void update (Icon icon, String tiptext);
}
