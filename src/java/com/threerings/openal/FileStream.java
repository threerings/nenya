//
// $Id: OggFileStream.java 119 2007-01-24 00:22:12Z dhoover $
//
// Narya library - tools for developing networked games
// Copyright (C) 2002-2005 Three Rings Design, Inc., All Rights Reserved
// http://www.threerings.net/code/narya/
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

import java.util.HashMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.nio.ByteBuffer;

import java.util.ArrayList;

import org.lwjgl.openal.AL10;

/**
 * An audio stream read from one or more files.
 */
public class FileStream extends Stream
{
    /**
     * Registers a class of {@link StreamDecoder} for the specified file extension.
     */
    public static void registerExtension (String extension, Class clazz)
    {
        _extensions.put(extension, clazz);
    }

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
        _decoder = createDecoder(file);
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

    // documentation inherited
    protected int getFormat ()
    {
        return _decoder.getFormat();
    }

    // documentation inherited
    protected int getFrequency ()
    {
        return _decoder.getFrequency();
    }

    // documentation inherited
    protected int populateBuffer (ByteBuffer buf)
        throws IOException
    {
        int read = _decoder.read(buf);
        while (buf.hasRemaining() && (!_queue.isEmpty() || _file.loop)) {
            if (!_queue.isEmpty()) {
                _file = _queue.remove(0);
            }
            _decoder = createDecoder(_file.file);
            read = Math.max(0, read);
            read += _decoder.read(buf);
        }
        return read;
    }

    /**
     * Creates and initializes a stream decoder for the specified file.
     */
    protected StreamDecoder createDecoder (File file)
        throws IOException
    {
        String path = file.getPath();
        int idx = path.lastIndexOf('.');
        if (idx == -1) {
            Log.warning("Missing extension for file [file=" + path + "].");
            return null;
        }
        String extension = path.substring(idx+1);
        Class clazz = _extensions.get(extension);
        if (clazz == null) {
            Log.warning("No decoder registered for extension [extension=" + extension +
                ", file=" + path + "].");
            return null;
        }
        StreamDecoder decoder;
        try {
            decoder = (StreamDecoder)clazz.newInstance();
        } catch (Exception e) {
            Log.warning("Error instantiating decoder [file=" + path + ", error=" + e + "].");
            return null;
        }
        decoder.init(new FileInputStream(file));
        return decoder;
    }

    /** The file currently being played. */
    protected QueuedFile _file;

    /** The stream decoder for the current file. */
    protected StreamDecoder _decoder;

    /** The queue of files to play after the current one. */
    protected ArrayList<QueuedFile> _queue = new ArrayList<QueuedFile>();

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

    /** Registered decoder classes. */
    protected static HashMap<String, Class> _extensions = new HashMap<String, Class>();
    static {
        registerExtension("ogg", OggStreamDecoder.class);
        registerExtension("mp3", Mp3StreamDecoder.class);
    }
}
