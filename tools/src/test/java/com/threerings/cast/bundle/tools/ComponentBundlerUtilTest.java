//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
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

package com.threerings.cast.bundle.tools;

import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.util.Map;

import org.junit.*;
import static org.junit.Assert.*;

import com.threerings.media.tile.SwissArmyTileSet;
import com.threerings.media.tile.TileSet;

/**
 * Tests the component bundler utilities.
 */
public class ComponentBundlerUtilTest
{
    @Test
    public void testParseActionTileSets ()
        throws Exception
    {
        Map<String, TileSet> map = ComponentBundlerUtil.parseActionTileSets(
            new ByteArrayInputStream(ACTION_DATA.getBytes()));

        SwissArmyTileSet defset = (SwissArmyTileSet)map.get("default");
        assertNotNull(defset);
        assertEquals("default", defset.getName());
        assertEquals(1, defset.getTileCount());
        assertArrayEquals(new int[] { 1 }, defset.getTileCounts());
        assertEquals(new Rectangle(0, 0, 540, 640), defset.computeTileBounds(0, new Rectangle()));

        SwissArmyTileSet statset = (SwissArmyTileSet)map.get("static");
        assertNotNull(statset);
        assertEquals("static", statset.getName());
        assertEquals(1, statset.getTileCount());
        assertArrayEquals(new int[] { 1 }, statset.getTileCounts());
        assertEquals(new Rectangle(0, 0, 312, 240), statset.computeTileBounds(0, new Rectangle()));
    }

    protected static final String ACTION_DATA =
        "<actions>\n" +
        "  <!-- actions relating to the face shot components -->\n" +
        "  <action name=\"default\">\n" +
        "    <framesPerSecond>1</framesPerSecond>\n" +
        "    <origin>270,640</origin>\n" +
        "    <!-- TODO: fix code that requires one array entry for each orientation -->\n" +
        "    <orients>SW</orients>\n" +
        "    <tileset>\n" +
        "      <widths>540</widths>\n" +
        "      <heights>640</heights>\n" +
        "      <tileCounts>1</tileCounts>\n" +
        "      <offsetPos>0, 0</offsetPos>\n" +
        "      <gapSize>0, 0</gapSize>\n" +
        "    </tileset>\n" +
        "  </action>\n" +
        "\n" +
        "  <!-- actions relating to the gang buckle components -->\n" +
        "  <action name=\"static\">\n" +
        "    <framesPerSecond>1</framesPerSecond>\n" +
        "    <origin>156,240</origin>\n" +
        "    <orients>SW</orients>\n" +
        "    <tileset>\n" +
        "      <widths>312</widths>\n" +
        "      <heights>240</heights>\n" +
        "      <tileCounts>1</tileCounts>\n" +
        "      <offsetPos>0, 0</offsetPos>\n" +
        "      <gapSize>0, 0</gapSize>\n" +
        "    </tileset>\n" +
        "  </action>\n" +
        "</actions>";
}
