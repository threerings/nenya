package com.threerings.media.sound;

import static com.threerings.media.Log.log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

import com.samskivert.util.Config;
import com.samskivert.util.ConfigUtil;
import com.samskivert.util.LRUHashMap;
import com.threerings.resource.ResourceManager;

/**
 * Loads sound clips specified by package-based properties files.
 */
public class SoundLoader
{
    public SoundLoader (ResourceManager rmgr, String defaultBundle, String defaultPath)
    {
        _rmgr = rmgr;
        _defaultClipBundle = defaultBundle;
        _defaultClipPath = defaultPath;
    }

    /**
     * Loads the sounds for key from the config in
     * <code>&lt;packagePath&gt;/sound.properties</code>
     */
    public byte[][] load (String packagePath, String key)
        throws IOException
    {
        String[] paths = getPaths(packagePath, key);
        if (paths == null) {
            log.warning("No such sound", "key", key);
            return null;
        }

        byte[][] data = new byte[paths.length][];
        String bundle = getBundle(packagePath);
        for (int ii = 0; ii < paths.length; ii++) {
            data[ii] = loadClipData(bundle, paths[ii]);
        }
        return data;
    }

    /**
     * Returns the paths to sounds for <code>key</code> in the given package. Returns null if no
     * sounds are found.
     */
    public String[] getPaths (String packagePath, String key)
    {
        return getConfig(packagePath).getValue(key, (String[])null);
    }


    /**
     * Returns the bundle for sounds in the given package.  Returns null if the bundle isn't found.
     */
    public String getBundle (String packagePath)
    {
        return getConfig(packagePath).getValue("bundle", (String)null);
    }

    /**
     * Attempts to load a sound stream from the given path from the given bundle and from the
     * classpath. If nothing is found, a FileNotFoundException is thrown.
     */
    public InputStream getSound (String bundle, String path)
        throws IOException
    {
        InputStream rsrc;
        try {
            rsrc = _rmgr.getResource(bundle, path);
        } catch (FileNotFoundException notFound) {
            // try from the classpath
            try {
                rsrc = _rmgr.getResource(path);
            } catch (FileNotFoundException notFoundAgain) {
                throw notFound;
            }
        }
        return rsrc;
    }

    public void shutdown ()
    {
        _configs.clear();
    }

    /**
     * Get the cached Config.
     */
    protected Config getConfig (String packagePath)
    {
        Config c = _configs.get(packagePath);
        if (c == null) {
            String propPath = packagePath + Sounds.PROP_NAME;
            Properties props = new Properties();
            try {
                props = ConfigUtil.loadInheritedProperties(propPath + ".properties",
                    _rmgr.getClassLoader());
            } catch (IOException ioe) {
                log.warning("Failed to load sound properties", "path", propPath, ioe);
            }
            c = new Config(propPath, props);
            _configs.put(packagePath, c);
        }
        return c;
    }

    /**
     * Read the data from the resource manager.
     */
    protected byte[] loadClipData (String bundle, String path)
        throws IOException
    {
        InputStream clipin = null;
        try {
            clipin = getSound(bundle, path);
        } catch (FileNotFoundException fnfe) {
            // only play the default sound if we have verbose sound debugging turned on.
            if (JavaSoundPlayer._verbose.getValue()) {
                log.warning("Could not locate sound data", "bundle", bundle, "path", path);
                if (_defaultClipPath != null) {
                    try {
                        clipin = _rmgr.getResource(_defaultClipBundle, _defaultClipPath);
                    } catch (FileNotFoundException fnfe3) {
                        try {
                            clipin = _rmgr.getResource(_defaultClipPath);
                        } catch (FileNotFoundException fnfe4) {
                            log.warning(
                                "Additionally, the default fallback sound could not be located",
                                "bundle", _defaultClipBundle, "path", _defaultClipPath);
                        }
                    }
                } else {
                    log.warning("No fallback default sound specified!");
                }
            }
            // if we couldn't load the default, rethrow
            if (clipin == null) {
                throw fnfe;
            }
        }

        return IOUtils.toByteArray(clipin);
    }

    protected ResourceManager _rmgr;

    /** The path of the default sound to use for missing sounds. */
    protected String _defaultClipBundle, _defaultClipPath;

    /** A cache of config objects we've created. */
    protected LRUHashMap<String, Config> _configs = new LRUHashMap<String, Config>(5);
}
