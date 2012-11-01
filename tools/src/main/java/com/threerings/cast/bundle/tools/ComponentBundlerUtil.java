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

package com.threerings.cast.bundle.tools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.xml.sax.SAXException;
import org.apache.commons.digester.Digester;

import com.threerings.media.tile.TileSet;
import com.threerings.media.tile.tools.xml.SwissArmyTileSetRuleSet;
import com.threerings.media.tile.tools.xml.TileSetRuleSet;
import com.threerings.media.tile.tools.xml.UniformTileSetRuleSet;

import com.threerings.cast.tools.xml.ActionRuleSet;

/**
 * Utilities needed when building component bundles.
 */
public class ComponentBundlerUtil
{
    /**
     * Parses the action tileset definitions in the supplied file and puts them into a hash map,
     * keyed on action name.
     */
    public static Map<String, TileSet> parseActionTileSets (File file)
        throws IOException, SAXException
    {
        return parseActionTileSets(new BufferedInputStream(new FileInputStream(file)));
    }

    /**
     * Parses the action tileset definitions in the supplied input stream, and puts them into a
     * hash map, keyed on action name.
     */
    public static Map<String, TileSet> parseActionTileSets (InputStream in)
        throws IOException, SAXException
    {
        Digester digester = new Digester();
        digester.addSetProperties("actions" + ActionRuleSet.ACTION_PATH);
        addTileSetRuleSet(digester, new SwissArmyTileSetRuleSet());
        addTileSetRuleSet(digester, new UniformTileSetRuleSet("/uniformTileset"));

        Map<String, TileSet> actsets = new ActionMap();
        digester.push(actsets);
        digester.parse(in);
        return actsets;
    }

    /**
     * Configures <code>ruleSet</code> and hooks it into <code>digester</code>.
     */
    protected static void addTileSetRuleSet (Digester digester, TileSetRuleSet ruleSet)
    {
        ruleSet.setPrefix("actions" + ActionRuleSet.ACTION_PATH);
        digester.addRuleSet(ruleSet);
        digester.addSetNext(ruleSet.getPath(), "addTileSet", TileSet.class.getName());
    }

    /** Used when parsing action tilesets. (This class must be public for Digester to work.) */
    public static class ActionMap extends HashMap<String, TileSet>
    {
        public void setName (String name) {
            _name = name;
        }
        public void addTileSet (TileSet set) {
            set.setName(_name);
            put(_name, set);
        }
        protected String _name;
    }
}
