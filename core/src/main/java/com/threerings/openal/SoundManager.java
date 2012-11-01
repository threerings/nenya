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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.util.IntListUtil;
import com.samskivert.util.LRUHashMap;
import com.samskivert.util.Queue;
import com.samskivert.util.RunQueue;

import com.threerings.openal.ClipBuffer.Observer;

import static com.threerings.openal.Log.log;

/**
 * An interface to the OpenAL library that provides a number of additional services:
 *
 * <ul>
 * <li> an object oriented interface to the OpenAL system
 * <li> a mechanism for loading a group of sounds and freeing their resources all at once
 * <li> a mechanism for loading sounds in a background thread and preloading sounds that are likely
 * to be needed soon
 * </ul>
 *
 * <p><em>Note:</em> the sound manager is not thread safe (other than during its interactions with
 * its internal background loading thread). It assumes that all sound loading and play requests
 * will be made from a single thread.
 */
public class SoundManager
{
    /**
     * Creates, initializes and returns the singleton sound manager instance.
     *
     * @param rqueue a queue that the sound manager can use to post short runnables that must be
     * executed on the same thread from which all other sound methods will be called.
     */
    public static SoundManager createSoundManager (RunQueue rqueue)
    {
        if (_soundmgr != null) {
            throw new IllegalStateException("A sound manager has already been created.");
        }
        _soundmgr = new SoundManager(rqueue);
        return _soundmgr;
    }

    /**
     * Shuts down the sound manager.
     */
    public void shutdown ()
    {
        if (isInitialized()) {
            AL.destroy();
        }
    }

    /**
     * Returns true if we were able to initialize the sound system.
     */
    public boolean isInitialized ()
    {
        return (_toLoad != null);
    }

    /**
     * Configures the size of our sound cache. If this value is larger than memory available to the
     * underlying sound system, it will be reduced when OpenAL first tells us we're out of memory.
     */
    public void setCacheSize (int bytes)
    {
        _clips.setMaxSize(bytes);
    }

    /**
     * Returns a reference to the listener object.
     */
    public Listener getListener ()
    {
        return _listener;
    }

    /**
     * Configures the base gain (which must be a value between 0 and 1.0) which is multiplied to
     * the individual gain assigned to sound effects (but not music).
     */
    public void setBaseGain (float gain)
    {
        if (_baseGain == gain) {
            return;
        }
        _baseGain = gain;

        // alert the groups that inherite the gain
        for (int ii = 0, nn = _groups.size(); ii < nn; ii++) {
            SoundGroup group = _groups.get(ii);
            if (group.getBaseGain() < 0f) {
                group.baseGainChanged();
            }
        }
    }

    /**
     * Returns the base gain used for sound effects (not music).
     */
    public float getBaseGain ()
    {
        return _baseGain;
    }

    /**
     * Creates an object that can be used to manage and play a group of sounds. <em>Note:</em> the
     * sound group <em>must</em> be disposed when it is no longer needed via a call to {@link
     * SoundGroup#dispose}.
     *
     * @param provider indicates from where the sound group will load its sounds.
     * @param sources indicates the maximum number of simultaneous sounds that can play in this
     * group.
     */
    public SoundGroup createGroup (ClipProvider provider, int sources)
    {
        return new SoundGroup(this, provider, sources);
    }

    /**
     * Returns a reference to the list of active streams.
     */
    public ArrayList<Stream> getStreams ()
    {
        return _streams;
    }

    /**
     * Updates all of the streams controlled by the manager.  This should be called once per frame
     * by the application.
     *
     * @param time the number of seconds elapsed since the last update
     */
    public void updateStreams (float time)
    {
        // iterate backwards through the list so that streams can dispose of themselves during
        // their update
        for (int ii = _streams.size() - 1; ii >= 0; ii--) {
            _streams.get(ii).update(time);
        }

        // delete any finalized objects
        deleteFinalizedObjects();
    }

    /**
     * Loads a clip buffer for the sound clip loaded via the specified provider with the
     * specified path. The loaded clip is placed in the cache.
     */
    public void loadClip (ClipProvider provider, String path)
    {
        loadClip(provider, path, null);
    }

    /**
     * Loads a clip buffer for the sound clip loaded via the specified provider with the
     * specified path. The loaded clip is placed in the cache.
     */
    public void loadClip (ClipProvider provider, String path, Observer observer)
    {
        getClip(provider, path, observer);
    }

    /**
     * Creates a sound manager and initializes the OpenAL sound subsystem.
     */
    protected SoundManager (RunQueue rqueue)
    {
        _rqueue = rqueue;

        // initialize the OpenAL sound system
        try {
            AL.create("", 44100, 15, false);
        } catch (Exception e) {
            log.warning("Failed to initialize sound system.", e);
            // don't start the background loading thread
            return;
        }

        int errno = AL10.alGetError();
        if (errno != AL10.AL_NO_ERROR) {
            log.warning("Failed to initialize sound system [errno=" + errno + "].");
            // don't start the background loading thread
            return;
        }

        // configure our LRU map with a removal observer
        _clips.setRemovalObserver(new LRUHashMap.RemovalObserver<String, ClipBuffer>() {
            public void removedFromMap (LRUHashMap<String, ClipBuffer> map,
                                        final ClipBuffer item) {
                _rqueue.postRunnable(new Runnable() {
                    public void run () {
                        log.debug("Flushing " + item.getKey());
                        item.dispose();
                    }
                });
            }
        });

        // create our loading queue
        _toLoad = new Queue<ClipBuffer>();

        // start up the background loader thread
        _loader.setDaemon(true);
        _loader.start();
    }

    /**
     * Creates a clip buffer for the sound clip loaded via the specified provider with the
     * specified path. The clip buffer may come from the cache, and it will immediately be queued
     * for loading if it is not already loaded.
     */
    protected ClipBuffer getClip (ClipProvider provider, String path)
    {
        return getClip(provider, path, null);
    }

    /**
     * Creates a clip buffer for the sound clip loaded via the specified provider with the
     * specified path. The clip buffer may come from the cache, and it will immediately be queued
     * for loading if it is not already loaded.
     */
    protected ClipBuffer getClip (ClipProvider provider, String path, Observer observer)
    {
        String ckey = ClipBuffer.makeKey(provider, path);
        ClipBuffer buffer = _clips.get(ckey);
        try {
            if (buffer == null) {
                // check to see if this clip is currently loading
                buffer = _loading.get(ckey);
                if (buffer == null) {
                    buffer = new ClipBuffer(this, provider, path);
                    _loading.put(ckey, buffer);
                }
            }
            buffer.resolve(observer);
            return buffer;

        } catch (Throwable t) {
            log.warning("Failure resolving buffer [key=" + ckey + "].", t);
            return null;
        }
    }

    /**
     * Queues the supplied clip buffer up for resolution. The {@link Clip} will be loaded into
     * memory and then bound into OpenAL on the background thread.
     */
    protected void queueClipLoad (ClipBuffer buffer)
    {
        if (_toLoad != null) {
            _toLoad.append(buffer);
        }
    }

    /**
     * Queues the supplied clip buffer up using our {@link RunQueue} to notify its observers that
     * it failed to load.
     */
    protected void queueClipFailure (final ClipBuffer buffer)
    {
        _rqueue.postRunnable(new Runnable() {
            public void run () {
                _loading.remove(buffer.getKey());
                buffer.failed();
            }
        });
    }

    /**
     * Adds the supplied clip buffer back to the cache after it has been marked for disposal and
     * subsequently re-requested.
     */
    protected void restoreClip (ClipBuffer buffer)
    {
        _clips.put(buffer.getKey(), buffer);
    }

    /**
     * Adds a stream to the list maintained by the manager.  Called by streams when they are
     * created.
     */
    protected void addStream (Stream stream)
    {
        _streams.add(stream);
    }

    /**
     * Removes a stream from the list maintained by the manager.  Called by streams when they are
     * disposed.
     */
    protected void removeStream (Stream stream)
    {
        _streams.remove(stream);
    }

    /**
     * Adds a group to the list maintained by the manager.  Called by groups when they are created.
     */
    protected void addGroup (SoundGroup group)
    {
        _groups.add(group);
    }

    /**
     * Removes a group from the list maintained by the manager.  Called by groups when they are
     * disposed.
     */
    protected void removeGroup (SoundGroup group)
    {
        _groups.remove(group);
    }

    /**
     * Called when a source has been finalized.
     */
    protected synchronized void sourceFinalized (int id)
    {
        _finalizedSources = IntListUtil.add(_finalizedSources, id);
    }

    /**
     * Called when a buffer has been finalized.
     */
    protected synchronized void bufferFinalized (int id)
    {
        _finalizedBuffers = IntListUtil.add(_finalizedBuffers, id);
    }

    /**
     * Deletes all finalized objects.
     */
    protected synchronized void deleteFinalizedObjects ()
    {
        if (_finalizedSources != null) {
            IntBuffer idbuf = BufferUtils.createIntBuffer(_finalizedSources.length);
            idbuf.put(_finalizedSources).rewind();
            AL10.alDeleteSources(idbuf);
            _finalizedSources = null;
        }
        if (_finalizedBuffers != null) {
            IntBuffer idbuf = BufferUtils.createIntBuffer(_finalizedBuffers.length);
            idbuf.put(_finalizedBuffers).rewind();
            AL10.alDeleteBuffers(idbuf);
            _finalizedBuffers = null;
        }
    }

    /** The thread that loads up sound clips in the background. */
    protected Thread _loader = new Thread("SoundManager.Loader") {
        @Override
        public void run () {
            while (true) {
                final ClipBuffer buffer = _toLoad.get();
                try {
                    log.debug("Loading " + buffer.getKey() + ".");
                    final Clip clip = buffer.load();
                    _rqueue.postRunnable(new Runnable() {
                        public void run () {
                            String ckey = buffer.getKey();
                            log.debug("Loaded " + ckey + ".");
                            _loading.remove(ckey);
                            if (buffer.bind(clip)) {
                                _clips.put(ckey, buffer);
                            } else {
                                // TODO: shrink the cache size if the bind failed due to
                                // OUT_OF_MEMORY
                            }
                        }
                    });

                } catch (Throwable t) {
                    log.warning("Failed to load clip [key=" + buffer.getKey() + "].", t);

                    // let the clip and its observers know that we are a miserable failure
                    queueClipFailure(buffer);
                }
            }
        }
    };

    /** Used to get back from the background thread to our "main" thread. */
    protected RunQueue _rqueue;

    /** The listener object. */
    protected Listener _listener = new Listener();

    /** A base gain that is multiplied by the individual gain assigned to sounds. */
    protected float _baseGain = 1;

    /** Contains a mapping of all currently-loading clips. */
    protected HashMap<String, ClipBuffer> _loading = Maps.newHashMap();

    /** Contains a mapping of all loaded clips. */
    protected LRUHashMap<String, ClipBuffer> _clips =
        new LRUHashMap<String, ClipBuffer>(DEFAULT_CACHE_SIZE, _sizer);

    /** Contains a queue of clip buffers waiting to be loaded. */
    protected Queue<ClipBuffer> _toLoad;

    /** The list of active streams. */
    protected ArrayList<Stream> _streams = Lists.newArrayList();

    /** The list of active groups. */
    protected List<SoundGroup> _groups = Lists.newArrayList();

    /** The list of sources to be deleted. */
    protected int[] _finalizedSources;

    /** The list of buffers to be deleted. */
    protected int[] _finalizedBuffers;

    /** The one and only sound manager, here for an exclusive performance by special request.
     * Available for all your sound playing needs. */
    protected static SoundManager _soundmgr;

    /** Used to compute the in-memory size of sound samples. */
    protected static LRUHashMap.ItemSizer<ClipBuffer> _sizer =
        new LRUHashMap.ItemSizer<ClipBuffer>() {
        public int computeSize (ClipBuffer item) {
            return item.getSize();
        }
    };

    /** Default to a cache size of one megabyte. */
    protected static final int DEFAULT_CACHE_SIZE = 8 * 1024 * 1024;
}
