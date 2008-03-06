// $Id$

package com.threerings.media.image;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import com.samskivert.util.LRUHashMap;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Throttle;
import com.samskivert.util.Tuple;

import com.threerings.media.Log;
import com.threerings.resource.ResourceManager;

/**
 * Provides a single point of access for image retrieval and caching.  This does not include
 * any tie-in to runtime adjustments to control caching and mirage creation.
 */
public class BaseImageManager
    implements ImageUtil.ImageCreator
{
    /**
     * Used to identify an image for caching and reconstruction.
     */
    public static class ImageKey
    {
        /** The data provider from which this image's data is loaded. */
        public ImageDataProvider daprov;

        /** The path used to identify the image to the data provider. */
        public String path;

        protected ImageKey (ImageDataProvider daprov, String path)
        {
            this.daprov = daprov;
            this.path = path;
        }

        public int hashCode ()
        {
            return path.hashCode() ^ daprov.getIdent().hashCode();
        }

        public boolean equals (Object other)
        {
            if (other == null || !(other instanceof ImageKey)) {
                return false;
            }

            ImageKey okey = (ImageKey)other;
            return ((okey.daprov.getIdent().equals(daprov.getIdent())) &&
                    (okey.path.equals(path)));
        }

        public String toString ()
        {
            return daprov.getIdent() + ":" + path;
        }
    }

    /**
     * This interface allows the image manager to create images that are in a format optimal for
     * rendering to the screen.
     */
    public interface OptimalImageCreator
    {
        /**
         * Requests that a blank image be created that is in a format and of a depth that are
         * optimal for rendering to the screen.
         */
        public BufferedImage createImage (int width, int height, int trans);
    }

    /**
     * Construct an image manager with the specified {@link ResourceManager} from which it will
     * obtain its data.
     */
    public BaseImageManager (ResourceManager rmgr, OptimalImageCreator icreator)
    {
        _rmgr = rmgr;
        _icreator = icreator;

        // create our image cache
        int icsize = getCacheSize();
        Log.debug("Creating image cache [size=" + icsize + "k].");
        _ccache = new LRUHashMap(icsize * 1024, new LRUHashMap.ItemSizer() {
            public int computeSize (Object value) {
                return (int)((CacheRecord)value).getEstimatedMemoryUsage();
            }
        });
        _ccache.setTracking(true);
    }

    /**
     * A convenience constructor that creates an {@link AWTImageCreator} for use by the image
     * manager.
     */
    public BaseImageManager (ResourceManager rmgr, Component context)
    {
        this(rmgr, new AWTImageCreator(context));
    }

    /**
     * Returns how much space we're willing to use for caching images.
     */
    public int getCacheSize ()
    {
        return DEFAULT_CACHE_SIZE;
    }

    /**
     * Clears all images out of the cache.
     */
    public void clearCache ()
    {
        Log.info("Clearing image manager cache.");

        _ccache.clear();
    }

    /**
     * Creates a buffered image, optimized for display on our graphics device.
     */
    public BufferedImage createImage (int width, int height, int transparency)
    {
        return _icreator.createImage(width, height, transparency);
    }

    /**
     * Loads (and caches) the specified image from the resource manager using the supplied path to
     * identify the image.
     */
    public BufferedImage getImage (String path)
    {
        return getImage(null, path, null);
    }

    /**
     * Like {@link #getImage(String)} but the specified colorizations are applied to the image
     * before it is returned.
     */
    public BufferedImage getImage (String path, Colorization[] zations)
    {
        return getImage(null, path, zations);
    }

    /**
     * Like {@link #getImage(String)} but the image is loaded from the specified resource set
     * rathern than the default resource set.
     */
    public BufferedImage getImage (String rset, String path)
    {
        return getImage(rset, path, null);
    }

    /**
     * Like {@link #getImage(String,String)} but the specified colorizations are applied to the
     * image before it is returned.
     */
    public BufferedImage getImage (String rset, String path, Colorization[] zations)
    {
        if (StringUtil.isBlank(path)) {
            String errmsg = "Invalid image path [rset=" + rset + ", path=" + path + "]";
            throw new IllegalArgumentException(errmsg);
        }
        return getImage(getImageKey(rset, path), zations);
    }

    /**
     * Loads (and caches) the specified image from the resource manager using the supplied path to
     * identify the image.
     *
     * <p> Additionally the image is optimized for display in the current graphics
     * configuration. Consider using {@link #getMirage(ImageKey)} instead of prepared images as
     * they (some day) will automatically use volatile images to increase performance.
     */
    public BufferedImage getPreparedImage (String path)
    {
        return getPreparedImage(null, path, null);
    }

    /**
     * Loads (and caches) the specified image from the resource manager, obtaining the image from
     * the supplied resource set.
     *
     * <p> Additionally the image is optimized for display in the current graphics
     * configuration. Consider using {@link #getMirage(ImageKey)} instead of prepared images as
     * they (some day) will automatically use volatile images to increase performance.
     */
    public BufferedImage getPreparedImage (String rset, String path)
    {
        return getPreparedImage(rset, path, null);
    }

    /**
     * Loads (and caches) the specified image from the resource manager, obtaining the image from
     * the supplied resource set and applying the using the supplied path to identify the image.
     *
     * <p> Additionally the image is optimized for display in the current graphics
     * configuration. Consider using {@link #getMirage(ImageKey,Colorization[])} instead of
     * prepared images as they (some day) will automatically use volatile images to increase
     * performance.
     */
    public BufferedImage getPreparedImage (String rset, String path, Colorization[] zations)
    {
        BufferedImage image = getImage(rset, path, zations);
        BufferedImage prepped = null;
        if (image != null) {
            prepped = createImage(image.getWidth(), image.getHeight(),
                                  image.getColorModel().getTransparency());
            Graphics2D pg = prepped.createGraphics();
            pg.drawImage(image, 0, 0, null);
            pg.dispose();
        }
        return prepped;
    }

    /**
     * Returns an image key that can be used to fetch the image identified by the specified
     * resource set and image path.
     */
    public ImageKey getImageKey (String rset, String path)
    {
        return getImageKey(getDataProvider(rset), path);
    }

    /**
     * Returns an image key that can be used to fetch the image identified by the specified data
     * provider and image path.
     */
    public ImageKey getImageKey (ImageDataProvider daprov, String path)
    {
        return new ImageKey(daprov, path);
    }

    /**
     * Obtains the image identified by the specified key, caching if possible. The image will be
     * recolored using the supplied colorizations if requested.
     */
    public BufferedImage getImage (ImageKey key, Colorization[] zations)
    {
        CacheRecord crec = null;
        synchronized (_ccache) {
            crec = (CacheRecord)_ccache.get(key);
        }
        if (crec != null) {
//             Log.info("Cache hit [key=" + key + ", crec=" + crec + "].");
            return crec.getImage(zations, _ccache);
        }
//         Log.info("Cache miss [key=" + key + ", crec=" + crec + "].");

        // load up the raw image
        BufferedImage image = loadImage(key);
        if (image == null) {
            Log.warning("Failed to load image " + key + ".");
            // create a blank image instead
            image = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_INDEXED);
        }

//         Log.info("Loaded " + key.path + ", image=" + image +
//                  ", size=" + ImageUtil.getEstimatedMemoryUsage(image));

        // create a cache record
        crec = new CacheRecord(key, image);
        synchronized (_ccache) {
            _ccache.put(key, crec);
        }
        _keySet.add(key);

        // periodically report our image cache performance
        reportCachePerformance();

        return crec.getImage(zations, _ccache);
    }

    /**
     * Creates a mirage which is an image optimized for display on our current display device and
     * which will be stored into video memory if possible.
     */
    public Mirage getMirage (String rsrcPath)
    {
        return getMirage(getImageKey(_defaultProvider, rsrcPath), null, null);
    }

    /**
     * Creates a mirage which is an image optimized for display on our current display device and
     * which will be stored into video memory if possible.
     */
    public Mirage getMirage (ImageKey key)
    {
        return getMirage(key, null, null);
    }

    /**
     * Like {@link #getMirage(ImageKey)} but that only the specified subimage of the source image
     * is used to build the mirage.
     */
    public Mirage getMirage (ImageKey key, Rectangle bounds)
    {
        return getMirage(key, bounds, null);
    }

    /**
     * Like {@link #getMirage(ImageKey)} but the supplied colorizations are applied to the source
     * image before creating the mirage.
     */
    public Mirage getMirage (ImageKey key, Colorization[] zations)
    {
        return getMirage(key, null, zations);
    }

    /**
     * Like {@link #getMirage(ImageKey,Colorization[])} except that the mirage is created using
     * only the specified subset of the original image.
     */
    public Mirage getMirage (ImageKey key, Rectangle bounds, Colorization[] zations)
    {
        BufferedImage src = null;

        float percentageOfDataBuffer = 1;
        if (bounds == null) {
            // if they specified no bounds, we need to load up the raw image and determine its
            // bounds so that we can pass those along to the created mirage
            src = getImage(key, zations);
            bounds = new Rectangle(0, 0, src.getWidth(), src.getHeight());

        }

        return new CachedVolatileMirage(this, key, bounds, zations);

    }

    /**
     * Returns the image creator that can be used to create buffered images optimized for rendering
     * to the screen.
     */
    public OptimalImageCreator getImageCreator ()
    {
        return _icreator;
    }

    /**
     * Returns the data provider configured to obtain image data from the specified resource set.
     */
    protected ImageDataProvider getDataProvider (final String rset)
    {
        if (rset == null) {
            return _defaultProvider;
        }

        ImageDataProvider dprov = (ImageDataProvider)_providers.get(rset);
        if (dprov == null) {
            dprov = new ImageDataProvider() {
                public BufferedImage loadImage (String path)
                    throws IOException {
                    // first attempt to load the image from the specified resource set
                    try {
                        return _rmgr.getImageResource(rset, path);
                    } catch (FileNotFoundException fnfe) {
                        // fall back to trying the classpath
                        return _rmgr.getImageResource(path);
                    }
                }

                public String getIdent () {
                    return "rmgr:" + rset;
                }
            };
            _providers.put(rset, dprov);
        }

        return dprov;
    }

    /**
     * Loads and returns the image with the specified key from the supplied data provider.
     */
    protected BufferedImage loadImage (ImageKey key)
    {
//         if (EventQueue.isDispatchThread()) {
//             Log.info("Loading image on AWT thread " + key + ".");
//         }

        BufferedImage image = null;
        try {
            Log.debug("Loading image " + key + ".");
            image = key.daprov.loadImage(key.path);
            if (image == null) {
                Log.warning("ImageDataProvider.loadImage(" + key + ") returned null.");
            }

        } catch (Exception e) {
            Log.warning("Unable to load image '" + key + "'.");
            Log.logStackTrace(e);

            // create a blank image in its stead
            image = createImage(1, 1, Transparency.OPAQUE);
        }

        return image;
    }

    /**
     * Reports statistics detailing the image manager cache performance and the current size of the
     * cached images.
     */
    protected void reportCachePerformance ()
    {
        if (/* Log.getLevel() != Log.log.DEBUG || */
            _cacheStatThrottle.throttleOp()) {
            return;
        }

        // compute our estimated memory usage
        long size = 0;

        int[] eff = null;
        synchronized (_ccache) {
            Iterator iter = _ccache.values().iterator();
            while (iter.hasNext()) {
                size += ((CacheRecord)iter.next()).getEstimatedMemoryUsage();
            }
            eff = _ccache.getTrackedEffectiveness();
        }
        Log.info("ImageManager LRU [mem=" + (size / 1024) + "k, size=" + _ccache.size() +
            ", hits=" + eff[0] + ", misses=" + eff[1] + ", totalKeys=" + _keySet.size() + "].");
    }

    /** Maintains a source image and a set of colorized versions in the image cache. */
    protected static class CacheRecord
    {
        public CacheRecord (ImageKey key, BufferedImage source)
        {
            _key = key;
            _source = source;
        }

        public BufferedImage getImage (Colorization[] zations, LRUHashMap cache)
        {
            if (zations == null) {
                return _source;
            }

            if (_colorized == null) {
                _colorized = new ArrayList<Tuple<Colorization[], BufferedImage>>();
            }

            // we search linearly through our list of colorized copies because it is not likely to
            // be very long
            int csize = _colorized.size();
            for (int ii = 0; ii < csize; ii++) {
                Tuple<Colorization[], BufferedImage> tup = _colorized.get(ii);
                Colorization[] tzations = tup.left;
                if (Arrays.equals(zations, tzations)) {
                    return tup.right;
                }
            }

            try {
                BufferedImage cimage = ImageUtil.recolorImage(_source, zations);
                _colorized.add(new Tuple<Colorization[], BufferedImage>(zations, cimage));
                cache.adjustSize((int)ImageUtil.getEstimatedMemoryUsage(cimage));
                return cimage;

            } catch (Exception re) {
                Log.warning("Failure recoloring image [source" + _key +
                            ", zations=" + StringUtil.toString(zations) + ", error=" + re + "].");
                // return the uncolorized version
                return _source;
            }
        }

        public long getEstimatedMemoryUsage ()
        {
            long usage = ImageUtil.getEstimatedMemoryUsage(_source);
            if (_colorized != null) {
                for (Tuple<Colorization[], BufferedImage> tup : _colorized) {
                    usage += ImageUtil.getEstimatedMemoryUsage(tup.right);
                }
            }
            return usage;
        }

        public String toString ()
        {
            return "[key=" + _key + ", wid=" + _source.getWidth() + ", hei=" + _source.getHeight() +
                ", ccount=" + ((_colorized == null) ? 0 : _colorized.size()) + "]";
        }

        protected ImageKey _key;
        protected BufferedImage _source;
        protected ArrayList<Tuple<Colorization[], BufferedImage>> _colorized;
    }

    /** A reference to the resource manager via which we load image data by default. */
    protected ResourceManager _rmgr;

    /** We use this to create images optimized for rendering. */
    protected OptimalImageCreator _icreator;

    /** A cache of loaded images. */
    protected LRUHashMap _ccache;

    /** The set of all keys we've ever seen. */
    protected HashSet _keySet = new HashSet();

    /** Throttle our cache status logging to once every 300 seconds. */
    protected Throttle _cacheStatThrottle = new Throttle(1, 300000L);

    /** Our default data provider. */
    protected ImageDataProvider _defaultProvider = new ImageDataProvider() {
        public BufferedImage loadImage (String path) throws IOException {
            return _rmgr.getImageResource(path);
        }
        public String getIdent () {
            return "rmgr:default";
        }
    };

    /** Data providers for different resource sets. */
    protected HashMap _providers = new HashMap();

    /** Default amount of data we'll store in our image cache. */
    protected static int DEFAULT_CACHE_SIZE = 32768;
}