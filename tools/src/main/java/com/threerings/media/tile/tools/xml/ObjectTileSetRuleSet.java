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

import com.samskivert.util.StringUtil;
import com.samskivert.xml.CallMethodSpecialRule;

import com.threerings.util.DirectionUtil;

import com.threerings.media.tile.ObjectTileSet;
import com.threerings.media.tile.TileSet;

/**
 * Parses {@link ObjectTileSet} instances from a tileset description. An
 * object tileset description looks like so:
 *
 * {@code
 * <tileset name="Sample Object Tileset">
 *   <imagePath>path/to/image.png</imagePath>
 *   <!-- the widths (per row) of each tile in pixels -->
 *   <widths>265</widths>
 *   <!-- the heights (per row) of each tile in pixels -->
 *   <heights>224</heights>
 *   <!-- the number of tiles in each row -->
 *   <tileCounts>4</tileCounts>
 *   <!-- the offset in pixels to the upper left tile -->
 *   <offsetPos>0, 0</offsetPos>
 *   <!-- the gap between tiles in pixels -->
 *   <gapSize>0, 0</gapSize>
 *   <!-- the widths (in unit tile count) of the objects -->
 *   <objectWidths>4, 3, 4, 3</objectWidths>
 *   <!-- the heights (in unit tile count) of the objects -->
 *   <objectHeights>3, 4, 3, 4</objectHeights>
 *   <!-- the default render priorities for these object tiles -->
 *   <priorities>0, 0, -1, 0</priorities>
 *   <!-- the constraints for these object tiles -->
 *   <constraints>ATTACH_N, ATTACH_E, ATTACH_S, ATTACH_W</constraints>
 * </tileset>
 * }
 */
public class ObjectTileSetRuleSet extends SwissArmyTileSetRuleSet
{
    @Override
    public void addRuleInstances (Digester digester)
    {
        super.addRuleInstances(digester);

        digester.addRule(
            _path + "/objectWidths",
            new CallMethodSpecialRule() {
                @Override
                public void parseAndSet (String bodyText, Object target)
                {
                    int[] widths = StringUtil.parseIntArray(bodyText);
                    ((ObjectTileSet)target).setObjectWidths(widths);
                }
            });

        digester.addRule(
            _path + "/objectHeights",
            new CallMethodSpecialRule() {
                @Override
                public void parseAndSet (String bodyText, Object target)
                {
                    int[] heights = StringUtil.parseIntArray(bodyText);
                    ((ObjectTileSet)target).setObjectHeights(heights);
                }
            });

        digester.addRule(
            _path + "/xOrigins", new CallMethodSpecialRule() {
                @Override
                public void parseAndSet (String bodyText, Object target)
                {
                    int[] xorigins = StringUtil.parseIntArray(bodyText);
                    ((ObjectTileSet)target).setXOrigins(xorigins);
                }
            });

        digester.addRule(
            _path + "/yOrigins", new CallMethodSpecialRule() {
                @Override
                public void parseAndSet (String bodyText, Object target)
                {
                    int[] yorigins = StringUtil.parseIntArray(bodyText);
                    ((ObjectTileSet)target).setYOrigins(yorigins);
                }
            });

        digester.addRule(
            _path + "/priorities",
            new CallMethodSpecialRule() {
                @Override
                public void parseAndSet (String bodyText, Object target)
                {
                    byte[] prios = StringUtil.parseByteArray(bodyText);
                    ((ObjectTileSet)target).setPriorities(prios);
                }
            });

        digester.addRule(
            _path + "/zations", new CallMethodSpecialRule() {
                @Override
                public void parseAndSet (String bodyText, Object target)
                {
                    String[] zations = StringUtil.parseStringArray(bodyText);
                    ((ObjectTileSet)target).setColorizations(zations);
                }
            });

        digester.addRule(
            _path + "/xspots", new CallMethodSpecialRule() {
                @Override
                public void parseAndSet (String bodyText, Object target)
                {
                    short[] xspots = StringUtil.parseShortArray(bodyText);
                    ((ObjectTileSet)target).setXSpots(xspots);
                }
            });

        digester.addRule(
            _path + "/yspots", new CallMethodSpecialRule() {
                @Override
                public void parseAndSet (String bodyText, Object target)
                {
                    short[] yspots = StringUtil.parseShortArray(bodyText);
                    ((ObjectTileSet)target).setYSpots(yspots);
                }
            });

        digester.addRule(
            _path + "/sorients", new CallMethodSpecialRule() {
                @Override
                public void parseAndSet (String bodyText, Object target)
                {
                    ObjectTileSet set = (ObjectTileSet)target;
                    String[] ostrs = StringUtil.parseStringArray(bodyText);
                    byte[] sorients = new byte[ostrs.length];
                    for (int ii = 0; ii < sorients.length; ii++) {
                        sorients[ii] = (byte)
                            DirectionUtil.fromShortString(ostrs[ii]);
                        if ((sorients[ii] == DirectionUtil.NONE) &&
                            // don't complain if they didn't even try to
                            // specify a valid direction
                            (! ostrs[ii].equals("-1"))) {
                            System.err.println("Invalid spot orientation " +
                                               "[set=" + set.getName() +
                                               ", idx=" + ii +
                                               ", orient=" + ostrs[ii] + "].");
                        }
                    }
                    set.setSpotOrients(sorients);
                }
            });

        digester.addRule(
            _path + "/constraints",
                new CallMethodSpecialRule() {
                @Override
                public void parseAndSet (String bodyText, Object target)
                {
                    String[] constrs = StringUtil.parseStringArray(
                        bodyText);
                    String[][] constraints = new String[constrs.length][];
                    for (int ii = 0; ii < constrs.length; ii++) {
                        constraints[ii] = constrs[ii].split("\\s*\\|\\s*");
                    }
                    ((ObjectTileSet)target).setConstraints(constraints);
                }
            });
    }

    @Override
    protected Class<? extends TileSet> getTileSetClass ()
    {
        return ObjectTileSet.class;
    }
}
