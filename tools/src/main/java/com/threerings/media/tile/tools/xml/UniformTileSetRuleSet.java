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

package com.threerings.media.tile.tools.xml;

import org.apache.commons.digester.Digester;

import com.threerings.media.tile.TileSet;
import com.threerings.media.tile.UniformTileSet;

import static com.threerings.media.Log.log;

/**
 * Parses {@link UniformTileSet} instances from a tileset description. A
 * uniform tileset description looks like so:
 *
 * {@code
 * <tileset name="Sample Uniform Tileset">
 *   <imagePath>path/to/image.png</imagePath>
 *   <!-- the width of each tile in pixels -->
 *   <width>64</width>
 *   <!-- the height of each tile in pixels -->
 *   <height>48</height>
 * </tileset>
 * }
 */
public class UniformTileSetRuleSet extends TileSetRuleSet
{
    public UniformTileSetRuleSet(){
        this(TILESET_PATH);
    }

    public UniformTileSetRuleSet(String tilesetPath){
        _tilesetPath = tilesetPath;
    }

    @Override
    public void addRuleInstances (Digester digester)
    {
        super.addRuleInstances(digester);

        digester.addCallMethod(_path + "/width", "setWidth", 0,
            new Class<?>[] { java.lang.Integer.TYPE });
        digester.addCallMethod(_path + "/height", "setHeight", 0,
            new Class<?>[] { java.lang.Integer.TYPE });
    }

    @Override
    public boolean isValid (Object target)
    {
        UniformTileSet set = (UniformTileSet)target;
        boolean valid = super.isValid(target);

        // check for a <width> element
        if (set.getWidth() == 0) {
            log.warning("Tile set definition missing valid <width> " +
                        "element [set=" + set + "].");
            valid = false;
        }

        // check for a <height> element
        if (set.getHeight() == 0) {
            log.warning("Tile set definition missing valid <height> " +
                        "element [set=" + set + "].");
            valid = false;
        }

        return valid;
    }

    @Override
    protected Class<? extends TileSet> getTileSetClass ()
    {
        return UniformTileSet.class;
    }
}
