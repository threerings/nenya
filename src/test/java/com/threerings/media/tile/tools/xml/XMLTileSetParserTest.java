//
// $Id: XMLTileSetParserTest.java 3099 2004-08-27 02:21:06Z mdb $
//
// Narya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
// http://www.threerings.net/code/narya/
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import java.io.IOException;

import com.threerings.media.tile.ObjectTileSet;
import com.threerings.media.tile.SwissArmyTileSet;
import com.threerings.media.tile.UniformTileSet;

import org.junit.*;
import static org.junit.Assert.*;

import com.threerings.media.tile.TileSet;

public class XMLTileSetParserTest
{
    @Test
    public void testRuleSets ()
        throws IOException
    {
        Map<String, TileSet> sets = new HashMap<String, TileSet>();

        XMLTileSetParser parser = new XMLTileSetParser();
        parser.addRuleSet("tilesets/uniform", new UniformTileSetRuleSet());
        parser.addRuleSet("tilesets/swissarmy", new SwissArmyTileSetRuleSet());
        parser.addRuleSet("tilesets/object", new ObjectTileSetRuleSet());

        // load up the tilesets
        parser.loadTileSets(TILESET_PATH, sets);

        // make sure they were properly parsed
        SwissArmyTileSet fset = (SwissArmyTileSet)sets.remove("Fringe");
        // System.out.println(fset);
        assertEquals("Fringe", fset.getName());
        assertEquals("fringe.png", fset.getImagePath());
        assertEquals(40, fset.getTileCount());
        assertArrayEquals(repeat(64, 5), fset.getWidths());
        assertArrayEquals(repeat(48, 5), fset.getHeights());
        assertArrayEquals(repeat(8, 5), fset.getTileCounts());

        ObjectTileSet bset = (ObjectTileSet)sets.remove("Building");
        // System.out.println(bset);
        assertEquals("Building", bset.getName());
        assertEquals("building.png", bset.getImagePath());
        assertEquals(4, bset.getTileCount());
        assertArrayEquals(repeat(224, 1), bset.getWidths());
        assertArrayEquals(repeat(293, 1), bset.getHeights());
        assertArrayEquals(repeat(4, 1), bset.getTileCounts());
        int[] owidths = { 4, 3, 4, 3 }, oheights = { 3, 4, 3, 4 };
        for (int ii = 0; ii < 4; ii++) {
            assertEquals(owidths[ii], bset.getBaseWidth(ii));
            assertEquals(oheights[ii], bset.getBaseHeight(ii));
        }
        // TODO: test offset pos and gap size

        UniformTileSet iset = (UniformTileSet)sets.remove("Node Icons");
        // System.out.println(iset);
        assertEquals("Node Icons", iset.getName());
        assertEquals("node-icons.png", iset.getImagePath());
        assertEquals(16, iset.getWidth());
        assertEquals(16, iset.getHeight());
        // TODO: can't test getTileCount() since that requires real image data

        assertEquals(0, sets.size());
    }

    protected int[] repeat (int value, int count)
    {
        int[] ints = new int[count];
        Arrays.fill(ints, value);
        return ints;
    }

    protected static final String TILESET_PATH = "rsrc/media/tile/tools/xml/tilesets.xml";
}
