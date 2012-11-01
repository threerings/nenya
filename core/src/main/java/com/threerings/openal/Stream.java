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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import org.lwjgl.openal.AL10;

import static com.threerings.openal.Log.log;

/**
 * Represents a streaming source of sound data.
 */
public abstract class Stream
{
    /**
     * Creates a new stream.  Call {@link #dispose} when finished with the stream.
     *
     * @param soundmgr a reference to the sound manager that will update the stream
     */
    public Stream (SoundManager soundmgr)
    {
        _soundmgr = soundmgr;

        // create the source and buffers
        _source = new Source(soundmgr);
        for (int ii = 0; ii < _buffers.length; ii++) {
            _buffers[ii] = new Buffer(soundmgr);
        }

        // register with sound manager
        _soundmgr.addStream(this);
    }

    /**
     * Sets the base gain of the stream.
     */
    public void setGain (float gain)
    {
        _gain = gain;
        if (_fadeMode == FadeMode.NONE) {
            _source.setGain(_gain);
        }
    }

    /**
     * Returns a reference to the stream source.
     */
    public Source getSource ()
    {
        return _source;
    }

    /**
     * Determines whether this stream is currently playing.
     */
    public boolean isPlaying ()
    {
        return _state == AL10.AL_PLAYING;
    }

    /**
     * Starts playing this stream.
     */
    public void play ()
    {
        if (_state == AL10.AL_PLAYING) {
            log.warning("Tried to play stream already playing.");
            return;
        }
        if (_state == AL10.AL_INITIAL) {
            _qidx = _qlen = 0;
            queueBuffers(_buffers.length);
        }
        _source.play();
        _state = AL10.AL_PLAYING;
    }

    /**
     * Pauses this stream.
     */
    public void pause ()
    {
        if (_state != AL10.AL_PLAYING) {
            log.warning("Tried to pause stream that wasn't playing.");
            return;
        }
        _source.pause();
        _state = AL10.AL_PAUSED;
    }

    /**
     * Stops this stream.
     */
    public void stop ()
    {
        if (_state == AL10.AL_STOPPED) {
            log.warning("Tried to stop stream that was already stopped.");
            return;
        }
        _source.stop();
        _state = AL10.AL_STOPPED;
    }

    /**
     * Fades this stream in over the specified interval.  If the stream isn't playing, it will be
     * started.
     */
    public void fadeIn (float interval)
    {
        if (_state != AL10.AL_PLAYING) {
            play();
        }
        _source.setGain(0f);
        _fadeMode = FadeMode.IN;
        _fadeInterval = interval;
        _fadeElapsed = 0f;
    }

    /**
     * Fades this stream out over the specified interval.
     *
     * @param dispose if true, dispose of the stream when done fading out
     */
    public void fadeOut (float interval, boolean dispose)
    {
        _fadeMode = dispose ? FadeMode.OUT_DISPOSE : FadeMode.OUT;
        _fadeInterval = interval;
        _fadeElapsed = 0f;
    }

    /**
     * Releases the resources held by this stream and removes it from the manager.
     */
    public void dispose ()
    {
        // make sure the stream is stopped
        if (_state != AL10.AL_STOPPED) {
            stop();
        }

        // delete the source and buffers
        _source.delete();
        for (Buffer buffer : _buffers) {
            buffer.delete();
        }

        // remove from manager
        _soundmgr.removeStream(this);
    }

    /**
     * Updates the state of this stream, loading data into buffers and adjusting gain as necessary.
     * Called periodically by the {@link SoundManager}.
     *
     * @param time the amount of time elapsed since the last update
     */
    protected void update (float time)
    {
        // update fade, which may stop playing
        updateFade(time);
        if (_state != AL10.AL_PLAYING) {
            return;
        }

        // find out how many buffers have been played and unqueue them
        int played = _source.getBuffersProcessed();
        if (played == 0) {
            return;
        }
        for (int ii = 0; ii < played; ii++) {
            _source.unqueueBuffers(_buffers[_qidx]);
            _qidx = (_qidx + 1) % _buffers.length;
            _qlen--;
        }

        // enqueue up to the number of buffers played
        queueBuffers(played);

        // find out if we're still playing; if not and we have buffers queued, we must restart
        _state = _source.getSourceState();
        if (_qlen > 0 && _state != AL10.AL_PLAYING) {
            play();
        }
    }

    /**
     * Updates the gain of the stream according to the fade state.
     */
    protected void updateFade (float time)
    {
        if (_fadeMode == FadeMode.NONE) {
            return;
        }
        float alpha = Math.min((_fadeElapsed += time) / _fadeInterval, 1f);
        _source.setGain(_gain * (_fadeMode == FadeMode.IN ? alpha : (1f - alpha)));
        if (alpha == 1f) {
            if (_fadeMode == FadeMode.OUT) {
                stop();
            } else if (_fadeMode == FadeMode.OUT_DISPOSE) {
                dispose();
            }
            _fadeMode = FadeMode.NONE;
        }
    }

    /**
     * Queues (up to) the specified number of buffers.
     */
    protected void queueBuffers (int buffers)
    {
        for (int ii = 0; ii < buffers; ii++) {
            Buffer buffer = _buffers[(_qidx + _qlen) % _buffers.length];
            if (populateBuffer(buffer)) {
                _source.queueBuffers(buffer);
                _qlen++;
            } else {
                break;
            }
        }
    }

    /**
     * Populates the identified buffer with as much data as it can hold.
     *
     * @return true if data was read into the buffer and it should be enqueued, false if the end of
     * the stream has been reached and no data was read into the buffer
     */
    protected boolean populateBuffer (Buffer buffer)
    {
        if (_abuf == null) {
            _abuf = ByteBuffer.allocateDirect(getBufferSize()).order(ByteOrder.nativeOrder());
        }
        _abuf.clear();
        int read = 0;
        try {
            read = Math.max(populateBuffer(_abuf), 0);
        } catch (IOException e) {
            log.warning("Error reading audio stream [error=" + e + "].");
        }
        if (read <= 0) {
            return false;
        }
        _abuf.rewind().limit(read);
        buffer.setData(getFormat(), _abuf, getFrequency());
        return true;
    }

    /**
     * Returns the OpenAL audio format of the stream.
     */
    protected abstract int getFormat ();

    /**
     * Returns the stream's playback frequency in samples per second.
     */
    protected abstract int getFrequency ();

    /**
     * Populates the given buffer with audio data.
     *
     * @return the total number of bytes read into the buffer, or -1 if the end of the stream has
     * been reached
     */
    protected abstract int populateBuffer (ByteBuffer buf)
        throws IOException;

    /**
     * Returns the size in bytes of the buffers to use.
     */
    protected int getBufferSize ()
    {
        return 131072;
    }

    /**
     * Returns the number of buffers to use.
     */
    protected int getNumBuffers ()
    {
        return 4;
    }

    /** The manager to which the stream was added. */
    protected SoundManager _soundmgr;

    /** The source through which the stream plays. */
    protected Source _source;

    /** The buffers through which we cycle. */
    protected Buffer[] _buffers = new Buffer[getNumBuffers()];

    /** The starting index and length of the current queue in {@link #_buffers}. */
    protected int _qidx, _qlen;

    /** The gain of the stream. */
    protected float _gain = 1f;

    /** The interval and elapsed time for fading. */
    protected float _fadeInterval, _fadeElapsed;

    /** The type of fading being performed. */
    protected FadeMode _fadeMode = FadeMode.NONE;

    /** The buffer used to store names. */
    protected IntBuffer _nbuf;

    /** The buffer used to store audio data temporarily. */
    protected ByteBuffer _abuf;

    /** The OpenAL state of the stream. */
    protected int _state = AL10.AL_INITIAL;

    /** Fading modes. */
    protected enum FadeMode { NONE, IN, OUT, OUT_DISPOSE }
}
