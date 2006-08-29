//
// $Id$

package com.threerings.jme.util;

import com.jme.scene.Node;
import com.jme.scene.Spatial;

/**
 * Performs a depth-first scene graph traversal looking for {@link Spatial}s
 * of a given class.
 */
public abstract class SpatialVisitor<T extends Spatial>
{
    public SpatialVisitor (Class<T> type)
    {
        _type = type;
    }
    
    /**
     * Traverses the given node in depth-first order, calling {@link #visit}
     * for each {@link Spatial} of the configured class encountered.
     */
    public void traverse (Spatial spatial)
    {
        if (_type.isInstance(spatial)) {
            visit(_type.cast(spatial));
        }
        if (spatial instanceof Node) {
            Node node = (Node)spatial;
            for (int ii = 0, nn = node.getQuantity(); ii < nn; ii++) {
                traverse(node.getChild(ii));
            }
        }
    }
    
    /**
     * Called once for each {@link Spatial} of the configured class in the
     * scene graph.
     */
    protected abstract void visit (T child);
    
    /** The type of spatial of interest. */
    protected Class<T> _type;
}
