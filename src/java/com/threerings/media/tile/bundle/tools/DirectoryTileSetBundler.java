//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2007 Three Rings Design, Inc., All Rights Reserved
// http://www.threerings.net/code/nenya/
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;

import com.threerings.media.Log;
import com.threerings.util.FileUtil;
import com.threerings.media.tile.ImageProvider;
import com.threerings.media.tile.ObjectTileSet;
import com.threerings.media.tile.TileSet;
import com.threerings.media.tile.TrimmedObjectTileSet;
import com.threerings.media.tile.bundle.BundleUtil;
import com.threerings.media.tile.bundle.TileSetBundle;

public class DirectoryTileSetBundler extends TileSetBundler
{
    public DirectoryTileSetBundler (File configFile)
        throws IOException
    {
        super(configFile);
    }

    public DirectoryTileSetBundler (String configPath)
        throws IOException
    {
        super(configPath);
    }

    /**
     * Finish the creation of a tileset bundle jar file.
     *
     * @param target the tileset bundle file that will be created.
     * @param bundle contains the tilesets we'd like to save out to the bundle.
     * @param improv the image provider.
     * @param imageBase the base directory for getting images for non
     * ObjectTileSet tilesets.
     */
    public boolean createBundle (
        File target, TileSetBundle bundle, ImageProvider improv, String imageBase)
        throws IOException
    {
        try {
            // write all of the image files to the bundle's target path, converting the
            // tilesets to trimmed tilesets in the process
            Iterator iditer = bundle.enumerateTileSetIds();
            while (iditer.hasNext()) {
                int tileSetId = ((Integer)iditer.next()).intValue();
                TileSet set = bundle.getTileSet(tileSetId);
                String imagePath = set.getImagePath();

                // sanity checks
                if (imagePath == null) {
                    Log.warning("Tileset contains no image path " +
                                "[set=" + set + "]. It ain't gonna work.");
                    continue;
                }

                // if this is an object tileset, we can't trim it!
                if (set instanceof ObjectTileSet) {
                    // set the tileset up with an image provider; we
                    // need to do this so that we can trim it!
                    set.setImageProvider(improv);

                    // we're going to trim it, so adjust the path
                    imagePath = adjustImagePath(imagePath);

                    try {
                        // create a trimmed object tileset, which will
                        // write the trimmed tileset image to the destination
                        // output stream
                        File outFile = new File(target, imagePath);
                        outFile.getParentFile().mkdirs();
                        FileOutputStream fout = new FileOutputStream(outFile);
                        TrimmedObjectTileSet tset =
                            TrimmedObjectTileSet.trimObjectTileSet(
                                (ObjectTileSet)set, fout, "png");
                        tset.setImagePath(imagePath);
                        // replace the original set with the trimmed
                        // tileset in the tileset bundle
                        bundle.addTileSet(tileSetId, tset);

                    } catch (Exception e) {
                        e.printStackTrace(System.err);

                        String msg = "Error adding tileset to bundle " + imagePath +
                                     ", " + set.getName() + ": " + e;
                        throw (IOException) new IOException(msg).initCause(e);
                    }

                } else {
                    // read the image file and convert it to our custom
                    // format in the bundle
                    File ifile = new File(imageBase, imagePath);
                    try {
                        BufferedImage image = ImageIO.read(ifile);
                        File outFile = new File(target, imagePath);
                        outFile.getParentFile().mkdirs();
                        FileOutputStream fout = new FileOutputStream(outFile);
                        FileInputStream imgin = new FileInputStream(ifile);
                        IOUtils.copy(imgin, fout);
                    } catch (Exception e) {
                        String msg = "Failure bundling image " + ifile +
                            ": " + e;
                        throw (IOException) new IOException(msg).initCause(e);
                    }
                }
            }

            // now write a serialized representation of the tileset bundle
            // object to the bundle jar file
            File outFile = new File(target, BundleUtil.METADATA_PATH);
            outFile.getParentFile().mkdirs();
            FileOutputStream fout = new FileOutputStream(outFile);
            ObjectOutputStream oout = new ObjectOutputStream(fout);
            oout.writeObject(bundle);
            oout.flush();

            return true;

        } catch (Exception e) {
            String errmsg = "Failed to create bundle " + target + ": " + e;
            throw (IOException) new IOException(errmsg).initCause(e);
        }
    }

    @Override // documentation inherited
    protected long getTgtModificationDate (File target)
    {
        // Return the oldest modification date of anything within the directory.
        return FileUtil.getOldestLastModified(target);
    }
}
