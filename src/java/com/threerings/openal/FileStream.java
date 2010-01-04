//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
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

package com.threerings.openal;

import java.util.ArrayList;

import java.io.File;
import java.io.IOException;

import java.nio.ByteBuffer;

import com.google.common.collect.Lists;

/**
 * An audio stream read from one or more files.
 */
public class FileStream extends Stream
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
        super(soundmgr);
        _file = new QueuedFile(file, loop);
        _decoder = StreamDecoder.createInstance(file);
    }

    /**
     * Adds a file to the queue of files to play.
     *
     * @param loop if true, play this file in a loop if there's nothing else
     * on the queue
     */
    public void queueFile (File file, boolean loop)
    {
        _queue.add(new QueuedFile(file, loop));
    }

    @Override
    protected int getFormat ()
    {
        return _decoder.getFormat();
    }

    @Override
    protected int getFrequency ()
    {
        return _decoder.getFrequency();
    }

    @Override
    protected int populateBuffer (ByteBuffer buf)
        throws IOException
    {
        int read = _decoder.read(buf);
        while (buf.hasRemaining() && (!_queue.isEmpty() || _file.loop)) {
            if (!_queue.isEmpty()) {
                _file = _queue.remove(0);
            }
            _decoder = StreamDecoder.createInstance(_file.file);
            read = Math.max(0, read);
            read += _decoder.read(buf);
        }
        return read;
    }

    /** The file currently being played. */
    protected QueuedFile _file;

    /** The stream decoder for the current file. */
    protected StreamDecoder _decoder;

    /** The queue of files to play after the current one. */
    protected ArrayList<QueuedFile> _queue = Lists.newArrayList();

    /** A file queued for play. */
    protected class QueuedFile
    {
        /** The file to play. */
        public File file;

        /** Whether or not to play the file in a loop when there's nothing
         * in the queue. */
        public boolean loop;

        public QueuedFile (File file, boolean loop)
        {
            this.file = file;
            this.loop = loop;
        }
    }
}
