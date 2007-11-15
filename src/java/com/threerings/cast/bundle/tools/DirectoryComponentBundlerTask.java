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

package com.threerings.cast.bundle.tools;

import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.threerings.util.FileUtil;

import com.threerings.media.tile.TileSet;
import com.threerings.media.tile.TrimmedTileSet;

/**
 * Creates all the information for a component bundle but places it into a specified directory
 *  rather than a bundle jar file.
 */
public class DirectoryComponentBundlerTask extends ComponentBundlerTask
{
    @Override // documentation inherited.
    protected OutputStream createOutputStream (File target)
        throws IOException
    {
        // Since we recreate our output stream on every entry, we don't need one to start with.
        return null;
    }



    @Override // documentation inherited
    protected OutputStream nextEntry (OutputStream lastEntry, String path)
        throws IOException
    {
        File file = new File(_target, path);
        file.getParentFile().mkdirs();
        return new FileOutputStream(file);
    }

    @Override // documentation inherited
    protected boolean skipEntry (String path, long newest)
    {
        File file = new File(_target, path);
        return (file.lastModified() > newest);
    }

    @Override // documentation inherited
    protected TrimmedTileSet trim (TileSet aset, OutputStream fout)
        throws IOException
    {
        return TrimmedTileSet.trimTileSet(aset, fout, "png");
    }

    @Override // documentation inherited
    protected boolean skipIfTargetNewer ()
    {
        // We have to check modification later on a file-by-file basis, so cannot skip.
        return false;
    }
 }
