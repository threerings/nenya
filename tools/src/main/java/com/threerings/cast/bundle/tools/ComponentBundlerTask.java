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

import java.util.Arrays;
import java.util.List;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import com.google.common.collect.Lists;

import com.samskivert.util.Tuple;

/**
 * Ant task for creating component bundles. This task must be configured
 * with a number of parameters:
 *
 * <pre>
 * target=[path to bundle file, which will be created]
 * mapfile=[path to the component map file which maintains a mapping from
 *          component id to component class/name, it will be created the
 *          first time and updated as new components are mapped]
 * </pre>
 *
 * It should also contain one or more nested &lt;fileset&gt; elements that
 * enumerate the action tileset images that should be included in the
 * component bundle.
 */
public class ComponentBundlerTask extends Task
{
    /**
     * Sets the path to the bundle file that we'll be creating.
     */
    public void setTarget (File target)
    {
        _target = target;
    }

    /**
     * Sets the path to the component map file that we'll use to obtain
     * component ids for the bundled components.
     */
    public void setMapfile (File mapfile)
    {
        _mapfile = mapfile;
    }

    /**
     * Sets the path to the action tilesets definition file.
     */
    public void setActiondef (File actiondef)
    {
        _actionDef = actiondef;
    }

    /**
     * Sets the root path which will be stripped from the image paths
     * prior to parsing them to obtain the component class, type and
     * action names.
     */
    public void setRoot (File root)
    {
        _root = root.getPath();
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
        ensureSet(_target, "Must specify the path to the target bundle " +
                  "file via the 'target' attribute.");
        ensureSet(_mapfile, "Must specify the path to the component map " +
                  "file via the 'mapfile' attribute.");
        ensureSet(_actionDef, "Must specify the action sequence " +
                  "definitions via the 'actiondef' attribute.");

        // enumerate all the dirs/files to be processed
        List<Tuple<File,List<String>>> sourceDirs = Lists.newArrayList();
        for (FileSet fs : _filesets) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            File fromDir = fs.getDir(getProject());
            String[] srcFiles = ds.getIncludedFiles();
            sourceDirs.add(Tuple.newTuple(fromDir, Arrays.asList(srcFiles)));
        }

        // do the deed!
        createBundler().execute(_root, _target, sourceDirs);
    }

    protected ComponentBundler createBundler () {
        return new ComponentBundler(_mapfile, _actionDef) {
            @Override protected boolean keepRawPngs () { return _keepRawPngs; }
            @Override protected boolean uncompressed () { return _uncompressed; }
        };
    }

    protected void ensureSet (Object value, String errmsg) throws BuildException
    {
        if (value == null) throw new BuildException(errmsg);
    }

    /** The path to our component bundle file. */
    protected File _target;

    /** The path to our component map file. */
    protected File _mapfile;

    /** The path to our action tilesets definition file. */
    protected File _actionDef;

    /** The component directory root. */
    protected String _root;

    /** A list of filesets that contain tile images. */
    protected List<FileSet> _filesets = Lists.newArrayList();

    /** Whether we should keep raw pngs rather than reencoding them in the bundle. */
    protected boolean _keepRawPngs;

    /** Whether we should keep the bundle jars uncompressed rather than zipped. */
    protected boolean _uncompressed;
}
