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

import com.jme.scene.Geometry;
import com.jme.scene.batch.GeomBatch;

/**
 * Visits all of the {@link GeomBatch}es in a scene.
 */
public abstract class BatchVisitor extends SpatialVisitor<Geometry>
{
    public BatchVisitor ()
    {
        super(Geometry.class);
    }

    // documentation inherited from SpatialVisitor
    protected void visit (Geometry geom)
    {
        for (int ii = 0, nn = geom.getBatchCount(); ii < nn; ii++) {
            visit(geom.getBatch(ii));
        }
    }

    /**
     * Called once for each {@link GeomBatch} in the scene graph.
     */
    protected abstract void visit (GeomBatch batch);
}
