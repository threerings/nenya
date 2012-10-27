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

package com.threerings.media.tile.bundle.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.Deflater;

/**
 * Writes files to disk either in a directory in a jar. The majority of the calling code doesn't
 * need to distinguish. Note that jars are not opened for appending, the class is intended to be
 * used in situations where the contents can be generated entirely.
 */
public class BundleWriter
{
    /**
     * Creates a new bundle writer that will write to the given target jar file.
     * @param target file to open as a jar
     * @param uncompressed whether compression is off
     */
    public BundleWriter (File target, final boolean uncompressed)
        throws IOException
    {
        _target = target;
        _dir = null;
        _jar = new JarImpl();
        _jar.uncompressed = uncompressed;
    }

    /**
     * Creates a new bundle writer that will write files into the given directory.
     * @param targetDir directory to write files to
     * @throws IOException if the directory cannot be created
     */
    public BundleWriter (File targetDir)
        throws IOException
    {
        _target = targetDir;
        _jar = null;
        _dir = new DirImpl();
    }

    /**
     * Opens a new file for writing in the jar or directory.
     */
    public OutputStream startNewFile (String path)
        throws IOException
    {
        if (_jar != null) {
            _jar.get().putNextEntry(new JarEntry(path));
            return _jar.get();
        } else {
            _dir.closeFile();
            return _dir.newFile(path);
        }
    }

    /**
     * Tests if the target is newer than the given time. Note that if we are in directory-mode,
     * false is returned.
     */
    public boolean isNewerThan (long newest)
    {
        if (_jar != null) {
            return _target.lastModified() > newest;
        }
        return false;
    }

    /**
     * Closes the writer.
     */
    public void close ()
        throws IOException
    {
        if (_jar != null) {
            _jar.get().close();
        } else {
            _dir.closeFile();
        }
    }

    public String toString ()
    {
        return _target.toString() + (_jar == null ? "/" : "");
    }

    /**
     * Deletes the target.
     */
    public boolean delete ()
    {
        if (_jar != null) {
            return _target.delete();
        }
        // TODO: delete directory
        return false;
    }

    protected class JarImpl
    {
        JarOutputStream get ()
            throws IOException
        {
            if (jar == null) {
                FileOutputStream fout = new FileOutputStream(_target);
                Manifest manifest = new Manifest();
                jar = new JarOutputStream(fout, manifest);
                jar.setLevel(uncompressed ? Deflater.NO_COMPRESSION : Deflater.BEST_COMPRESSION);
            }
            return jar;
        }

        JarOutputStream jar;
        boolean uncompressed;
    }

    protected class DirImpl
    {
        public DirImpl ()
            throws IOException
        {
            _target.mkdirs();
            if (!_target.isDirectory()) {
                throw new IOException("Not a directory: " + _target);
            }
        }

        public void closeFile ()
        {
            if (current != null) {
                try {
                    current.close();
                } catch (IOException ex) {
                    System.err.println("Unable to close file: " + current);
                }
                current = null;
            }
        }

        public OutputStream newFile (String path)
            throws IOException
        {
            return current = new FileOutputStream(new File(_target, path));
        }

        OutputStream current;
    }

    protected final File _target;
    protected final JarImpl _jar;
    protected final DirImpl _dir;
}
