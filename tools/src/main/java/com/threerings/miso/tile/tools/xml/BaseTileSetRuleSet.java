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

package com.threerings.miso.tile.tools.xml;

import org.apache.commons.digester.Digester;

import com.samskivert.util.StringUtil;
import com.samskivert.xml.CallMethodSpecialRule;

import com.threerings.media.tile.TileSet;
import com.threerings.media.tile.tools.xml.SwissArmyTileSetRuleSet;

import com.threerings.miso.tile.BaseTileSet;

import static com.threerings.miso.Log.log;

/**
 * Parses {@link BaseTileSet} instances from a tileset description. Base
 * tilesets extend swiss army tilesets with the addition of a passability
 * flag for each tile.
 *
 * @see SwissArmyTileSetRuleSet
 */
public class BaseTileSetRuleSet extends SwissArmyTileSetRuleSet
{
    @Override
    public void addRuleInstances (Digester digester)
    {
        super.addRuleInstances(digester);

        digester.addRule(_path + "/passable", new CallMethodSpecialRule() {
                @Override
                public void parseAndSet (String bodyText, Object target) {
                    int[] values = StringUtil.parseIntArray(bodyText);
                    boolean[] passable = new boolean[values.length];
                    for (int ii = 0; ii < values.length; ii++) {
                        passable[ii] = (values[ii] != 0);
                    }
                    BaseTileSet starget = (BaseTileSet)target;
                    starget.setPassability(passable);
                }
            });
    }

    @Override
    public boolean isValid (Object target)
    {
        BaseTileSet set = (BaseTileSet)target;
        boolean valid = super.isValid(target);

        // check for a <passable> element
        if (set.getPassability() == null) {
            log.warning("Tile set definition missing valid <passable> " +
                        "element [set=" + set + "].");
            valid = false;
        }

        return valid;
    }

    @Override
    protected Class<? extends TileSet> getTileSetClass ()
    {
        return BaseTileSet.class;
    }
}
