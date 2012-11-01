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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Creates all the information that would be in a metadata bundle and place it in a directory.
 */
public class DirectoryMetadataBundlerTask extends MetadataBundlerTask
{
    @Override
    protected OutputStream createOutputStream (File target)
        throws IOException
    {
        // Since we recreate our output stream on every entry, we don't need one to start with.
        return null;
    }

    @Override
    protected OutputStream nextEntry (OutputStream lastEntry, String path)
        throws IOException
    {
        File file = new File(_target, path);
        file.getParentFile().mkdirs();
        return new FileOutputStream(file);
    }
}
