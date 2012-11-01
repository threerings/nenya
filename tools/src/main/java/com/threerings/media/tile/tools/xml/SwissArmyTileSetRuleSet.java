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

import java.awt.Dimension;
import java.awt.Point;

import org.apache.commons.digester.Digester;

import com.samskivert.util.StringUtil;
import com.samskivert.xml.CallMethodSpecialRule;

import com.threerings.media.tile.SwissArmyTileSet;
import com.threerings.media.tile.TileSet;

import static com.threerings.media.Log.log;

/**
 * Parses {@link SwissArmyTileSet} instances from a tileset description. A
 * swiss army tileset description looks like so:
 *
 * {@code
 * <tileset name="Sample Swiss Army Tileset">
 *   <imagePath>path/to/image.png</imagePath>
 *   <!-- the widths (per row) of each tile in pixels -->
 *   <widths>64, 64, 64, 64</widths>
 *   <!-- the heights (per row) of each tile in pixels -->
 *   <heights>48, 48, 48, 64</heights>
 *   <!-- the number of tiles in each row -->
 *   <tileCounts>16, 5, 3, 10</tileCounts>
 *   <!-- the offset in pixels to the upper left tile -->
 *   <offsetPos>8, 8</offsetPos>
 *   <!-- the gap between tiles in pixels -->
 *   <gapSize>12, 12</gapSize>
 * </tileset>
 * }
 */
public class SwissArmyTileSetRuleSet extends TileSetRuleSet
{
    @Override
    public void addRuleInstances (Digester digester)
    {
        super.addRuleInstances(digester);

        digester.addRule(
            _path + "/widths", new CallMethodSpecialRule() {
                @Override
                public void parseAndSet (String bodyText, Object target)
                {
                    int[] widths = StringUtil.parseIntArray(bodyText);
                    ((SwissArmyTileSet)target).setWidths(widths);
                }
            });

        digester.addRule(
            _path + "/heights", new CallMethodSpecialRule() {
                @Override
                public void parseAndSet (String bodyText, Object target)
                {
                    int[] heights = StringUtil.parseIntArray(bodyText);
                    ((SwissArmyTileSet)target).setHeights(heights);
                }
            });

        digester.addRule(
            _path + "/tileCounts",
            new CallMethodSpecialRule() {
                @Override
                public void parseAndSet (String bodyText, Object target)
                {
                    int[] tileCounts = StringUtil.parseIntArray(bodyText);
                    ((SwissArmyTileSet)target).setTileCounts(tileCounts);
                }
            });

        digester.addRule(
            _path + "/offsetPos", new CallMethodSpecialRule() {
                @Override
                public void parseAndSet (String bodyText, Object target)
                {
                    int[] values = StringUtil.parseIntArray(bodyText);
                    SwissArmyTileSet starget = (SwissArmyTileSet)target;
                    if (values.length == 2) {
                        starget.setOffsetPos(new Point(values[0], values[1]));
                    } else {
                        log.warning("Invalid 'offsetPos' definition '" +
                                    bodyText + "'.");
                    }
                }
            });

        digester.addRule(
            _path + "/gapSize", new CallMethodSpecialRule() {
                @Override
                public void parseAndSet (String bodyText, Object target)
                {
                    int[] values = StringUtil.parseIntArray(bodyText);
                    SwissArmyTileSet starget = (SwissArmyTileSet)target;
                    if (values.length == 2) {
                        starget.setGapSize(new Dimension(values[0], values[1]));
                    } else {
                        log.warning("Invalid 'gapSize' definition '" +
                                    bodyText + "'.");
                    }
                }
            });
    }

    @Override
    public boolean isValid (Object target)
    {
        SwissArmyTileSet set = (SwissArmyTileSet)target;
        boolean valid = super.isValid(target);

        // check for a <widths> element
        if (set.getWidths() == null) {
            log.warning("Tile set definition missing valid <widths> " +
                        "element [set=" + set + "].");
            valid = false;
        }

        // check for a <heights> element
        if (set.getHeights() == null) {
            log.warning("Tile set definition missing valid <heights> " +
                        "element [set=" + set + "].");
            valid = false;
        }

        // check for a <tileCounts> element
        if (set.getTileCounts() == null) {
            log.warning("Tile set definition missing valid <tileCounts> " +
                        "element [set=" + set + "].");
            valid = false;
        }

        return valid;
    }

    @Override
    protected Class<? extends TileSet> getTileSetClass ()
    {
        return SwissArmyTileSet.class;
    }
}
