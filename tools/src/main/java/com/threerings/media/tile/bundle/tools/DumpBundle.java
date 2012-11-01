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

package com.threerings.media.tile.bundle.tools;

import java.util.Iterator;

import java.io.File;

import com.threerings.resource.FileResourceBundle;
import com.threerings.resource.ResourceBundle;

import com.threerings.media.tile.TileSet;
import com.threerings.media.tile.bundle.BundleUtil;
import com.threerings.media.tile.bundle.TileSetBundle;

/**
 * Dumps the contents of a tileset bundle to stdout (just the serialized
 * object info, not the image data).
 */
public class DumpBundle
{
    public static void main (String[] args)
    {
        boolean dumpTiles = false;

        if (args.length < 1) {
            String usage = "Usage: DumpBundle [-tiles] " +
                "(bundle.jar|tsbundle.dat) [...]";
            System.err.println(usage);
            System.exit(-1);
        }

        for (String arg : args) {
            // oh the hackery
            if (arg.equals("-tiles")) {
                dumpTiles = true;
                continue;
            }

            File file = new File(arg);
            try {
                TileSetBundle tsb = null;
                if (arg.endsWith(".jar")) {
                    ResourceBundle bundle = new FileResourceBundle(file);
                    tsb = BundleUtil.extractBundle(bundle);
                    tsb.init(bundle);
                } else {
                    tsb = BundleUtil.extractBundle(file);
                }

                Iterator<Integer> tsids = tsb.enumerateTileSetIds();
                while (tsids.hasNext()) {
                    Integer tsid = tsids.next();
                    TileSet set = tsb.getTileSet(tsid.intValue());
                    System.out.println(tsid + " => " + set);
                    if (dumpTiles) {
                        for (int t = 0, nn = set.getTileCount(); t < nn; t++) {
                            System.out.println("  " + t + " => " +
                                               set.getTile(t));
                        }
                    }
                }

            } catch (Exception e) {
                System.err.println("Error dumping bundle [path=" + arg +
                                   ", error=" + e + "].");
                e.printStackTrace();
            }
        }
    }
}
