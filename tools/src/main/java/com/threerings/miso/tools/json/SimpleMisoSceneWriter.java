//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2013 Three Rings Design, Inc., All Rights Reserved
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

package com.threerings.miso.tools.json;

import com.threerings.miso.data.ObjectInfo;
import com.threerings.miso.data.SimpleMisoSceneModel;

import playn.core.Json;

/**
 * Writes a simple miso scene to JSON format.
 */
public class SimpleMisoSceneWriter
{
    /** The element used to enclose scene models written with this writer. */
    public static final String OUTER_ELEMENT = "miso";

    /** Writes {@code model} to {@code out}. */
    public static void write (SimpleMisoSceneModel model, Json.Writer out) {
        out.object(); // outer
        out.value("width", model.width);
        out.value("height", model.height);
        out.value("viewwidth", model.vwidth);
        out.value("viewheight", model.vheight);

        out.array("base");
        for (int tileId : model.baseTileIds) out.value(tileId);
        out.end();

        out.array("objects");
        for (int ii = 0; ii < model.objectTileIds.length; ii++) {
            out.object();
            out.value("tileId", model.objectTileIds[ii]);
            out.value("x", model.objectXs[ii]);
            out.value("y", model.objectYs[ii]);
            out.end();
        }
        for (ObjectInfo info : model.objectInfo) {
            out.object();
            out.value("tileId", info.tileId);
            out.value("x", info.x);
            out.value("y", info.y);
            if (info.action != null) out.value("action", info.action);
            if (info.priority != 0) out.value("priority", info.priority);
            if (info.sx != 0 || info.sy != 0) {
                out.value("sx", info.sx);
                out.value("sy", info.sy);
                out.value("sorient", info.sorient);
            }
            if (info.zations != 0) out.value("zations", info.zations);
        }
        out.end(); // objects
        out.end(); // outer
    }
}
