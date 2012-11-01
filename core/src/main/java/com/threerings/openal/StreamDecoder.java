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

import java.util.HashMap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.nio.ByteBuffer;

import com.google.common.collect.Maps;

import static com.threerings.openal.Log.log;

/**
 * Decodes audio streams from data read from an {@link InputStream}.
 */
public abstract class StreamDecoder
{
    /**
     * Registers a class of {@link StreamDecoder} for the specified file extension.
     */
    public static void registerExtension (String extension, Class<? extends StreamDecoder> clazz)
    {
        _extensions.put(extension, clazz);
    }

    /**
     * Creates and initializes a stream decoder for the specified file.
     */
    public static StreamDecoder createInstance (File file)
        throws IOException
    {
        return createInstance(file.toURI().toURL());
    }

    /**
     * Creates and initializes a stream decoder for the specified URL.
     */
    public static StreamDecoder createInstance (URL url)
        throws IOException
    {
        String path = url.getPath();
        int idx = path.lastIndexOf('.');
        if (idx == -1) {
            log.warning("Missing extension for URL.", "url", url);
            return null;
        }
        String extension = path.substring(idx+1);
        Class<? extends StreamDecoder> clazz = _extensions.get(extension);
        if (clazz == null) {
            log.warning("No decoder registered for extension.",
                "extension", extension, "url", url);
            return null;
        }
        StreamDecoder decoder;
        try {
            decoder = clazz.newInstance();
        } catch (Exception e) {
            log.warning("Error instantiating decoder.", "url", url, e);
            return null;
        }
        decoder.init(url.openStream());
        return decoder;
    }

    /**
     * Initializes the decoder with its input stream.
     */
    public abstract void init (InputStream in)
        throws IOException;

    /**
     * Returns the sound format (see {@link Stream#getFormat}).
     */
    public abstract int getFormat ();

    /**
     * Returns the sound frequency (see {@link Stream#getFrequency}).
     */
    public abstract int getFrequency ();

    /**
     * Reads as much data as will fit into the specified buffer.
     *
     * @return the number of bytes read.  If less than or equal to zero, the decoder has reached
     * the end of the stream.
     */
    public abstract int read (ByteBuffer buf)
        throws IOException;

    /** Maps file extensions to decoder classes. */
    protected static HashMap<String, Class<? extends StreamDecoder>> _extensions =
        Maps.newHashMap();
    static {
        registerExtension("ogg", OggStreamDecoder.class);
        registerExtension("mp3", Mp3StreamDecoder.class);
    }
}
