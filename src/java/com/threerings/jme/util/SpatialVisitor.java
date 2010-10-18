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
