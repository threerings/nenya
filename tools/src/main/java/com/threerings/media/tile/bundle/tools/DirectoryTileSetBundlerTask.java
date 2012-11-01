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

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;

/**
 * Ant task for creating tileset bundles that are placed in a specified directory instead
 *  of wrapped up in a fancy jar file.
 */
public class DirectoryTileSetBundlerTask extends TileSetBundlerTask
{
    /**
     * Sets the path to the directory in which we'll be putting our bundle's files.
     */
    public void setDeployDir (File deployDir)
    {
        _deployDir = deployDir;
    }

    @Override
    public void execute () throws BuildException
    {
        ensureSet(_deployDir, "Must specify the path to which we want to deploy tileset files.");
        super.execute();
    }

    @Override
    protected TileSetBundler createBundler ()
        throws IOException
    {
        return new DirectoryTileSetBundler(_config);
    }

    @Override
    protected String getTargetPath (File fromDir, String path)
    {
        File xmlFile = new File(path.replace(fromDir.getPath(), _deployDir.getPath()));
        return xmlFile.getParent();
    }

    /** The directory in which we want to place our tile set files for deployment. */
    protected File _deployDir;
}
