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

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

/**
 * Represents an OpenAL buffer object.
 */
public class Buffer
{
    /**
     * Creates a new buffer for the specified sound manager.
     */
    public Buffer (SoundManager soundmgr)
    {
        _soundmgr = soundmgr;
        IntBuffer idbuf = BufferUtils.createIntBuffer(1);
        AL10.alGenBuffers(idbuf);
        _id = idbuf.get(0);
    }

    /**
     * Returns this buffer's OpenAL identifier.
     */
    public final int getId ()
    {
        return _id;
    }

    /**
     * Sets the data in this buffer.
     */
    public void setData (int format, ByteBuffer data, int frequency)
    {
        AL10.alBufferData(_id, format, data, frequency);
    }

    /**
     * Sets the data in this buffer.
     */
    public void setData (int format, IntBuffer data, int frequency)
    {
        AL10.alBufferData(_id, format, data, frequency);
    }

    /**
     * Sets the data in this buffer.
     */
    public void setData (int format, ShortBuffer data, int frequency)
    {
        AL10.alBufferData(_id, format, data, frequency);
    }

    /**
     * Returns the size of this buffer.
     */
    public int getSize ()
    {
        return AL10.alGetBufferi(_id, AL10.AL_SIZE);
    }

    /**
     * Deletes this buffer, rendering it unusable.
     */
    public void delete ()
    {
        IntBuffer idbuf = BufferUtils.createIntBuffer(1);
        idbuf.put(_id).rewind();
        AL10.alDeleteBuffers(idbuf);
        _id = 0;
    }

    @Override
    protected void finalize ()
        throws Throwable
    {
        super.finalize();
        if (_id > 0) {
            _soundmgr.bufferFinalized(_id);
        }
    }

    /** The sound manager responsible for this buffer. */
    protected SoundManager _soundmgr;

    /** The OpenAL identifier for this buffer. */
    protected int _id;
}
