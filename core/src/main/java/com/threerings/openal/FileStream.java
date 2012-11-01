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

package com.threerings.openal;

import java.io.File;
import java.io.IOException;

import java.net.MalformedURLException;

import static com.threerings.openal.Log.log;

/**
 * An audio stream read from one or more files.
 */
public class FileStream extends URLStream
{
    /**
     * Creates a new stream for the specified file.
     *
     * @param loop whether or not to play the file in a continuous loop
     * if there's nothing on the queue
     */
    public FileStream (SoundManager soundmgr, File file, boolean loop)
        throws IOException
    {
        super(soundmgr, file.toURI().toURL(), loop);
    }

    /**
     * Adds a file to the queue of files to play.
     *
     * @param loop if true, play this file in a loop if there's nothing else
     * on the queue
     */
    public void queueFile (File file, boolean loop)
    {
        try {
            queueURL(file.toURI().toURL(), loop);
        } catch (MalformedURLException e) {
            log.warning("Invalid file url.", "file", file, e);
        }
    }
}
