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

import java.util.Map;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.nio.ByteBuffer;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.lwjgl.openal.AL10;
import org.lwjgl.util.WaveData;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import com.samskivert.util.BasicRunQueue;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.ResultListener;
import com.samskivert.util.RunQueue;

import com.threerings.media.FrameManager;
import com.threerings.media.sound.JavaSoundPlayer;
import com.threerings.media.sound.SoundLoader;
import com.threerings.media.sound.SoundPlayer;
import com.threerings.media.timer.MediaTimer;

import static com.threerings.media.Log.log;

/**
 * Implements the abstract pieces of {@link SoundPlayer} via OpenAL.
 */
public class OpenALSoundPlayer extends SoundPlayer
    implements ClipProvider
{

    public OpenALSoundPlayer (SoundLoader loader)
    {
        _loader = loader;
        try {
            _alSoundManager = createSoundManager();
            _group = _alSoundManager.createGroup(this, SOURCE_COUNT);
        } catch (Throwable t) {
            log.warning("Unable to initialize OpenAL", "cause", t);
        }
        _ticker.start();
    }

    public Clip loadClip (String path)
        throws IOException
    {
        int bundleEnd = path.lastIndexOf(":");
        InputStream sound = _loader.getSound(path.substring(0, bundleEnd),
            path.substring(bundleEnd + 1));
        if (path.endsWith(".ogg")) {
            try {
                AudioInputStream instream = JavaSoundPlayer.setupAudioStream(sound);
                ByteArrayOutputStream outstream = new ByteArrayOutputStream();

                byte[] buf = new byte[16 * 1024];
                int read;
                do {
                    read = instream.read(buf, 0, buf.length);
                    if (read >= 0) {
                        outstream.write(buf, 0, read);
                    }
                } while (read >= 0);

                byte[] audio = outstream.toByteArray();
                AudioFormat format = instream.getFormat();
                long length = audio.length / format.getFrameSize();

                instream = new AudioInputStream(new ByteArrayInputStream(audio), format, length);

                outstream = new ByteArrayOutputStream();

                AudioSystem.write(instream, AudioFileFormat.Type.WAVE, outstream);

                sound = new ByteArrayInputStream(outstream.toByteArray());

            } catch (Exception e) {
                log.warning("Error decompressing audio clip", "path", path, e);
                return new Clip();
            }
        }
        return new Clip(WaveData.create(sound));
    }

    /**
     * Returns the loader used by this player.
     */
    public SoundLoader getSoundLoader ()
    {
        return _loader;
    }

    @Override
    public RunQueue getSoundQueue ()
    {
        return _ticker;
    }

    @Override
    public void setClipVolume (final float vol)
    {
        super.setClipVolume(vol);
        getSoundQueue().postRunnable(new Runnable() {
            public void run () {
                _alSoundManager.setBaseGain(vol);
            }});
    }

    @Override
    public void lock (String pkgPath, String... keys)
    {
        for (String key : keys) {
            for (final String path : getPaths(pkgPath, key)) {
                getSoundQueue().postRunnable(new Runnable() {
                    public void run () {
                        if (_locked.containsKey(path)) {
                            return;
                        }
                        _alSoundManager.loadClip(OpenALSoundPlayer.this, path,
                            new ClipBuffer.Observer() {
                                public void clipFailed (ClipBuffer buffer) {
                                    log.warning("Unable to load sound", "path", path);
                                }

                                public void clipLoaded (ClipBuffer buffer) {
                                    _locked.put(path, buffer);
                                }
                            });
                    }
                });
            }
        }
    }

    @Override
    public void unlock (final String pkgPath, String... keys)
    {
        for (final String key : keys) {
            getSoundQueue().postRunnable(new Runnable() {
                public void run () {
                    for (String path : getPaths(pkgPath, key)) {
                        _locked.remove(path);
                    }
                }
            });
        }
    }

    /**
     * Streams ogg files from the given bundle and path.
     */
    public void stream (final String bundle, final String path, final boolean loop,
        final ResultListener<Stream> listener)
        throws IOException
    {
        if (!path.endsWith(".ogg")) {
            log.warning("Unknown file type for streaming", "bundle", bundle, "path", path);
            return;
        }
        InputStream rsrc = _loader.getSound(bundle, path);
        final StreamDecoder dec = new OggStreamDecoder();
        dec.init(rsrc);
        getSoundQueue().postRunnable(new Runnable() {
            public void run () {
                Stream s = new Stream(_alSoundManager) {
                    @Override
                    protected void update (float time) {
                        super.update(time);
                        if (_state != AL10.AL_PLAYING) {
                            return;
                        }
                        super.setGain(_clipVol * _streamGain);
                    }

                    @Override
                    public void setGain (float gain) {
                        _streamGain = gain;
                        super.setGain(_clipVol * _streamGain);
                    }

                    @Override
                    protected int getFormat () {
                        return dec.getFormat();
                    }

                    @Override
                    protected int getFrequency () {
                        return dec.getFrequency();
                    }

                    @Override
                    protected int populateBuffer (ByteBuffer buf) throws IOException {
                        int read = dec.read(buf);
                        if (buf.hasRemaining() && loop) {
                            dec.init(_loader.getSound(bundle, path));
                            read = Math.max(0, read);
                            read += dec.read(buf);
                        }
                        return read;
                    }

                    protected float _streamGain = 1F;
                };
                s.setGain(_clipVol);
                listener.requestCompleted(s);
            }});
    }

    @Override
    public Frob loop (String pkgPath, String key, float pan)
    {
        return loop(pkgPath, key, pan, 1f);
    }

    public Frob loop (String pkgPath, String key, float pan, float gain)
    {
        return loop(null, pkgPath, key, gain, null);
    }

    public Frob loop (SoundType type, String pkgPath, String key, final float gain,
        final float[] pos)
    {
        if (!shouldPlay(type)) {
            return null;
        }

        final SoundGrabber loader = new SoundGrabber(pkgPath, key) {
            @Override
            protected void soundLoaded () {
                sound.setGain(gain);
                if (pos != null) {
                    sound.setPosition(pos[0], pos[1], pos[2]);
                }
                sound.loop(true);
            }};
        getSoundQueue().postRunnable(loader);
        return new Frob() {
            public float getPan () {
                return 0;
            }

            public float getVolume () {
                return 0;
            }

            public void setPan (float pan) {}

            public void setVolume (float vol) {}

            public void stop () {
                getSoundQueue().postRunnable(new Runnable(){
                    public void run () {
                        if (loader.sound != null) {
                            loader.sound.stop();
                        }
                    }});
            }};
    }

    @Override
    public void play (String pkgPath, String key, float pan)
    {
        play(pkgPath, key, pan, 1f);
    }

    public void play (String pkgPath, String key, float pan, final float gain)
    {
        play(null, pkgPath, key, gain, null);
    }

    public boolean play (SoundType type, String pkgPath, String key, final float gain,
        final float[] pos)
    {
        if (!shouldPlay(type)) {
            return false;
        }

        getSoundQueue().postRunnable(new SoundGrabber(pkgPath, key) {
            @Override
            protected void soundLoaded () {
                sound.setGain(gain);
                if (pos != null) {
                    sound.setPosition(pos[0], pos[1], pos[2]);
                }
                sound.play(true);
            }
        });
        return true;
    }

    @Override
    public void shutdown ()
    {
        getSoundQueue().postRunnable(new Runnable() {
            public void run () {
                _group.dispose();
                _locked.clear();
                for (Stream stream : _alSoundManager.getStreams()) {
                    stream.dispose();
                }
            }
        });
    }

    /**
     * Returns bundle:path for all sounds under key in pkgPath.
     */
    protected String[] getPaths (String pkgPath, String key)
    {

        String bundle = _loader.getBundle(pkgPath);
        Preconditions.checkNotNull(bundle,
            "Unable to find the bundle name for a package [package=%s, key=%s]", pkgPath, key);
        String[] names = _loader.getPaths(pkgPath, key);
        Preconditions.checkNotNull(names, "No such sound [package=%s, key=%s]", pkgPath, key);
        String[] paths = new String[names.length];
        for (int ii = 0; ii < paths.length; ii++) {
            paths[ii] = bundle + ":" + names[ii];
        }
        return paths;
    }

    /**
     * Creates our SoundManager.
     */
    protected SoundManager createSoundManager ()
    {
        return new MediaALSoundManager();
    }

    /**
     * Extends sound manager to allow sounds to be pulled out of the locked map.
     */
    protected class MediaALSoundManager extends SoundManager {
        protected MediaALSoundManager () {
            super(getSoundQueue());
        }

        @Override
        protected ClipBuffer getClip (ClipProvider provider, String path) {
            if (_locked.containsKey(path)) {
                return _locked.get(path);
            }
            return super.getClip(provider, path, null);
        }
    }

    /**
     * Updates the sound manager's streams every STREAM_UPDATE_INTERVAL and processes sound
     * runnables added to its queue.
     */
    protected class TickingQueue extends BasicRunQueue
    {
        public TickingQueue () {
            super("SoundPlayerQueue");
        }

        @Override
        protected void iterate () {
            long elapsed = _timer.getElapsedMillis() - _lastTick;
            Runnable r;
            if (elapsed >= STREAM_UPDATE_INTERVAL) {
                r = _queue.getNonBlocking();
            } else {
                r = _queue.get(STREAM_UPDATE_INTERVAL - elapsed);
            }
            long newTime;
            if (_alSoundManager == null) {
                // We weren't able to initialize the sound system, and we logged it earlier, so
                // just empty the queue and update the tick time without running the code that
                // needs the sound manager.
                newTime = _timer.getElapsedMillis();
            } else {
                if (r != null) {
                    try {
                        r.run();

                    } catch (Throwable t) {
                        log.warning("Runnable posted to SoundPlayerQueue barfed.", t);
                    }
                }
                newTime = _timer.getElapsedMillis();
                try {
                    _alSoundManager.updateStreams((newTime - _lastTick) / 1000F);
                } catch (Throwable t) {
                    log.warning("Updating OpenAL streams barfed.", t);
                }
            }
            _lastTick = newTime;
        }

        protected MediaTimer _timer = FrameManager.createTimer();

        protected long _lastTick;
    }

    /**
     * Loads a sound in its run method and calls subclasses with soundLoaded to let them know it's
     * ready.
     */
    protected abstract class SoundGrabber
        implements Runnable
    {
        public String path;

        public Sound sound;

        public SoundGrabber (String pkgPath, String key) {
            String[] paths = getPaths(pkgPath, key);
            path = paths[RandomUtil.getInt(paths.length)];
        }

        public void run () {
            sound = _group.getSound(path);
            soundLoaded();
        }

        protected abstract void soundLoaded ();
    }

    /** Number of milliseconds to wait between stream updates. */
    protected static final int STREAM_UPDATE_INTERVAL = 100;

    protected TickingQueue _ticker = new TickingQueue();

    protected Map<String, ClipBuffer> _locked = Maps.newHashMap();

    protected SoundLoader _loader;
    protected SoundGroup _group;
    protected SoundManager _alSoundManager;

    /** Number of sounds that can be played simultaneously. */
    protected final int SOURCE_COUNT = 10;
}
