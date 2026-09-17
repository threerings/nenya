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

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALC11;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.EXTDisconnect;
import org.lwjgl.openal.SOFTReopenDevice;
import org.lwjgl.openal.SOFTSystemEventProc;
import org.lwjgl.openal.SOFTSystemEvents;

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
     * Initialization properties for the sound manager.
     *
     * @param frequency TODO
     * @param refresh TODO
     * @param sync TODO
     */
    public static record InitArgs (int frequency, int refresh, boolean sync) {}

    // Match the pre-LWJGL3 AL.create("", 44100, 15, false).
    public static InitArgs getLegacyInitArgs ()
    {
        return new InitArgs(44100, 15, false);
    }

    /**
     * Get the "default" initialization args.
     */
    public static InitArgs getDefaultInitArgs ()
    {
        return new InitArgs(44100, 30, false);
    }

    // To move to a modern 48 kHz mixer, use 48000 / 60 here. (OpenAL picks driver
    // defaults (often 48 kHz / 50 Hz), resamples our 44.1 kHz assets, and audibly degrades quality.
    public static InitArgs getModernInitArgs ()
    {
        return new InitArgs(48000, 60, false);
    }

    /**
     * Creates, initializes and returns the singleton sound manager instance with default
     * parameters.
     *
     * @param rqueue a queue that the sound manager can use to post short runnables that must be
     * executed on the same thread from which all other sound methods will be called.
     */
    public static SoundManager createSoundManager (RunQueue rqueue)
    {
        return createSoundManager(rqueue, getDefaultInitArgs());
    }

    /**
     * Creates, initializes and returns the singleton sound manager instance.
     *
     * @param rqueue a queue that the sound manager can use to post short runnables that must be
     * executed on the same thread from which all other sound methods will be called.
     *
     * @param args init arg for creating the openAL context, or null to not pass init args.
     */
    public static SoundManager createSoundManager (RunQueue rqueue, InitArgs args)
    {
        if (_soundmgr != null) {
            throw new IllegalStateException("A sound manager has already been created.");
        }
        _soundmgr = new SoundManager(rqueue, args);
        return _soundmgr;
    }

    /**
     * Shuts down the sound manager.
     */
    public void shutdown ()
    {
        unregisterDeviceEvents();
        if (_alcContext != 0L) {
            ALC10.alcDestroyContext(_alcContext);
            _alcContext = 0L;
        }
        if (_alcDevice != 0L) {
            ALC10.alcCloseDevice(_alcDevice);
            _alcDevice = 0L;
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
    protected SoundManager (RunQueue rqueue) // provided for backwards compatibility..?
    {
        this(rqueue, null);
    }

    protected SoundManager (RunQueue rqueue, InitArgs args)
    {
        _rqueue = rqueue;

        // initialize the OpenAL sound system
        try {
            _alcDevice = ALC10.alcOpenDevice((ByteBuffer)null);
            if (_alcDevice == 0) {
                log.warning("Failed to create AL device.");
                // don't start the background loading thread
                return;
            }
            ALCCapabilities deviceCaps = ALC.createCapabilities(_alcDevice);
            _alcCaps = deviceCaps;

            // keep the attribute list: alcReopenDeviceSOFT takes the same list as
            // alcCreateContext, and we want the moved device configured identically
            _alcAttribs = createContextAttribs(args);
            _alcContext = ALC10.alcCreateContext(_alcDevice, _alcAttribs);
            ALC10.alcMakeContextCurrent(_alcContext);
            AL.createCapabilities(deviceCaps);

            registerDeviceEvents(deviceCaps);

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
     * Builds the attribute list handed to {@code alcCreateContext} (and later to
     * {@code alcReopenDeviceSOFT}).
     *
     * @param args the configuration to encode, or null for the library defaults.
     * @return a direct, zero-terminated buffer positioned at zero, or null if {@code args} is
     * null. LWJGL only accepts direct NIO buffers for native calls, hence {@link BufferUtils}.
     */
    protected static IntBuffer createContextAttribs (InitArgs args)
    {
        if (args == null) {
            return null;
        }
        IntBuffer attribs = BufferUtils.createIntBuffer(7);
        attribs.put(ALC10.ALC_FREQUENCY).put(args.frequency());
        attribs.put(ALC10.ALC_REFRESH).put(args.refresh());
        attribs.put(ALC10.ALC_SYNC).put(args.sync() ? ALC10.ALC_TRUE : ALC10.ALC_FALSE);
        attribs.put(0).flip();
        return attribs;
    }

    /**
     * Asks OpenAL to tell us when the system's default playback device changes or a playback
     * device is unplugged, so that {@link #reopenDevice} can move our output to the new default.
     *
     * <p>Why this is needed: {@code alcOpenDevice(NULL)} binds the device handle to whatever
     * endpoint was the default <em>at that moment</em> and OpenAL Soft never moves it on its
     * own. Without this, a player who switches Windows from speakers to a headset mid-session
     * keeps hearing the game on the speakers (or hears nothing at all if the old endpoint went
     * away). Both pieces we rely on are OpenAL Soft extensions: {@code ALC_SOFT_system_events}
     * (the notification) and {@code ALC_SOFT_reopen_device} (the move). Each is checked on the
     * device's capabilities; when either is missing we log once and keep the old behaviour.
     *
     * <p>What the JVM side allows, and why the callback is shaped the way it is:
     * <ul>
     * <li> The callback fires on a thread owned by the audio system (WASAPI's MMDevice
     * notification thread on Windows, a CoreAudio thread on macOS), not on any thread this
     * class knows about. LWJGL attaches that thread to the JVM for the duration of the call
     * and detaches it afterwards, so it is a valid Java thread while inside the callback but
     * has no OpenAL context, no AWT/Swing ownership and none of our locks.
     * <li> The extension spec forbids AL/ALC calls from inside the callback ("AL and ALC
     * functions may not be called in the callback") and OpenAL Soft's author has confirmed that
     * calling {@code alcReopenDeviceSOFT} there deadlocks against the notification lock. So the
     * callback does two thread-safe things only: it sets an {@link AtomicBoolean} and posts a
     * runnable to {@link #_rqueue}, which is where every other OpenAL call this class makes
     * already happens.
     * <li> Windows reports one default change as several events (one per device role), and a
     * hotplug can produce a removal and a default change together. The flag coalesces the burst
     * into a single reopen.
     * <li> {@link SOFTSystemEventProc} is an LWJGL {@code Callback}: it owns a native trampoline
     * that is not reclaimed by the garbage collector. We hold the only reference in
     * {@link #_eventProc} and release it in {@link #unregisterDeviceEvents}, after first
     * clearing the registration so a late event cannot land on freed memory.
     * <li> OpenAL Soft keeps a single process-wide event callback. This class is already a
     * singleton ({@link #createSoundManager}), so nothing else in the process competes for it.
     * </ul>
     *
     * @param caps the capabilities of the device we just opened; used to check for the two
     * extensions.
     */
    protected void registerDeviceEvents (ALCCapabilities caps)
    {
        if (!caps.ALC_SOFT_reopen_device) {
            log.info("OpenAL lacks ALC_SOFT_reopen_device; output will not follow the " +
                     "default device.");
            return;
        }
        if (!caps.ALC_SOFT_system_events) {
            log.info("OpenAL lacks ALC_SOFT_system_events; output will not follow the " +
                     "default device.");
            return;
        }

        _eventProc = SOFTSystemEventProc.create(
            (eventType, deviceType, device, length, message, userParam) -> {
                // foreign thread: no OpenAL calls, no blocking, no logging; see the JavaDoc
                if (deviceType != SOFTSystemEvents.ALC_PLAYBACK_DEVICE_SOFT) {
                    return;
                }
                if (_reopenPending.compareAndSet(false, true)) {
                    _rqueue.postRunnable(this::reopenDevice);
                }
            });
        SOFTSystemEvents.alcEventCallbackSOFT(_eventProc, 0L);

        IntBuffer events = BufferUtils.createIntBuffer(2);
        events.put(SOFTSystemEvents.ALC_EVENT_TYPE_DEFAULT_DEVICE_CHANGED_SOFT);
        events.put(SOFTSystemEvents.ALC_EVENT_TYPE_DEVICE_REMOVED_SOFT).flip();
        if (!SOFTSystemEvents.alcEventControlSOFT(events, true)) {
            log.warning("OpenAL refused device event registration; output will not follow " +
                        "the default device [error=" + ALC10.alcGetError(_alcDevice) + "].");
            unregisterDeviceEvents();
            return;
        }
        log.info("Following the default audio device [device=" + describeDevice() + "].");
    }

    /**
     * Undoes {@link #registerDeviceEvents}. Safe to call when nothing was registered. The order
     * matters: disable the events, clear the callback, then free the trampoline, so that a
     * notification racing with shutdown finds either a live callback or none, never a freed one.
     */
    protected void unregisterDeviceEvents ()
    {
        if (_eventProc == null) {
            return;
        }
        IntBuffer events = BufferUtils.createIntBuffer(2);
        events.put(SOFTSystemEvents.ALC_EVENT_TYPE_DEFAULT_DEVICE_CHANGED_SOFT);
        events.put(SOFTSystemEvents.ALC_EVENT_TYPE_DEVICE_REMOVED_SOFT).flip();
        SOFTSystemEvents.alcEventControlSOFT(events, false);
        SOFTSystemEvents.nalcEventCallbackSOFT(0L, 0L);
        _eventProc.free();
        _eventProc = null;
    }

    /**
     * Moves our output to the current default playback device. Runs on the {@link #_rqueue}
     * thread in response to a device event; never call it from the event callback itself.
     *
     * <p>{@code alcReopenDeviceSOFT} keeps the context, sources, buffers and streams intact and
     * simply re-associates the device handle with a new endpoint, so playing sounds and the
     * music stream carry on with at most a short gap. On failure the library leaves the device
     * on its previous endpoint and we log; the next event will try again.
     */
    protected void reopenDevice ()
    {
        _reopenPending.set(false);
        if (_alcDevice == 0L) {
            return;
        }
        String before = describeDevice();
        // the ByteBuffer overload is the one that accepts NULL, meaning "the default device"
        if (!SOFTReopenDevice.alcReopenDeviceSOFT(_alcDevice, (ByteBuffer)null, _alcAttribs)) {
            log.warning("Failed to move audio to the default device [was=" + before +
                        ", error=" + ALC10.alcGetError(_alcDevice) + "].");
            return;
        }
        log.info("Moved audio to the default device [was=" + before +
                 ", now=" + describeDevice() + "].");
    }

    /**
     * Returns a short description of the endpoint our device is currently bound to, for
     * logging. Reports the OpenAL device name and whether the endpoint is still connected.
     */
    protected String describeDevice ()
    {
        String name = ALC10.alcGetString(_alcDevice, ALC11.ALC_ALL_DEVICES_SPECIFIER);
        if (!_alcCaps.ALC_EXT_disconnect) {
            return name;
        }
        boolean connected =
            ALC10.alcGetInteger(_alcDevice, EXTDisconnect.ALC_CONNECTED) == ALC10.ALC_TRUE;
        return name + (connected ? "" : " (disconnected)");
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

    protected long _alcDevice;
    protected long _alcContext;

    /** The capabilities of {@link #_alcDevice}, kept for extension checks after startup. */
    protected ALCCapabilities _alcCaps;

    /** The attribute list our context was created with, reused when the device is reopened. */
    protected IntBuffer _alcAttribs;

    /** Receives device notifications from OpenAL; null when following is unavailable. */
    protected SOFTSystemEventProc _eventProc;

    /** Set by the event callback, cleared by {@link #reopenDevice}; folds bursts into one move. */
    protected final AtomicBoolean _reopenPending = new AtomicBoolean();

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
