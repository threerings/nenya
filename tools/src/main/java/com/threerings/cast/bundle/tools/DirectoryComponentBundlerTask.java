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

import com.threerings.media.tile.TileSet;
import com.threerings.media.tile.TrimmedTileSet;

/**
 * Creates all the information for a component bundle but places it into a specified directory
 * rather than a bundle jar file.
 */
public class DirectoryComponentBundlerTask extends ComponentBundlerTask
{
    @Override protected ComponentBundler createBundler () {
        return new ComponentBundler(_mapfile, _actionDef) {
            @Override protected boolean keepRawPngs () { return _keepRawPngs; }
            @Override protected boolean uncompressed () { return _uncompressed; }

            @Override protected OutputStream createOutputStream (File target) throws IOException {
                // we recreate our output stream on every entry; we don't need one to start with
                return null;
            }

            @Override protected OutputStream nextEntry (OutputStream lastEntry,
                                                        String path) throws IOException {
                File file = new File(_target, path);
                file.getParentFile().mkdirs();
                if (!file.getParentFile().isDirectory()) {
                    throw new IOException(
                        "Unable to make component directory. [dir=" + file.getParentFile() + "]");
                }
                return new FileOutputStream(file);
            }

            @Override protected boolean skipEntry (String path, long newest) {
                File file = new File(_target, path);
                return (file.lastModified() > newest);
            }

            @Override protected TrimmedTileSet trim (TileSet aset,
                                                     OutputStream fout) throws IOException {
                return TrimmedTileSet.trimTileSet(aset, fout, "png");
            }

            @Override protected boolean skipIfTargetNewer () {
                return false; // we have to check mod later on a file-by-file basis, so can't skip
            }
        };
    }
 }
