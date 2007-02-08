//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2006 Three Rings Design, Inc., All Rights Reserved
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import java.util.ArrayList;

import org.lwjgl.openal.AL10;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.decoder.SampleBuffer;

/**
 * An audio stream read from one or more mp3 files.
 */
public class Mp3FileStream extends Stream
{
    /**
     * Creates a new stream for the specified file.
     *
     * @param loop whether or not to play the file in a continuous loop if there's nothing on the
     * queue
     */
    public Mp3FileStream (SoundManager soundmgr, File file, boolean loop)
        throws IOException
    {
        super(soundmgr);
        _file = new Mp3File(file, loop);
        _istream = new Bitstream(new FileInputStream(file));
        _decoder = new Decoder();
    }

    /**
     * Adds a file to the queue of files to play.
     *
     * @param loop if true, play this file in a loop if there's nothing else on the queue
     */
    public void queueFile (File file, boolean loop)
    {
        _queue.add(new Mp3File(file, loop));
    }

    @Override // from Stream
    public void dispose ()
    {
        super.dispose();

        // TODO: close our bitstream...
    }

    // documentation inherited
    protected int getFormat ()
    {
        return AL10.AL_FORMAT_STEREO16;
    }

    // documentation inherited
    protected int getFrequency ()
    {
        return _frequency;
    }

    // documentation inherited
    protected int populateBuffer (ByteBuffer buf)
        throws IOException
    {
        if (_buffer == null) {
            try {
                Header h = _istream.readFrame();
                for (int ii = 0; ii < 10 && (h == null); ii++) {
                    _istream.close();
                    Mp3File file = getNextFile();
                    if (file == null) {
                        return 0;
                    }
                    _istream = new Bitstream(new FileInputStream(file.file));
                    h = _istream.readFrame();
                }

                _buffer = (SampleBuffer)_decoder.decodeFrame(h, _istream);
                _frequency = _buffer.getSampleFrequency();

            } catch (JavaLayerException jle) {
                throw (IOException)new IOException("JavaLayer decoding error").initCause(jle);
            }
        }

        int srem = buf.remaining();
        ShortBuffer sbuf = buf.asShortBuffer();
        int wrote = Math.min(_buffer.getBufferLength()-_offset, sbuf.remaining());
        sbuf.put(_buffer.getBuffer(), _offset, wrote);
        if (_offset + wrote >= _buffer.getBufferLength()) {
            _buffer = null;
            _offset = 0;
            _istream.closeFrame();
        }

        return 2 * wrote;
    }

    protected Mp3File getNextFile ()
    {
        if (_file.loop) {
            return _file;
        } else if (!_queue.isEmpty()) {
            return _queue.remove(0);
        } else {
            return null;
        }
    }

    /** The file currently being played. */
    protected Mp3File _file;

    /** Handles reading the mp3 data. */
    protected Bitstream _istream;

    /** Handles decoding the mp3 data. */
    protected Decoder _decoder;

    /** The currently decoded frame, if any. */
    protected SampleBuffer _buffer;

    /** Our offset into the currently decoded frame. */
    protected int _offset;

    /** The frequency of the most recently processed frame. */
    protected int _frequency;

    /** The queue of files to play after the current one. */
    protected ArrayList<Mp3File> _queue = new ArrayList<Mp3File>();

    /** A file queued for play. */
    protected class Mp3File
    {
        /** The file to play. */
        public File file;

        /** Whether or not to play the file in a loop when there's nothing in the queue. */
        public boolean loop;

        public Mp3File (File file, boolean loop) {
            this.file = file;
            this.loop = loop;
        }
    }
}
