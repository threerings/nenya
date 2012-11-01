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

package com.threerings.media.tools;

import java.util.ArrayList;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import com.google.common.collect.Lists;

/**
 * Creates a file that lists all the resources in a fileset out to an index file.
 */
public class ResourceIndexerTask extends Task
{
    /**
     * Adds a nested &lt;fileset&gt; element.
     */
    public void addFileset (FileSet set)
    {
        _filesets.add(set);
    }

    public void setIndexFile (String file)
    {
        _indexFile = file;
    }

    @Override
    public void execute () throws BuildException
    {
        PrintWriter fout = null;
        try {
            fout = new PrintWriter(new FileWriter(getProject().getBaseDir() + "/" + _indexFile));

            for (FileSet fs : _filesets) {
                DirectoryScanner ds = fs.getDirectoryScanner(getProject());
                String[] srcFiles = ds.getIncludedFiles();
                for (String filename : srcFiles) {
                    fout.println(filename);
                }
            }

        } catch (IOException ioe) {
            throw new BuildException(ioe);
        } finally {
            if (fout != null) {
                fout.close();
            }
        }
    }

    /** A list of filesets that contain files to include in the index. */
    protected ArrayList<FileSet> _filesets = Lists.newArrayList();

    /** The name of the file to which we should write the index. */
    protected String _indexFile;
}
