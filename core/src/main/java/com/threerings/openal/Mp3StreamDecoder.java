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

import java.io.IOException;
import java.io.InputStream;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.decoder.SampleBuffer;

import org.lwjgl.openal.AL10;

/**
 * Decodes MP3 streams.
 */
public class Mp3StreamDecoder extends StreamDecoder
{
    @Override
    public void init (InputStream in)
        throws IOException
    {
        _istream = new Bitstream(in);
        try {
            _header = _istream.readFrame();
        } catch (JavaLayerException e) {
            throw new IOException(e.toString());
        }
        _decoder = new Decoder();
    }

    @Override
    public int getFormat ()
    {
        return AL10.AL_FORMAT_STEREO16;
    }

    @Override
    public int getFrequency ()
    {
        return _header.frequency();
    }

    @Override
    public int read (ByteBuffer buf)
        throws IOException
    {
        ShortBuffer sbuf = buf.asShortBuffer();
        int total = 0;
        while (sbuf.hasRemaining() && _header != null) {
            if (_buffer == null) {
                try {
                    _buffer = (SampleBuffer)_decoder.decodeFrame(_header, _istream);
                    _istream.closeFrame();
                    _header = _istream.readFrame();
                } catch (JavaLayerException e) {
                    throw new IOException(e.toString());
                }
            }
            int blen = _buffer.getBufferLength(),
                length = Math.min(sbuf.remaining(), blen - _offset);
            sbuf.put(_buffer.getBuffer(), _offset, length);
            if ((_offset += length) >= blen) {
                _offset = 0;
                _buffer = null;
            }
            total += (length * 2);
        }
        buf.position(buf.position() + total);
        return total;
    }

    /** Handles reading the mp3 data. */
    protected Bitstream _istream;

    /** The mp3 header. */
    protected Header _header;

    /** Handles decoding the mp3 data. */
    protected Decoder _decoder;

    /** The sample buffer used for output. */
    protected SampleBuffer _buffer;

    /** Our offset into the currently decoded frame. */
    protected int _offset;
}
