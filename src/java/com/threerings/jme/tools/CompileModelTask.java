//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
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

package com.threerings.jme.tools;

import java.io.File;

import java.util.ArrayList;
import java.util.logging.Level;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import com.google.common.collect.Lists;

import com.jme.util.LoggingSystem;
import com.jmex.model.XMLparser.Converters.DummyDisplaySystem;

/**
 * An ant task for compiling 3D models defined in XML to fast-loading binary files.
 */
public class CompileModelTask extends Task
{
    public void setDest (File dest)
    {
        _dest = dest;
    }

    public void addFileset (FileSet set)
    {
        _filesets.add(set);
    }

    public void init () throws BuildException
    {
        // create a dummy display system
        new DummyDisplaySystem();
        LoggingSystem.getLogger().setLevel(Level.WARNING);
    }

    public void execute ()
        throws BuildException
    {
        String baseDir = getProject().getBaseDir().getPath();
        for (FileSet fs : _filesets) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            File fromDir = fs.getDir(getProject());
            for (String file : ds.getIncludedFiles()) {
                File source = new File(fromDir, file);
                File destDir = (_dest == null) ? source.getParentFile() :
                    new File(source.getParent().replaceAll(baseDir, _dest.getPath()));
                try {
                    CompileModel.compile(source, destDir);
                } catch (Exception e) {
                    System.err.println("Error compiling " + source + ": " + e);
                }
            }
        }
    }

    /** The directory in which we will generate our model output (in a directory tree mirroring the
     * source files. */
    protected File _dest;

    /** A list of filesets that contain XML models. */
    protected ArrayList<FileSet> _filesets = Lists.newArrayList();
}
