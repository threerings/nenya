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

import org.lwjgl.openal.AL10;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;

/**
 * Decodes Ogg Vorbis streams.
 */
public class OggStreamDecoder extends StreamDecoder
{
    @Override
    public void init (InputStream in)
        throws IOException
    {
        _in = in;
        _sync.init();
        _info.init();

        // read the first packet "manually" to make sure everything's kosher
        readChunk();
        if (_sync.pageout(_page) != 1) {
            throw new IOException("Input is not an Ogg bitstream.");
        }
        _stream.init(_page.serialno());
        _stream.reset();

        if (_stream.pagein(_page) < 0) {
            throw new IOException("Error reading first page of Ogg bitstream data.");
        }
        if (_stream.packetout(_packet) != 1) {
            throw new IOException("Error reading initial header packet.");
        }

        Comment comment = new Comment();
        comment.init();
        if (_info.synthesis_headerin(comment, _packet) < 0) {
            throw new IOException("Ogg bitstream does not contain Vorbis audio data.");
        }

        // two more packets in header
        for (int ii = 0; ii < 2; ii++) {
            if (!readPacket()) {
                throw new IOException("End of file before reading all Vorbis headers.");
            }
            _info.synthesis_headerin(comment, _packet);
        }
        _dsp.synthesis_init(_info);
        _block.init(_dsp);
        _offsets = new int[_info.channels];
    }

    @Override
    public int getFormat ()
    {
        return (_info.channels == 1) ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
    }

    @Override
    public int getFrequency ()
    {
        return _info.rate;
    }

    @Override
    public int read (ByteBuffer buf)
        throws IOException
    {
        ShortBuffer sbuf = buf.asShortBuffer();
        int channels = _info.channels;
        int total = 0;
        while (sbuf.hasRemaining()) {
            int samples = Math.min(readSamples(), sbuf.remaining() / channels);
            if (samples == 0) {
                break;
            }
            int length = samples * channels;
            if (_data.length < length) {
                _data = new short[length];
            }
            short[] data = _data;
            for (int ii = 0; ii < channels; ii++) {
                float[] pcm = _pcm[0][ii];
                int sidx = _offsets[ii], didx = ii;
                for (int jj = 0; jj < samples; jj++) {
                    float value = Math.min(Math.max(pcm[sidx], -1f), +1f);
                    data[didx] = (short)(value * 32767f);
                    sidx++;
                    didx += channels;
                }
            }
            sbuf.put(_data, 0, length);
            _dsp.synthesis_read(samples);
            total += (length * 2);
        }
        buf.position(buf.position() + total);
        return total;
    }

    /**
     * Reads a buffer's worth of samples.
     *
     * @return the number of samples read, or zero if we've reached the end of the stream.
     */
    protected int readSamples ()
        throws IOException
    {
        int samples;
        while ((samples = _dsp.synthesis_pcmout(_pcm, _offsets)) <= 0) {
            if (samples == 0 && !readPacket()) {
                return 0;
            }
            if (_block.synthesis(_packet) == 0) {
                _dsp.synthesis_blockin(_block);
            }
        }
        return samples;
    }

    /**
     * Reads a packet.
     *
     * @return true if a packet was read, false if we've reached the end of the stream.
     */
    protected boolean readPacket ()
        throws IOException
    {
        int result;
        while ((result = _stream.packetout(_packet)) != 1) {
            if (result == 0 && !readPage()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Reads in a page.
     *
     * @return true if a page was read, false if we've reached the end of the stream.
     */
    protected boolean readPage ()
        throws IOException
    {
        int result;
        while ((result = _sync.pageout(_page)) != 1) {
            if (result == 0 && !readChunk()) {
                return false;
            }
        }
        _stream.pagein(_page);
        return true;
    }

    /**
     * Reads in a chunk of data from the underlying input stream.
     *
     * @return true if a chunk was read, false if we've reached the end of the stream.
     */
    protected boolean readChunk ()
        throws IOException
    {
        int offset = _sync.buffer(BUFFER_SIZE);
        int bytes = _in.read(_sync.data, offset, BUFFER_SIZE);
        if (bytes > 0) {
            _sync.wrote(bytes);
            return true;
        }
        return false;
    }

    /** The underlying input stream. */
    protected InputStream _in;

    /** The sync state. */
    protected SyncState _sync = new SyncState();

    /** The stream state. */
    protected StreamState _stream = new StreamState();

    /** The output page. */
    protected Page _page = new Page();

    /** The output packet. */
    protected Packet _packet = new Packet();

    /** The stream info. */
    protected Info _info = new Info();

    /** The DSP state. */
    protected DspState _dsp = new DspState();

    /** The output block. */
    protected Block _block = new Block(_dsp);

    /** Holds the decoded PCM data buffers. */
    protected float[][][] _pcm = new float[1][][];

    /** The DSP offsets for each channel. */
    protected int[] _offsets;

    /** Intermediate storage for converted data. */
    protected short[] _data = new short[0];

    /** The decode buffer size. */
    protected static final int BUFFER_SIZE = 4096 * 2;
}
