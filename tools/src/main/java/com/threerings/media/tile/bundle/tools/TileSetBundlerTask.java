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

import java.util.ArrayList;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import com.google.common.collect.Lists;

import com.threerings.media.tile.tools.MapFileTileSetIDBroker;

/**
 * Ant task for creating tilset bundles.
 */
public class TileSetBundlerTask extends Task
{
    /**
     * Sets the path to the bundler configuration file that we'll use when
     * creating the bundle.
     */
    public void setConfig (File config)
    {
        _config = config;
    }

    /**
     * Sets the path to the tileset id mapping file we'll use when
     * creating the bundle.
     */
    public void setMapfile (File mapfile)
    {
        _mapfile = mapfile;
    }

    /**
     * Adds a nested &lt;fileset&gt; element.
     */
    public void addFileset (FileSet set)
    {
        _filesets.add(set);
    }

    /**
     * Note whether we are supposed to use the raw png files directly in the bundle or try to
     *  re-encode them.
     */
    public void setKeepRawPngs (boolean keep)
    {
        _keepRawPngs = keep;
    }

    /**
     * Note whether we are supposed to leave the jar uncompressed rather than the normal process
     *  of zipping it at maximum compression.
     */
    public void setUncompressed (boolean uncompressed)
    {
        _uncompressed = uncompressed;
    }

    /**
     * Performs the actual work of the task.
     */
    @Override
    public void execute () throws BuildException
    {
        // make sure everything was set up properly
        ensureSet(_config, "Must specify the path to the bundler config " +
                  "file via the 'config' attribute.");
        ensureSet(_mapfile, "Must specify the path to the tileset id map " +
                  "file via the 'mapfile' attribute.");

        File cfile = null;
        try {
            // create a tileset bundler
            TileSetBundler bundler = createBundler();

            // create our tileset id broker
            MapFileTileSetIDBroker broker =
                new MapFileTileSetIDBroker(_mapfile);

            // deal with the filesets
            for (int ii = 0; ii < _filesets.size(); ii++) {
                FileSet fs = _filesets.get(ii);
                DirectoryScanner ds = fs.getDirectoryScanner(getProject());
                File fromDir = fs.getDir(getProject());
                String[] srcFiles = ds.getIncludedFiles();

                for (String srcFile : srcFiles) {
                    cfile = new File(fromDir, srcFile);

                    // figure out the bundle file based on the definition
                    // file
                    String cpath = cfile.getPath();
                    if (!cpath.endsWith(".xml")) {
                        System.err.println("Can't infer bundle name from " +
                                           "bundle config name " +
                                           "[path=" + cpath + "].\n" +
                                           "Config file should end with .xml.");
                        continue;
                    }
                    String bpath = getTargetPath(fromDir, cpath);
                    File bfile = new File(bpath);

                    // create the bundle
                    if (bundler.createBundle(broker, cfile, bfile)) {
                        System.out.println(
                            "Created bundle from '" + cpath + "'...");
                    } else {
                        System.out.println(
                            "Tileset bundle up to date '" + bpath + "'.");
                    }
                }
            }

            // commit changes to the tileset id mapping
            broker.commit();

        } catch (Exception e) {
            String errmsg = "Failure creating tileset bundle [source=" + cfile +
                "]: " + e.getMessage();
            throw new BuildException(errmsg, e);
        }
    }

    /**
     * Create the bundler to use during creation.
     */
    protected TileSetBundler createBundler ()
        throws IOException
    {
        return new TileSetBundler(_config, _keepRawPngs, _uncompressed);
    }

    /**
     * Returns the target path in which our bundler will write the tile set.
     */
    protected String getTargetPath (File fromDir, String path)
    {
        return path.substring(0, path.length()-4) + ".jar";
    }

    protected void ensureSet (Object value, String errmsg)
        throws BuildException
    {
        if (value == null) {
            throw new BuildException(errmsg);
        }
    }

    protected File _config;
    protected File _mapfile;

    /** A list of filesets that contain tileset bundle definitions. */
    protected ArrayList<FileSet> _filesets = Lists.newArrayList();

    /** Whether we should keep raw pngs rather than reencoding them in the bundle. */
    protected boolean _keepRawPngs;

    /** Whether we should keep the bundle jars uncompressed rather than zipped. */
    protected boolean _uncompressed;
}
