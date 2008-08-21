//
// $Id$

package com.threerings.openal;

import static com.threerings.media.Log.log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;

import org.lwjgl.openal.AL10;
import org.lwjgl.util.WaveData;

import com.google.common.collect.Maps;

import com.samskivert.util.BasicRunQueue;
import com.samskivert.util.RunQueue;

import com.threerings.media.FrameManager;
import com.threerings.media.sound.SoundLoader;
import com.threerings.media.sound.SoundPlayer;
import com.threerings.media.timer.MediaTimer;

/**
 * Implements the abstract pieces of {@link SoundPlayer} via OpenAL.
 */
public class OpenALSoundPlayer extends SoundPlayer
    implements ClipProvider
{

    public OpenALSoundPlayer (SoundLoader loader)
    {
        _loader = loader;
        _alSoundManager = new MediaALSoundManager();
        _group = _alSoundManager.createGroup(this, SOURCE_COUNT);
        _ticker.start();
    }

    public Clip loadClip (String path)
        throws IOException
    {
        int pkgEnd = path.lastIndexOf("/") + 1;
        byte[] data = _loader.load(path.substring(0, pkgEnd), path.substring(pkgEnd))[0];
        return new Clip(WaveData.create(new ByteArrayInputStream(data)));
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
        _ticker.postRunnable(new Runnable(){
            public void run () {
                _alSoundManager.setBaseGain(vol);
            }});
    }

    @Override
    public void lock (String pkgPath, String... keys)
    {
        for (String key : keys) {
            final String path = pkgPath + key;
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

    @Override
    public void unlock (final String pkgPath, String... keys)
    {
        for (final String key : keys) {
            getSoundQueue().postRunnable(new Runnable() {
                public void run () {
                    _locked.remove(pkgPath + key);
                }
            });
        }
    }

    /**
     * Streams ogg files from the given bundle and path.
     */
    public Stream stream (final String bundle, final String path, final boolean loop)
        throws IOException
    {
        if (!path.endsWith(".ogg")) {
            log.warning("Unknown file type for streaming", "bundle", bundle, "path", path);
            return null;
        }
        InputStream rsrc = _loader.getSound(bundle, path);
        final StreamDecoder dec = new OggStreamDecoder();
        dec.init(rsrc);
        return new Stream(_alSoundManager) {

            @Override
            protected void update (float time) {
                super.update(time);
                if (_state != AL10.AL_PLAYING) {
                    return;
                }
                setGain(_clipVol);
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
                if(buf.hasRemaining() && loop) {
                    dec.init(_loader.getSound(bundle, path));
                    read = Math.max(0, read);
                    read += dec.read(buf);
                }
                return read;
            }};
    }

    @Override
    protected Frob loop (String pkgPath, String key, float pan)
    {
        final SoundGrabber loader = new SoundGrabber(pkgPath, key) {
            @Override
            protected void soundLoaded () {
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
        getSoundQueue().postRunnable(new SoundGrabber(pkgPath, key) {
            @Override
            protected void soundLoaded () {
                sound.play(true);
            }});
    }

    @Override
    public void shutdown ()
    {
        _group.dispose();
        _locked.clear();
        for (Stream stream : _alSoundManager.getStreams()) {
            stream.dispose();
        }
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
        public TickingQueue ()
        {
            super("SoundPlayerQueue");
        }

        @Override
        protected void iterate ()
        {
            long elapsed = _timer.getElapsedMillis() - _lastTick;
            Runnable r;
            if (elapsed >= STREAM_UPDATE_INTERVAL) {
                r = _queue.getNonBlocking();
            } else {
                r = _queue.get(STREAM_UPDATE_INTERVAL - elapsed);
            }
            if (r != null) {
                try {
                    r.run();

                } catch (Throwable t) {
                    log.warning("Runnable posted to SoundPlayerQueue barfed.", t);
                }
            }
            long newTime = _timer.getElapsedMillis();
            _alSoundManager.updateStreams((newTime - _lastTick)/ 1000F);
            _lastTick = newTime;
        }

        protected MediaTimer _timer = FrameManager.createTimer();

        protected long _lastTick;
    };

    /**
     * Loads a sound in its run method and calls subclasses with soundLoaded to let them know it's
     * ready.
     */
    protected abstract class SoundGrabber
        implements Runnable
    {
        public String path;

        public Sound sound;

        public SoundGrabber (String pkgPath, String key)
        {
            path = pkgPath + key;
        }

        public void run ()
        {
            sound = _group.getSound(path);
            soundLoaded();
        }

        protected abstract void soundLoaded ();
    }

    /** Number of milliseconds to wait between stream updates. */
    protected static final int STREAM_UPDATE_INTERVAL = 100;

    protected TickingQueue _ticker = new TickingQueue();

    protected Map<String, ClipBuffer> _locked = Maps.newHashMap();

    protected final SoundLoader _loader;
    protected final SoundGroup _group;
    protected final SoundManager _alSoundManager;

    /** Number of sounds that can be played simultaneously. */
    protected final int SOURCE_COUNT = 10;
}
