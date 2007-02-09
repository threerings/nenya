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

import java.io.IOException;
import java.io.InputStream;

import java.nio.ByteBuffer;

/**
 * Decodes audio streams from data read from an {@link InputStream}.
 */
public interface StreamDecoder
{
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
}
