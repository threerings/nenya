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

package com.threerings.media.sound;

import java.util.HashMap;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.google.common.collect.Maps;

import com.samskivert.io.StreamUtil;
import com.samskivert.swing.RuntimeAdjust;
import com.samskivert.util.LRUHashMap;
import com.samskivert.util.Queue;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.RunQueue;
import com.samskivert.util.StringUtil;

import com.threerings.media.MediaPrefs;
import com.threerings.resource.ResourceManager;

import static com.threerings.media.Log.log;

/**
 * Manages the playing of audio files via the Java Sound APIs.
 */
public class JavaSoundPlayer extends SoundPlayer
{
    /** The default clip cache holds 4 megs. */
    public static final int DEFAULT_CACHE_SIZE = 4 * 1024 * 1024;

    /**
     * Constructs a sound manager.
     */
    public JavaSoundPlayer (ResourceManager rmgr)
    {
        this(rmgr, null, null);
    }

    /**
     * Constructs a sound manager with the default clip cache size.
     *
     * @param defaultClipPath The pathname of a sound clip to use as a
     * fallback if another sound clip cannot be located.
     */
    public JavaSoundPlayer (ResourceManager rmgr, String defaultClipBundle, String defaultClipPath)
    {
        this(rmgr, defaultClipBundle, defaultClipPath, DEFAULT_CACHE_SIZE);
    }

    /**
     * Constructs a sound manager.
     *
     * @param defaultClipPath The pathname of a sound clip to use as a fallback if another sound
     * clip cannot be located.
     * @param cacheSize the number of bytes of sound clips to cache.
     */
    public JavaSoundPlayer (ResourceManager rmgr, String defaultClipBundle, String defaultClipPath,
            int cacheSize)
    {
        this(new SoundLoader(rmgr, defaultClipBundle, defaultClipPath), cacheSize);
    }

    public JavaSoundPlayer (SoundLoader loader, int cacheSize)
    {
        // save things off
        _loader = loader;
        _clipCache = new LRUHashMap<SoundKey, byte[][]>(cacheSize,
            new LRUHashMap.ItemSizer<byte[][]>() {
                public int computeSize (byte[][] value) {
                    int total = 0;
                    for (byte[] bs : value) {
                        total += bs.length;
                    }
                    return total;
                }
            });
    }

    @Override
    public void shutdown ()
    {
        // TODO: we need to stop any looping sounds
        synchronized (_queue) {
            _queue.clear();
            if (_spoolerCount > 0) {
                _queue.append(new SoundKey(DIE)); // signal death
            }
        }
        synchronized (_clipCache) {
            _lockedClips.clear();
            _loader.shutdown();
        }
    }

    /**
     * Sets the run queue on which sound should be played.
     */
    public void setSoundQueue (RunQueue queue)
    {
        _callbackQueue = queue;
    }

    @Override
    public RunQueue getSoundQueue ()
    {
        return _callbackQueue;
    }

    @Override
    public void lock (String pkgPath, String... keys)
    {
        for (int ii=0; ii < keys.length; ii++) {
            enqueue(new SoundKey(LOCK, pkgPath, keys[ii]), (ii == 0));
        }
    }

    @Override
    public void unlock (String pkgPath, String... keys)
    {
        for (int ii = 0; ii < keys.length; ii++) {
            enqueue(new SoundKey(UNLOCK, pkgPath, keys[ii]), (ii == 0));
        }
    }

    // ==== End of public methods ====

    @Override
    protected void play (String pkgPath, String key, float pan)
    {
        addToPlayQueue(new SoundKey(PLAY, pkgPath, key, 0, _clipVol, pan));
    }

    @Override
    protected Frob loop (String pkgPath, String key, float pan)
    {
        return loop(pkgPath, key, pan, LOOP);

    }

    /**
     * Loop the specified sound.
     */
    protected Frob loop (String pkgPath, String key, float pan, byte cmd)
    {
        SoundKey skey = new SoundKey(cmd, pkgPath, key, 0, _clipVol, pan);
        addToPlayQueue(skey);
        return skey; // it is a frob
    }

    /**
     * Add the sound clip key to the queue to be played.
     */
    protected void addToPlayQueue (SoundKey skey)
    {
        boolean queued = enqueue(skey, true);
        if (queued) {
            if (_verbose.getValue()) {
                log.info("Sound request [key=" + skey.key + "].");
            }

        } else /* if (_verbose.getValue()) */ {
            log.warning("SoundManager not playing sound because too many sounds in queue " +
                        "[key=" + skey + "].");
        }
    }

    /**
     * Enqueue a new SoundKey.
     */
    protected boolean enqueue (SoundKey key, boolean okToStartNew)
    {
        boolean add;
        boolean queued;
        synchronized (_queue) {
            if (key.cmd == PLAY && _queue.size() > MAX_QUEUE_SIZE) {
                queued = add = false;
            } else {
                _queue.appendLoud(key);
                queued = true;
                add = okToStartNew && (_freeSpoolers == 0) && (_spoolerCount < MAX_SPOOLERS);
                if (add) {
                    _spoolerCount++;
                }
            }
        }

        // and if we need a new thread, add it
        if (add) {
            Thread spooler = new Thread("narya SoundManager line spooler") {
                @Override
                public void run () {
                    spoolerRun();
                }
            };
            spooler.setDaemon(true);
            spooler.start();
        }

        return queued;
    }

    /**
     * This is the primary run method of the sound-playing threads.
     */
    protected void spoolerRun ()
    {
        while (true) {
            try {
                SoundKey key;
                synchronized (_queue) {
                    _freeSpoolers++;
                    key = _queue.get(MAX_WAIT_TIME);
                    _freeSpoolers--;

                    if (key == null || key.cmd == DIE) {
                        _spoolerCount--;
                        // if dying and there are others to kill, do so
                        if (key != null && _spoolerCount > 0) {
                            _queue.appendLoud(key);
                        }
                        return;
                    }
                }

                // process the command
                processKey(key);

            } catch (Exception e) {
                log.warning(e);
            }
        }
    }

    /**
     * Process the requested command in the specified SoundKey.
     */
    protected void processKey (SoundKey key)
        throws Exception
    {
        switch (key.cmd) {
        case PLAY:
        case LOOP:
            playSound(key);
            break;

        case LOCK:
            if (!isTesting()) {
                synchronized (_clipCache) {
                    try {
                        getClipData(key); // preload
                        // copy cached to lock map
                        _lockedClips.put(key, _clipCache.get(key));
                    } catch (Exception e) {
                        // don't whine about LOCK failures unless we are verbosely logging
                        if (_verbose.getValue()) {
                            throw e;
                        }
                    }
                }
            }
            break;

        case UNLOCK:
            synchronized (_clipCache) {
                _lockedClips.remove(key);
            }
            break;
        }
    }

    /**
     * Sets up an audio stream from the given byte array, and gets it to convert itself to PCM
     * data for writing to our output line (if it isn't already that)
     */
    public static AudioInputStream setupAudioStream (byte[] data)
        throws UnsupportedAudioFileException, IOException
    {
        return setupAudioStream(new ByteArrayInputStream(data));
    }

    /**
     * Sets up an audio stream from the given byte array, and gets it to convert itself to PCM
     * data for writing to our output line (if it isn't already that)
     */
    public static AudioInputStream setupAudioStream (InputStream in)
        throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream stream = AudioSystem.getAudioInputStream(in);
        AudioFormat format = stream.getFormat();
        if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
            stream = AudioSystem.getAudioInputStream(
                new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                format.getSampleRate(),
                                16,
                                format.getChannels(),
                                format.getChannels() * 2,
                                format.getSampleRate(),
                                false), stream);
        }

        return stream;
    }

    /**
     * On a spooling thread,
     */
    protected void playSound (SoundKey key)
    {
        if (!key.running) {
            return;
        }
        key.thread = Thread.currentThread();
        SourceDataLine line = null;
        try {
            // get the sound data from our LRU cache
            byte[] data = getClipData(key);
            if (data == null) {
                return; // borked!

            } else if (key.isExpired()) {
                if (_verbose.getValue()) {
                    log.info("Sound expired [key=" + key.key + "].");
                }
                return;

            }

            AudioInputStream stream = setupAudioStream(data);

            if (key.isLoop() && stream.markSupported()) {
                stream.mark(data.length);
            }

            // open the sound line
            AudioFormat format = stream.getFormat();
            line = (SourceDataLine)AudioSystem.getLine(
                new DataLine.Info(SourceDataLine.class, format));
            line.open(format, LINEBUF_SIZE);
            float setVolume = 1;
            float setPan = PAN_CENTER;
            line.start();

            _soundSeemsToWork = true;
            long startTime = System.currentTimeMillis();

            byte[] buffer = new byte[LINEBUF_SIZE];
            int totalRead = 0;
            do {
                // play the sound
                int count = 0;
                while (key.running && count != -1) {
                    float vol = key.volume;
                    if (vol != setVolume) {
                        adjustVolume(line, vol);
                        setVolume = vol;
                    }
                    float pan = key.pan;
                    if (pan != setPan) {
                        adjustPan(line, pan);
                        setPan = pan;
                    }
                    try {
                        count = stream.read(buffer, 0, buffer.length);
                        totalRead += count; // The final -1 will make us slightly off, but that's ok

                    } catch (IOException e) {
                        // this shouldn't ever ever happen because the stream
                        // we're given is from a reliable source
                        log.warning("Error reading clip data!", e);
                        return;
                    }

                    if (count >= 0) {
                        line.write(buffer, 0, count);
                    }
                }

                if (key.isLoop()) {
                    // if we're going to loop, reset the stream to the beginning if we can,
                    // otherwise just remake the stream
                    if (stream.markSupported()) {
                        stream.reset();
                    } else {
                        stream = setupAudioStream(data);
                    }
                }
            } while (key.isLoop() && key.running);

            // sleep the drain time. We never trust line.drain() because
            // it is buggy and locks up on natively multithreaded systems
            // (linux, winXP with HT).
            float sampleRate = format.getSampleRate();
            if (sampleRate == AudioSystem.NOT_SPECIFIED) {
                sampleRate = 11025; // most of our sounds are
            }
            int sampleSize = format.getSampleSizeInBits();
            if (sampleSize == AudioSystem.NOT_SPECIFIED) {
                sampleSize = 16;
            }

            // Calculate the numerator as a long as a decent sized clip * 8000 can overflow an int
            int drainTime = (int) Math.ceil((totalRead * 8 * 1000L) / (sampleRate * sampleSize));

            // subtract out time we've already spent doing things.
            drainTime -= System.currentTimeMillis() - startTime;

            drainTime = Math.max(0, drainTime);

            // add in a fudge factor of half a second
            drainTime += 500;

            try {
                Thread.sleep(drainTime);
            } catch (InterruptedException ie) { }

        } catch (IOException ioe) {
            log.warning("Error loading sound file [key=" + key + ", e=" + ioe + "].");

        } catch (UnsupportedAudioFileException uafe) {
            log.warning("Unsupported sound format [key=" + key + ", e=" + uafe + "].");

        } catch (LineUnavailableException lue) {
            String err = "Line not available to play sound [key=" + key.key + ", e=" + lue + "].";
            if (_soundSeemsToWork) {
                log.warning(err);
            } else {
                // this error comes every goddamned time we play a sound on someone with a
                // misconfigured sound card, so let's just keep it to ourselves
                log.debug(err);
            }

        } finally {
            if (line != null) {
                line.close();
            }
            key.thread = null;
        }
    }

    /**
     * @return true if we're using a test sound directory.
     */
    protected boolean isTesting ()
    {
        return !StringUtil.isBlank(_testDir.getValue());
    }

    /**
     * Called by spooling threads, loads clip data from the resource manager or the cache.
     */
    protected byte[] getClipData (SoundKey key)
        throws IOException, UnsupportedAudioFileException
    {
        byte[][] data;
        synchronized (_clipCache) {
            // if we're testing, clear all non-locked sounds every time
            if (isTesting()) {
                _clipCache.clear();
            }

            data = _clipCache.get(key);

            // see if it's in the locked cache (we first look in the regular
            // clip cache so that locked clips that are still cached continue
            // to be moved to the head of the LRU queue)
            if (data == null) {
                data = _lockedClips.get(key);
            }

            if (data == null) {
                // if there is a test sound, JUST use the test sound.
                InputStream stream = getTestClip(key);
                if (stream != null) {
                    data = new byte[1][];
                    data[0] = StreamUtil.toByteArray(stream);

                } else {
                    data = _loader.load(key.pkgPath, key.key);
                }

                _clipCache.put(key, data);
            }
        }

        return (data.length > 0) ? data[RandomUtil.getInt(data.length)] : null;
    }

    protected InputStream getTestClip (SoundKey key)
    {
        String testDirectory = _testDir.getValue();
        if (StringUtil.isBlank(testDirectory)) {
            return null;
        }

        final String namePrefix = key.key;
        File f = new File(testDirectory);
        File[] list = f.listFiles(new FilenameFilter() {
            public boolean accept (File f, String name)
            {
                if (name.startsWith(namePrefix)) {
                    String backhalf = name.substring(namePrefix.length());
                    int dot = backhalf.indexOf('.');
                    if (dot == -1) {
                        dot = backhalf.length();
                    }

                    // allow the file if the portion of the name
                    // after the prefix but before the extension is blank
                    // or a parsable integer
                    String extra = backhalf.substring(0, dot);
                    if ("".equals(extra)) {
                        return true;
                    } else {
                        try {
                            Integer.parseInt(extra);
                            // success!
                            return true;
                        } catch (NumberFormatException nfe) {
                            // not a number, we fall through...
                        }
                    }
                    // else fall through
                }
                return false;
            }
        });
        if (list == null) {
            return null;
        }
        if (list.length > 0) {
            File pick = list[RandomUtil.getInt(list.length)];
            try {
                return new FileInputStream(pick);
            } catch (Exception e) {
                log.warning("Error reading test sound [e=" + e + ", file=" + pick + "].");
            }
        }
        return null;
    }

    /**
     * Use the gain control to implement volume.
     */
    protected static void adjustVolume (Line line, float vol)
    {
        FloatControl control = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);

        // the only problem is that gain is specified in decibals, which is a logarithmic scale.
        // Since we want max volume to leave the sample unchanged, our
        // maximum volume translates into a 0db gain.
        float gain;
        if (vol == 0f) {
            gain = control.getMinimum();
        } else {
            gain = (float) ((Math.log(vol) / Math.log(10.0)) * 20.0);
        }

        control.setValue(gain);
        //Log.info("Set gain: " + gain);
    }

    /**
     * Set the pan value for the specified line.
     */
    protected static void adjustPan (Line line, float pan)
    {
        try {
            FloatControl control = (FloatControl) line.getControl(FloatControl.Type.PAN);
            control.setValue(pan);
        } catch (Exception e) {
            log.debug("Cannot set pan on line: " + e);
        }
    }

    /**
     * A key for tracking sounds.
     */
    protected static class SoundKey
        implements Frob
    {
        public byte cmd;

        public String pkgPath;

        public String key;

        public long stamp;

        /** Should we still be running? */
        public volatile boolean running = true;

        public volatile float volume;

        /** The pan, or 0 to center the sound. */
        public volatile float pan;

        /** The player thread, if it's playing us. */
        public Thread thread;

        /**
         * Create a SoundKey that just contains the specified command.
         */
        public SoundKey (byte cmd)
        {
            this.cmd = cmd;
        }

        /**
         * Quicky constructor for music keys and lock operations.
         */
        public SoundKey (byte cmd, String pkgPath, String key)
        {
            this(cmd);
            this.pkgPath = pkgPath;
            this.key = key;
        }

        /**
         * Constructor for a sound effect soundkey.
         */
        public SoundKey (byte cmd, String pkgPath, String key, int delay, float volume,
                float pan)
        {
            this(cmd, pkgPath, key);

            stamp = System.currentTimeMillis() + delay;
            setVolume(volume);
            setPan(pan);
        }

        // documentation inherited from interface Frob
        public void stop ()
        {
            running = false;
            Thread t = thread;
            if (t != null) {
                // doesn't actually ever seem to do much
                t.interrupt();
            }
        }

        // documentation inherited from interface Frob
        public void setVolume (float vol)
        {
            volume = Math.max(0f, Math.min(1f, vol));
        }

        // documentation inherited from interface Frob
        public float getVolume ()
        {
            return volume;
        }

        // documentation inherited from interface Frob
        public void setPan (float newPan)
        {
            pan = Math.max(PAN_LEFT, Math.min(PAN_RIGHT, newPan));
        }

        // documentation inherited from interface Frob
        public float getPan ()
        {
            return pan;
        }

        /**
         * Has this sound key expired.
         */
        public boolean isExpired ()
        {
            return (stamp + MAX_SOUND_DELAY < System.currentTimeMillis());
        }

        /**
         * If this key is one of the two loop types.
         */
        protected boolean isLoop() {
            return cmd == LOOP;
        }

        @Override
        public String toString ()
        {
            return "SoundKey{cmd=" + cmd + ", pkgPath=" + pkgPath + ", key=" + key + "}";
        }

        @Override
        public int hashCode ()
        {
            return pkgPath.hashCode() ^ key.hashCode();
        }

        @Override
        public boolean equals (Object o)
        {
            if (o instanceof SoundKey) {
                SoundKey that = (SoundKey) o;
                return this.pkgPath.equals(that.pkgPath) && this.key.equals(that.key);
            }
            return false;
        }
    }

    /** Does our package based sound loading. */
    protected SoundLoader _loader;

    /** The queue where callbacks for keys being processed are dispatched. */
    protected RunQueue _callbackQueue = RunQueue.AWT;

    /** The queue of sound clips to be played. */
    protected Queue<SoundKey> _queue = new Queue<SoundKey>();

    /** The number of currently active LineSpoolers. */
    protected int _spoolerCount, _freeSpoolers;

    /** If we every play a sound successfully, this is set to true. */
    protected boolean _soundSeemsToWork = false;

    /** The cache of recent audio clips . */
    protected LRUHashMap<SoundKey, byte[][]> _clipCache;

    /**
     * The set of locked audio clips; this is separate from the LRU so that locking clips doesn't
     * booch up an otherwise normal caching agenda.
     */
    protected HashMap<SoundKey,byte[][]> _lockedClips = Maps.newHashMap();

    /** Soundkey command constants. */
    protected static final byte PLAY = 0;
    protected static final byte LOCK = 1;
    protected static final byte UNLOCK = 2;
    protected static final byte DIE = 3;
    protected static final byte LOOP = 4;

    /** A pref that specifies a directory for us to get test sounds from. */
    protected static RuntimeAdjust.FileAdjust _testDir =
        new RuntimeAdjust.FileAdjust(
            "Test sound directory", "narya.media.sound.test_dir", MediaPrefs.config, true, "");

    protected static RuntimeAdjust.BooleanAdjust _verbose =
        new RuntimeAdjust.BooleanAdjust(
            "Verbose sound event logging", "narya.media.sound.verbose", MediaPrefs.config, false);

    /** The queue size at which we start to ignore requests to play sounds. */
    protected static final int MAX_QUEUE_SIZE = 25;

    /** The maximum time after which we throw away a sound rather than play it. */
    protected static final long MAX_SOUND_DELAY = 400L;

    /** The size of the line's buffer. */
    protected static final int LINEBUF_SIZE = 16 * 1024;

    /** The maximum time a spooler will wait for a stream before deciding to shut down. */
    protected static final long MAX_WAIT_TIME = 30000L;

    /** The maximum number of spoolers we'll allow. This is a lot. */
    protected static final int MAX_SPOOLERS = 12;
}
