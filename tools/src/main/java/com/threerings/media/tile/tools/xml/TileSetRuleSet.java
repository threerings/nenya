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
import org.apache.commons.digester.RuleSetBase;

import com.samskivert.util.StringUtil;
import com.samskivert.xml.ValidatedSetNextRule;
import com.samskivert.xml.ValidatedSetNextRule.Validator;

import com.threerings.media.tile.TileSet;

import static com.threerings.media.Log.log;

/**
 * The tileset rule set is used to parse the base attributes of a tileset
 * instance. Derived classes would extend this and add rules for their own
 * special tilesets.
 */
public abstract class TileSetRuleSet
    extends RuleSetBase implements Validator
{
    /** The component of the digester path that is appended by the tileset
     * rule set to match a tileset. This is appended to whatever prefix is
     * provided to the tileset rule set to obtain the complete XML path to
     * a matched tileset. */
    public static final String TILESET_PATH = "/tileset";

    /**
     * @return The full path used to match tilesets.  Consists of the prefile plus _tilesetPath.
     */
    public String getPath () {
        return _path;
    }

    /**
     * Instructs the tileset rule set to match tilesets with the supplied
     * prefix. For example, passing a prefix of
     * <code>tilesets.objectsets</code> will match tilesets in the
     * following XML file:
     *
     * <pre>
     * &lt;tilesets&gt;
     *   &lt;objectsets&gt;
     *     &lt;tileset&gt;
     *       // ...
     *     &lt;/tileset&gt;
     *   &lt;/objectsets&gt;
     * &lt;/tilesets&gt;
     * </pre>
     *
     * This must be called before adding the ruleset to a digester.
     */
    public void setPrefix (String prefix)
    {
        _path =  prefix + _tilesetPath;
    }

    /**
     * Adds the necessary rules to the digester to parse our tilesets.
     * Derived classes should override this method, being sure to call the
     * superclass method and then adding their own rule instances (which
     * should register themselves relative to the <code>_prefix</code>
     * member).
     */
    @Override
    public void addRuleInstances (Digester digester)
    {
        // this creates the appropriate instance when we encounter a
        // <tileset> tag
        digester.addObjectCreate(_path, getTileSetClass().getName());

        // grab the name attribute from the <tileset> tag
        digester.addSetProperties(_path);

        // grab the image path from an element
        digester.addCallMethod(_path + "/imagePath", "setImagePath", 0);
    }

    /**
     * The ruleset can be provided to a {@link ValidatedSetNextRule} to ensure that the tileset
     * was fully parsed before doing something with it.
     */
    public boolean isValid (Object target)
    {
        TileSet set = (TileSet)target;
        boolean valid = true;

        // check for the 'name' attribute
        if (StringUtil.isBlank(set.getName())) {
            log.warning("Tile set definition missing 'name' attribute " +
                        "[set=" + set + "].");
            valid = false;
        }

        // check for an <imagePath> element
        if (StringUtil.isBlank(set.getImagePath())) {
            log.warning("Tile set definition missing <imagePath> element " +
                        "[set=" + set + "].");
            valid = false;
        }

        return valid;
    }

    /**
     * A tileset rule set will create tilesets of a particular class,
     * which must be provided by the derived class via this method.
     */
    protected abstract Class<? extends TileSet> getTileSetClass ();

    /** The tileset path we append to the prefix to get the full path. */
    protected String _tilesetPath = TILESET_PATH;

    /** The full path at which me match our tilesets. */
    protected String _path;
}
