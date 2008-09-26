package com.threerings.miso.client;

import java.util.Collection;

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
    public void layout (Graphics2D gfx, SceneObject key, Rectangle viewBounds,
        Collection<Rectangle> otherIndicators);

    /**
     * Called when the indicator is removed from the scene.
     */
    public void removed ();

    /**
     * Paints the indicator in the scene. Always called after
     * {@link #layout(Graphics2D, SceneObject, Rectangle, Collection)}
     */
    public void paint (Graphics2D gfx);

    /**
     * Updates the Icon and text for the indicator.
     */
    public void update (Icon icon, String tiptext);
}
