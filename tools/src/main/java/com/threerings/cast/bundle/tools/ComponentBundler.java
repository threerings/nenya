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

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.Deflater;

import com.google.common.collect.Lists;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ComparableArrayList;
import com.samskivert.util.FileUtil;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.Tuple;

import com.threerings.cast.ComponentIDBroker;
import com.threerings.cast.StandardActions;
import com.threerings.cast.bundle.BundleUtil;
import com.threerings.media.tile.ImageProvider;
import com.threerings.media.tile.SimpleCachingImageProvider;
import com.threerings.media.tile.TileSet;
import com.threerings.media.tile.TrimmedTileSet;

/**
 * Handles the logic of generating component bundles. Used by the Ant task and Maven plugin.
 */
public abstract class ComponentBundler {

    public ComponentBundler (File mapfile, File actionDef) {
        _mapfile = mapfile;
        _actionDef = actionDef;
    }

    protected boolean keepRawPngs () { return false; }
    protected boolean uncompressed () { return false; }

    protected void logInfo (String message) {
        System.out.println(message);
    }
    protected void logWarn (String message) {
        System.err.println(message);
    }

    public void execute (String root, File target, List<Tuple<File,List<String>>> sourceDirs) {
        // load the id broker
        HashMapIDBroker broker = new HashMapIDBroker();
        try {
            BufferedReader bin = new BufferedReader(new FileReader(_mapfile));
            broker.readFrom(bin);
            bin.close();
        } catch (FileNotFoundException fnfe) {
            // if the file doesn't yet exist, start with a blank broker
        } catch (Exception e) {
            throw new RuntimeException(
                "Error loading component ID map [mapfile=" + _mapfile + "]", e);
        }

        // load the action tilesets
        Map<String, TileSet> actsets;
        try {
            actsets = ComponentBundlerUtil.parseActionTileSets(_actionDef);
        } catch (FileNotFoundException fnfe) {
            throw new RuntimeException(
                "Unable to load action definition file [path=" + _actionDef.getPath() + "].", fnfe);
        } catch (Exception e) {
            throw new RuntimeException("Parsing error.", e);
        }

        // check to see if any of the source files are newer than the target file
        List<File> sources = Lists.newArrayList();
        for (Tuple<File,List<String>> source : sourceDirs) {
            File fromDir = source.left;
            for (String srcFile : source.right) {
                sources.add(new File(fromDir, srcFile));
            }
        }
        sources.add(_mapfile);
        sources.add(_actionDef);

        long newest = getNewestDate(sources);
        if (skipIfTargetNewer() && newest < target.lastModified()) {
            logInfo(target.getPath() + " is up to date.");
            return;
        }

        logInfo("Generating " + target + "...");

        try {
            // make sure we can create our bundle file
            OutputStream fout = createOutputStream(target);

            // we'll fill this with component id to tuple mappings
            HashIntMap<Tuple<String, String>> mapping = new HashIntMap<Tuple<String, String>>();

            // process our files; control is inverted here so that the Ant task can enumerate using
            // its internal data structures and the Maven plugin can use its
            for (Tuple<File,List<String>> source : sourceDirs) {
                File fromDir = source.left;
                for (String srcFile : source.right) {
                    File cfile = new File(fromDir, srcFile);
                    // determine the [class, name, action] triplet
                    String[] info = decomposePath(root, cfile.getPath());

                    // make sure we have an action tileset definition
                    TileSet aset = actsets.get(info[2]);
                    if (aset == null) {
                        logWarn("No tileset definition for component action '" + info[2] +
                                "' [class=" + info[0] + ", name=" + info[1] + "].");
                        continue;
                    }
                    aset.setImageProvider(_improv);

                    // obtain the component id from our id broker
                    int cid = broker.getComponentID(info[0], info[1]);
                    // add a mapping for this component
                    mapping.put(cid, new Tuple<String, String>(info[0], info[1]));

                    // process and store the main component image
                    processComponent(info, aset, cfile, fout, newest);

                    // pick up any auxiliary images as well like the shadow or
                    // crop files
                    String action = info[2];
                    String ext = BundleUtil.IMAGE_EXTENSION;
                    for (String element : AUX_EXTS) {
                        File afile = new File(FileUtil.resuffix(cfile, ext, element + ext));
                        if (afile.exists()) {
                            info[2] = action + element;
                            processComponent(info, aset, afile, fout, newest);
                        }
                    }
                }
            }

            // write our mapping table to the jar file as well
            if (!skipEntry(BundleUtil.COMPONENTS_PATH, newest)) {
                fout = nextEntry(fout, BundleUtil.COMPONENTS_PATH);
                ObjectOutputStream oout = new ObjectOutputStream(fout);
                oout.writeObject(mapping);
                oout.flush();
            }

            if (fout != null) {
                // seal up our jar file if we created one
                fout.close();
            }

        } catch (IOException ioe) {
            String errmsg = "Unable to create component bundle.";
            throw new RuntimeException(errmsg, ioe);

        } catch (PersistenceException pe) {
            String errmsg = "Unable to obtain component ID mapping.";
            throw new RuntimeException(errmsg, pe);
        }

        // save our updated component ID broker
        saveBroker(_mapfile, broker);
    }

    protected void processComponent (
        String[] info, TileSet aset, File cfile, OutputStream fout, long newest) throws IOException
    {
        // construct the path that'll go in the jar file
        String ipath = composePath(
            info, BundleUtil.IMAGE_EXTENSION);

        // If we decide that the entry is up to date and we don't need to process it, bail out.
        if (skipEntry(ipath, newest)) {
            return;
        }

        fout = nextEntry(fout, ipath);

        aset.setImagePath(cfile.getPath());

        TileSet tset;
        if (keepRawPngs()) {
            // We've elected to keep the pngs as they are and just stuff them into the jar.
            try {
                tset = aset;
                BufferedImage image = aset.getRawTileSetImage();
                ImageIO.write(image, "png", fout);
            } catch (Throwable t) {
                logWarn("Failure storing tileset in jar [class=" + info[0] + ", name=" + info[1] +
                        ", action=" + info[2] + ", srcimg=" + aset.getImagePath() + "].");
                String errmsg = "Failure trimming tileset.";
                throw new RuntimeException(errmsg, t);
            }

        } else {
            // create a trimmed tileset based on the source action tileset and
            // stuff the new trimmed image into the jar file at the same time
            try {
                tset = trim(aset, fout);
                tset.setImagePath(ipath);
            } catch (Throwable t) {
                logWarn("Failure trimming tileset [class=" + info[0] + ", name=" + info[1] +
                        ", action=" + info[2] + ", srcimg=" + aset.getImagePath() + "].");
                String errmsg = "Failure trimming tileset.";
                throw new RuntimeException(errmsg, t);
            }
        }

        // then write our trimmed tileset bundle data
        String tpath = composePath(info, BundleUtil.TILESET_EXTENSION);
        if (!skipEntry(tpath, newest) && !keepRawPngs()) {
            fout = nextEntry(fout, tpath);

            ObjectOutputStream oout = new ObjectOutputStream(fout);
            oout.writeObject(tset);
            oout.flush();
        }
    }

    protected long getNewestDate (List<File> sources)
    {
        long newest = 0L;
        for (int ii = 0; ii < sources.size(); ii++) {
            newest = Math.max(newest, sources.get(ii).lastModified());
        }
        return newest;
    }

    /**
     * Returns whether we should skip updating the bundle if the target is newer than any component.
     */
    protected boolean skipIfTargetNewer ()
    {
        return true;
    }

    /**
     * Decomposes the full path to a component image into a [class, name,
     * action] triplet.
     */
    protected String[] decomposePath (String root, String path)
    {
        // first strip off the root
        if (!path.startsWith(root)) {
            throw new RuntimeException("Can't bundle images outside the root directory " +
                                       "[root=" + root + ", path=" + path + "].");
        }
        path = path.substring(root.length());

        // strip off any preceding file separator
        if (path.startsWith(File.separator)) {
            path = path.substring(1);
        }

        // now strip off the file extension
        if (!path.endsWith(BundleUtil.IMAGE_EXTENSION)) {
            throw new RuntimeException("Can't bundle malformed image file [path=" + path + "].");
        }
        path = path.substring(0, path.length() - BundleUtil.IMAGE_EXTENSION.length());

        // now decompose the path; the component type and action must always be a single string but
        // the class can span multiple directories for easier component organization; thus
        // "male/head/goatee/standing" will be parsed as
        // [class=male/head, type=goatee, action=standing]
        String malmsg = "Can't decode malformed image path: '" + path + "'";
        String[] info = new String[3];
        int lsidx = path.lastIndexOf(File.separator);
        if (lsidx == -1) {
            throw new RuntimeException(malmsg);
        }
        info[2] = path.substring(lsidx+1);
        int slsidx = path.lastIndexOf(File.separator, lsidx-1);
        if (slsidx == -1) {
            throw new RuntimeException(malmsg);
        }
        info[1] = path.substring(slsidx+1, lsidx);
        info[0] = path.substring(0, slsidx);
        // we need to turn file separator characters (platform dependent) into jar path separator
        // characters (always forward slash)
        info[0].replace(File.separatorChar, '/');
        return info;
    }

    /**
     * Composes a triplet of [class, name, action] into the path that should be supplied to the
     * JarEntry that contains the associated image data.
     */
    protected String composePath (String[] info, String extension)
    {
        return (info[0] + "/" + info[1] + "/" + info[2] + extension);
    }

    /**
     * Creates the base output stream to which to write our bundle's files.
     */
    protected OutputStream createOutputStream (File target)
        throws IOException
    {
        // make sure the parent directory exists
        target.getParentFile().mkdirs();
        // now create our file
        JarOutputStream jout = new JarOutputStream(new FileOutputStream(target));
        jout.setLevel(uncompressed() ? Deflater.NO_COMPRESSION : Deflater.BEST_COMPRESSION);
        return jout;
    }

    /**
     * Advances to the next named entry in the bundle and returns the stream to which to write
     *  that entry.
     */
    protected OutputStream nextEntry (OutputStream lastEntry, String path)
        throws IOException
    {
        ((JarOutputStream)lastEntry).putNextEntry(new JarEntry(path));
        return lastEntry;
    }

    /**
     * Returns whether we should skip the specified entry in the bundle, presumably if it was
     *  already created and up to date.
     */
    protected boolean skipEntry (String path, long newest)
    {
        // If we're making the bundle, by default, we don't skip anything.
        return false;
    }

    /**
     * Converts the tileset to a trimmed tile set and saves it at the specified location.
     */
    protected TrimmedTileSet trim (TileSet aset, OutputStream fout) throws IOException
    {
        return TrimmedTileSet.trimTileSet(aset, fout);
    }

    /**
     * Stores a persistent representation of the supplied hashmap ID
     * broker in the specified file.
     */
    protected void saveBroker (File mapfile, HashMapIDBroker broker)
        throws RuntimeException
    {
        // bail if the broker wasn't modified
        if (!broker.isModified()) {
            return;
        }

        try {
            BufferedWriter bout = new BufferedWriter(new FileWriter(mapfile));
            broker.writeTo(bout);
            bout.close();
        } catch (IOException ioe) {
            throw new RuntimeException(
                "Unable to store component ID map [mapfile=" + mapfile + "]", ioe);
        }
    }

    protected static class HashMapIDBroker
        extends HashMap<Tuple<String, String>, Integer> implements ComponentIDBroker
    {
        public int getComponentID (String cclass, String cname)
            throws PersistenceException
        {
            Tuple<String, String> key = new Tuple<String, String>(cclass, cname);
            Integer cid = get(key);
            if (cid == null) {
                cid = Integer.valueOf(++_nextCID);
                put(key, cid);
            }
            return cid.intValue();
        }

        public void commit ()
            throws PersistenceException
        {
            // nothing doing
        }

        public boolean isModified ()
        {
            return _nextCID != _startCID;
        }

        public void writeTo (BufferedWriter bout)
            throws IOException
        {
            // write out our most recently assigned component id
            String cidline = "" + _nextCID;
            bout.write(cidline, 0, cidline.length());
            bout.newLine();

            // write out the keys and values
            ComparableArrayList<String> lines = new ComparableArrayList<String>();
            Iterator<Tuple<String, String>> keys = keySet().iterator();
            while (keys.hasNext()) {
                Tuple<String, String> key = keys.next();
                Integer value = get(key);
                String line = key.left + SEP_STR + key.right + SEP_STR + value;
                lines.add(line);
            }

            // sort the output
            lines.sort();

            // now write it to the file
            int lcount = lines.size();
            for (int ii = 0; ii < lcount; ii++) {
                String line = lines.get(ii);
                bout.write(line, 0, line.length());
                bout.newLine();
            }
        }

        public void readFrom (BufferedReader bin)
            throws IOException
        {
            // read in our most recently assigned component id
            _nextCID = readInt(bin);
            // keep track of this so that we can tell if we were modified
            _startCID = _nextCID;

            // now read in our keys and values
            String line;
            while ((line = bin.readLine()) != null) {
                String orig = line;
                int sidx = line.indexOf(SEP_STR);
                if (sidx == -1) {
                    throw new IOException("Malformed line '" + orig + "'");
                }
                String cclass = line.substring(0, sidx);
                line = line.substring(sidx + SEP_STR.length());
                sidx = line.indexOf(SEP_STR);
                if (sidx == -1) {
                    throw new IOException("Malformed line '" + orig + "'");
                }
                String cname = line.substring(0, sidx);
                line = line.substring(sidx + SEP_STR.length());
                try {
                    put(new Tuple<String, String>(cclass, cname), Integer.valueOf(line));
                } catch (NumberFormatException nfe) {
                    String err = "Malformed line, invalid code '" + orig + "'";
                    throw new IOException(err);
                }
            }
        }

        protected int readInt (BufferedReader bin)
            throws IOException
        {
            String line = bin.readLine();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException nfe) {
                throw new IOException("Expected number, got '" + line + "'");
            }
        }

        protected int _nextCID = 0;
        protected int _startCID = 0;
    }

    // an image provider for loading our component images
    protected final ImageProvider _improv = new SimpleCachingImageProvider() {
        @Override
        protected BufferedImage loadImage (String path)
            throws IOException {
            return ImageIO.read(new File(path));
        }
    };

    /** The path to our component map file. */
    protected final File _mapfile;

    /** The path to our action tilesets definition file. */
    protected final File _actionDef;

    /** Used to separate keys and values in the map file. */
    protected static final String SEP_STR = " := ";

    /** Used to process auxilliary tilesets. */
    protected static final String[] AUX_EXTS = { "_" + StandardActions.SHADOW_TYPE,
                                                 "_" + StandardActions.CROP_TYPE };

}
