//
// $Id$

package com.threerings.openal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import org.lwjgl.util.WaveData;

import com.google.common.collect.Maps;

import com.samskivert.util.RunQueue;

import com.threerings.media.sound.SoundLoader;
import com.threerings.media.sound.SoundPlayer;

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
        _alSoundManager = new MediaALSoundManager();
        _group = _alSoundManager.createGroup(this, SOURCE_COUNT);
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
        return RunQueue.AWT;
    }

    @Override
    public void lock (final String pkgPath, String... keys)
    {
        for (final String key : keys) {
            final String path = pkgPath + key;
            if (_locked.containsKey(path)) {
                continue;
            }
            _alSoundManager.loadClip(this, path, new ClipBuffer.Observer() {
                public void clipFailed (ClipBuffer buffer) {
                    log.warning("Unable to load sound", "path", path);
                }

                public void clipLoaded (ClipBuffer buffer) {
                    _locked.put(path, buffer);
                }});
        }
    }

    @Override
    public void unlock (String pkgPath, String... keys)
    {
        for (String key : keys) {
            _locked.remove(pkgPath + key);
        }
    }

    @Override
    protected Frob loop (String pkgPath, String key, float pan)
    {
        final Sound sound = _group.getSound(pkgPath + key);
        sound.loop(true);
        return new Frob(){
            public float getPan () {
                return 0;
            }

            public float getVolume () {
                return 0;
            }

            public void setPan (float pan) {}

            public void setVolume (float vol) { }

            public void stop () {
                sound.stop();
            }};
    }

    @Override
    public void play (String pkgPath, String key, float pan)
    {
        Sound sound = _group.getSound(pkgPath + key);
        sound.play(true);
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

    protected Map<String, ClipBuffer> _locked = Maps.newHashMap();

    protected final SoundLoader _loader;
    protected final SoundGroup _group;
    protected final SoundManager _alSoundManager;

    /** Number of sounds that can be played simultaneously. */
    protected final int SOURCE_COUNT = 10;
}
