//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2007 Three Rings Design, Inc., All Rights Reserved
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

package com.threerings.resource;

import java.awt.EventQueue;
import java.awt.image.BufferedImage;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

import com.samskivert.io.StreamUtil;
import com.samskivert.net.PathUtil;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;

/**
 * The resource manager is responsible for maintaining a repository of resources that are
 * synchronized with a remote source. This is accomplished in the form of sets of jar files
 * (resource bundles) that contain resources and that are updated from a remote resource repository
 * via HTTP.  These resource bundles are organized into resource sets. A resource set contains one
 * or more resource bundles and is defined much like a classpath.
 *
 * <p> The resource manager can load resources from the default resource set, and can make
 * available named resource sets to entities that wish to do their own resource loading. If the
 * resource manager fails to locate a resource in the default resource set, it falls back to
 * loading the resource via the classloader (which will search the classpath).
 *
 * <p> Applications that wish to make use of resource sets and their associated bundles must call
 * {@link #initBundles} after constructing the resource manager, providing the path to a resource
 * definition file which describes these resource sets. The definition file will be loaded and the
 * resource bundles defined within will be loaded relative to the resource directory.  The bundles
 * will be cached in the user's home directory and only reloaded when the source resources have
 * been updated. The resource definition file looks something like the following:
 *
 * <pre>
 * resource.set.default = sets/misc/config.jar: \
 *                        sets/misc/icons.jar
 * resource.set.tiles = sets/tiles/ground.jar: \
 *                      sets/tiles/objects.jar: \
 *                      /global/resources/tiles/ground.jar: \
 *                      /global/resources/tiles/objects.jar
 * resource.set.sounds = sets/sounds/sfx.jar: \
 *                       sets/sounds/music.jar: \
 *                       /global/resources/sounds/sfx.jar: \
 *                       /global/resources/sounds/music.jar
 * </pre>
 *
 * <p> All resource set definitions are prefixed with <code>resource.set.</code> and all text
 * following that string is considered to be the name of the resource set. The resource set named
 * <code>default</code> is the default resource set and is the one that is searched for resources
 * is a call to {@link #getResource}.
 *
 * <p> When a resource is loaded from a resource set, the set is searched in the order that entries
 * are specified in the definition.
 */
public class ResourceManager
{
    /**
     * Provides facilities for notifying an observer of the resource unpacking process.
     */
    public interface InitObserver
    {
        /**
         * Indicates a percent completion along with an estimated time remaining in seconds.
         */
        public void progress (int percent, long remaining);

        /**
         * Indicates that there was a failure unpacking our resource bundles.
         */
        public void initializationFailed (Exception e);
    }

    /**
     * An adapter that wraps an {@link InitObserver} and routes all method invocations to the AWT
     * thread.
     */
    public static class AWTInitObserver implements InitObserver
    {
        public AWTInitObserver (InitObserver obs) {
            _obs = obs;
        }

        public void progress (final int percent, final long remaining) {
            EventQueue.invokeLater(new Runnable() {
                public void run () {
                    _obs.progress(percent, remaining);
                }
            });
        }

        public void initializationFailed (final Exception e) {
            EventQueue.invokeLater(new Runnable() {
                public void run () {
                    _obs.initializationFailed(e);
                }
            });
        }

        protected InitObserver _obs;
    }

    /**
     * Constructs a resource manager which will load resources via the classloader, prepending
     * <code>resourceRoot</code> to their path.
     *
     * @param resourceRoot the path to prepend to resource paths prior to attempting to load them
     * via the classloader. When resources are bundled into the default resource bundle, they don't
     * need this prefix, but if they're to be loaded from the classpath, it's likely that they'll
     * live in some sort of <code>resources</code> directory to isolate them from the rest of the
     * files in the classpath. This is not a platform dependent path (forward slash is always used
     * to separate path elements).
     */
    public ResourceManager (String resourceRoot)
    {
        this(resourceRoot, ResourceManager.class.getClassLoader());
    }

    /**
     * Creates a resource manager with the specified class loader via which to load classes. See
     * {@link #ResourceManager(String)} for further documentation.
     */
    public ResourceManager (String resourceRoot, ClassLoader loader)
    {
        _rootPath = resourceRoot;
        _loader = loader;

        // check a system property to determine if we should unpack our bundles, but don't freak
        // out if we fail to read it
        try {
            _unpack = !Boolean.getBoolean("no_unpack_resources");
        } catch (SecurityException se) {
            // no problem, we're in a sandbox so we definitely won't be unpacking
        }

        // get our resource directory from resource_dir if possible
        initResourceDir(null);
    }

    /**
     * Registers a protocol handler with URL to handle <code>resource:</code> URLs. The URLs take
     * the form: <pre>resource://bundle_name/resource_path</pre> Resources from the default bundle
     * can be loaded via: <pre>resource:///resource_path</pre>
     */
    public void activateResourceProtocol ()
    {
        // set up a URL handler so that things can be loaded via urls with the 'resource' protocol
        try {
            AccessController.doPrivileged(new PrivilegedAction() {
                public Object run () {
                    Handler.registerHandler(ResourceManager.this);
                    return null;
                }
            });
        } catch (SecurityException se) {
            Log.info("Running in sandbox. Unable to bind rsrc:// handler.");
        }
    }

    /**
     * Set where we should look for locale-specific resources.
     */
    public void setLocalePrefix (String prefix)
    {
        _localePrefix = prefix;
    }

    /**
     * Configures whether we unpack our resource bundles or not. This must be called before {@link
     * #initBundles}. One can also pass the <code>-Dno_unpack_resources=true</code> system property
     * to disable resource unpacking.
     */
    public void setUnpackResources (boolean unpackResources)
    {
        _unpack = unpackResources;
    }

    /**
     * Initializes the bundle sets to be made available by this resource manager.  Applications
     * that wish to make use of resource bundles should call this method after constructing the
     * resource manager.
     *
     * @param resourceDir the base directory to which the paths in the supplied configuration file
     * are relative. If this is null, the system property <code>resource_dir</code> will be used,
     * if available.
     * @param configPath the path (relative to the resource dir) of the resource definition file.
     * @param initObs a bundle initialization observer to notify of unpacking progress and success
     * or failure, or <code>null</code> if the caller doesn't care to be informed; note that in the
     * latter case, the calling thread will block until bundle unpacking is complete.
     *
     * @exception IOException thrown if we are unable to read our resource manager configuration.
     */
    public void initBundles (String resourceDir, String configPath, InitObserver initObs)
        throws IOException
    {
        // reinitialize our resource dir if it was specified
        if (resourceDir != null) {
            initResourceDir(resourceDir);
        }

        // load up our configuration
        Properties config = new Properties();
        try {
            config.load(new FileInputStream(new File(_rdir, configPath)));
        } catch (Exception e) {
            String errmsg = "Unable to load resource manager config [rdir=" + _rdir +
                ", cpath=" + configPath + "]";
            Log.warning(errmsg + ".");
            Log.logStackTrace(e);
            throw new IOException(errmsg);
        }

        // resolve the configured resource sets
        List<ResourceBundle> dlist = new ArrayList<ResourceBundle>();
        Enumeration names = config.propertyNames();
        while (names.hasMoreElements()) {
            String key = (String)names.nextElement();
            if (!key.startsWith(RESOURCE_SET_PREFIX)) {
                continue;
            }
            String setName = key.substring(RESOURCE_SET_PREFIX.length());
            resolveResourceSet(setName, config.getProperty(key), dlist);
        }

        // if an observer was passed in, then we do not need to block the caller
        final boolean[] shouldWait = new boolean[] { false };
        if (initObs == null) {
            // if there's no observer, we'll need to block the caller
            shouldWait[0] = true;
            initObs = new InitObserver() {
                public void progress (int percent, long remaining) {
                    if (percent >= 100) {
                        synchronized (this) {
                            // turn off shouldWait, in case we reached 100% progress before the
                            // calling thread even gets a chance to get to the blocking code, below
                            shouldWait[0] = false;
                            notify();
                        }
                    }
                }
                public void initializationFailed (Exception e) {
                    synchronized (this) {
                        shouldWait[0] = false;
                        notify();
                    }
                }
            };
        }

        // start a thread to unpack our bundles
        Unpacker unpack = new Unpacker(dlist, initObs);
        unpack.start();

        if (shouldWait[0]) {
            synchronized (initObs) {
                if (shouldWait[0]) {
                    try {
                        initObs.wait();
                    } catch (InterruptedException ie) {
                        Log.warning("Interrupted while waiting for bundles to unpack.");
                    }
                }
            }
        }
    }

    /**
     * Given a path relative to the resource directory, the path is properly jimmied (assuming we
     * always use /) and combined with the resource directory to yield a {@link File} object that
     * can be used to access the resource.
     *
     * @return a file referencing the specified resource or null if the resource manager was never
     * configured with a resource directory.
     */
    public File getResourceFile (String path)
    {
        if (_rdir == null) {
            return null;
        }
        if (!"/".equals(File.separator)) {
            path = StringUtil.replace(path, "/", File.separator);
        }
        return new File(_rdir, path);
    }

    /**
     * Checks to see if the specified bundle exists, is unpacked and is ready to be used.
     */
    public boolean checkBundle (String path)
    {
        File bfile = getResourceFile(path);
        return (bfile == null) ? false : new FileResourceBundle(bfile, true, _unpack).isUnpacked();
    }

    /**
     * Resolve the specified bundle (the bundle file must already exist in the appropriate place on
     * the file system) and return it on the specified result listener. Note that the result
     * listener may be notified before this method returns on the caller's thread if the bundle is
     * already resolved, or it may be notified on a brand new thread if the bundle requires
     * unpacking.
     */
    public void resolveBundle (String path, final ResultListener listener)
    {
        File bfile = getResourceFile(path);
        if (bfile == null) {
            String errmsg = "ResourceManager not configured with resource directory.";
            listener.requestFailed(new IOException(errmsg));
            return;
        }

        final FileResourceBundle bundle = new FileResourceBundle(bfile, true, _unpack);
        if (bundle.isUnpacked()) {
            if (bundle.sourceIsReady()) {
                listener.requestCompleted(bundle);
            } else {
                String errmsg = "Bundle initialization failed.";
                listener.requestFailed(new IOException(errmsg));
            }
            return;
        }

        // start a thread to unpack our bundles
        ArrayList list = new ArrayList();
        list.add(bundle);
        Unpacker unpack = new Unpacker(list, new InitObserver() {
            public void progress (int percent, long remaining) {
                if (percent == 100) {
                    listener.requestCompleted(bundle);
                }
            }
            public void initializationFailed (Exception e) {
                listener.requestFailed(e);
            }
        });
        unpack.start();
    }

    /**
     * Returns the class loader being used to load resources if/when there are no resource bundles
     * from which to load them.
     */
    public ClassLoader getClassLoader ()
    {
        return _loader;
    }

    /**
     * Configures the class loader this manager should use to load resources if/when there are no
     * bundles from which to load them.
     */
    public void setClassLoader (ClassLoader loader)
    {
        _loader = loader;
    }

    /**
     * Fetches a resource from the local repository.
     *
     * @param path the path to the resource (ie. "config/miso.properties"). This should not begin
     * with a slash.
     *
     * @exception IOException thrown if a problem occurs locating or reading the resource.
     */
    public InputStream getResource (String path)
        throws IOException
    {
        InputStream in = null;

        // first look for this resource in our default resource bundle
        for (ResourceBundle bundle : _default) {
            in = bundle.getResource(path);
            if (in != null) {
                return in;
            }
        }

        // fallback next to an unpacked resource file
        File file = getResourceFile(path);
        if (file != null && file.exists()) {
            return new FileInputStream(file);
        }

        // if we still didn't find anything, try the classloader; first try a locale-specific file
        if (_localePrefix != null) {
            final String rpath = PathUtil.appendPath(
                _rootPath, PathUtil.appendPath(_localePrefix, path));
            in = (InputStream)AccessController.doPrivileged(new PrivilegedAction() {
                public Object run () {
                    return _loader.getResourceAsStream(rpath);
                }
            });
            if (in != null) {
                return in;
            }
        }

        // if we didn't find that, try locale-neutral
        final String rpath = PathUtil.appendPath(_rootPath, path);
        in = (InputStream)AccessController.doPrivileged(new PrivilegedAction() {
            public Object run () {
                return _loader.getResourceAsStream(rpath);
            }
        });
        if (in != null) {
            return in;
        }

        // if we still haven't found it, we throw an exception
        String errmsg = "Unable to locate resource [path=" + path + "]";
        throw new FileNotFoundException(errmsg);
    }

    /**
     * Fetches and decodes the specified resource into a {@link BufferedImage}.
     *
     * @exception FileNotFoundException thrown if the resource could not be located in any of the
     * bundles in the specified set, or if the specified set does not exist.
     * @exception IOException thrown if a problem occurs locating or reading the resource.
     */
    public BufferedImage getImageResource (String path)
        throws IOException
    {
        // first look for this resource in our default resource bundle
        for (ResourceBundle bundle : _default) {
            BufferedImage image = bundle.getImageResource(path, false);
            if (image != null) {
                return image;
            }
        }

        // fallback next to an unpacked resource file
        File file = getResourceFile(path);
        if (file != null && file.exists()) {
            if (path.endsWith(FastImageIO.FILE_SUFFIX)) {
                return loadImage(file, true);
            } else {
                return loadImage(file, false);
            }
        }

        // first try a locale-specific file
        if (_localePrefix != null) {
            final String rpath = PathUtil.appendPath(
                _rootPath, PathUtil.appendPath(_localePrefix, path));
            InputStream in = (InputStream)AccessController.doPrivileged(new PrivilegedAction() {
                public Object run () {
                    return _loader.getResourceAsStream(rpath);
                }
            });
            if (in != null) {
                return loadImage(in);
            }
        }

        // if we still didn't find anything, try the classloader
        final String rpath = PathUtil.appendPath(_rootPath, path);
        InputStream in = (InputStream)AccessController.doPrivileged(new PrivilegedAction() {
            public Object run () {
                return _loader.getResourceAsStream(rpath);
            }
        });
        if (in != null) {
            return loadImage(in);
        }

        // if we still haven't found it, we throw an exception
        String errmsg = "Unable to locate image resource [path=" + path + "]";
        throw new FileNotFoundException(errmsg);
    }

    /**
     * Returns an input stream from which the requested resource can be loaded. <em>Note:</em> this
     * performs a linear search of all of the bundles in the set and returns the first resource
     * found with the specified path, thus it is not extremely efficient and will behave
     * unexpectedly if you use the same paths in different resource bundles.
     *
     * @exception FileNotFoundException thrown if the resource could not be located in any of the
     * bundles in the specified set, or if the specified set does not exist.
     * @exception IOException thrown if a problem occurs locating or reading the resource.
     */
    public InputStream getResource (String rset, String path)
        throws IOException
    {
        // grab the resource bundles in the specified resource set
        ResourceBundle[] bundles = getResourceSet(rset);
        if (bundles == null) {
            throw new FileNotFoundException(
                "Unable to locate resource [set=" + rset + ", path=" + path + "]");
        }

        // look for the resource in any of the bundles
        int size = bundles.length;
        for (int ii = 0; ii < size; ii++) {
            InputStream instr = null;

            // Try a localized version first.
            if (_localePrefix != null) {
                instr = bundles[ii].getResource(PathUtil.appendPath(_localePrefix, path));
            }
            // If we didn't find that, try a generic.
            if (instr == null) {
                instr = bundles[ii].getResource(path);
            }
            if (instr != null) {
//                 Log.info("Found resource [rset=" + rset +
//                          ", bundle=" + bundles[ii].getSource().getPath() +
//                          ", path=" + path + ", in=" + instr + "].");
                return instr;
            }
        }

        throw new FileNotFoundException(
            "Unable to locate resource [set=" + rset + ", path=" + path + "]");
    }

    /**
     * Fetches and decodes the specified resource into a {@link BufferedImage}.
     *
     * @exception FileNotFoundException thrown if the resource could not be located in any of the
     * bundles in the specified set, or if the specified set does not exist.
     * @exception IOException thrown if a problem occurs locating or reading the resource.
     */
    public BufferedImage getImageResource (String rset, String path)
        throws IOException
    {
        // grab the resource bundles in the specified resource set
        ResourceBundle[] bundles = getResourceSet(rset);
        if (bundles == null) {
            throw new FileNotFoundException(
                "Unable to locate image resource [set=" + rset + ", path=" + path + "]");
        }

        // look for the resource in any of the bundles
        int size = bundles.length;
        for (int ii = 0; ii < size; ii++) {
            BufferedImage image = null;
            // try a localized version first
            if (_localePrefix != null) {
                image =
                    bundles[ii].getImageResource(PathUtil.appendPath(_localePrefix, path), false);
            }

            // if we didn't find that, try generic
            if (image == null) {
                image = bundles[ii].getImageResource(path, false);
            }

            if (image != null) {
//                 Log.info("Found image resource [rset=" + rset +
//                          ", bundle=" + bundles[ii].getSource() + ", path=" + path + "].");
                return image;
            }
        }

        String errmsg = "Unable to locate image resource [set=" + rset + ", path=" + path + "]";
        throw new FileNotFoundException(errmsg);
    }

    /**
     * Returns a reference to the resource set with the specified name, or null if no set exists
     * with that name. Services that wish to load their own resources can allow the resource
     * manager to load up a resource set for them, from which they can easily load their resources.
     */
    public ResourceBundle[] getResourceSet (String name)
    {
        return (ResourceBundle[])_sets.get(name);
    }

    protected void initResourceDir (String resourceDir)
    {
        // if none was specified, check the resource_dir system property
        if (resourceDir == null) {
            try {
                resourceDir = System.getProperty("resource_dir");
            } catch (SecurityException se) {
                // no problem
            }
        }

        // if we found no resource directory, don't use one
        if (resourceDir == null) {
            return;
        }

        // make sure there's a trailing slash
        if (!resourceDir.endsWith(File.separator)) {
            resourceDir += File.separator;
        }
        _rdir = new File(resourceDir);
    }

    /**
     * Loads up a resource set based on the supplied definition information.
     */
    protected void resolveResourceSet (
        String setName, String definition, List<ResourceBundle> dlist)
    {
        List<ResourceBundle> set = new ArrayList<ResourceBundle>();
        StringTokenizer tok = new StringTokenizer(definition, ":");
        while (tok.hasMoreTokens()) {
            String path = tok.nextToken().trim();
            FileResourceBundle bundle =
                new FileResourceBundle(getResourceFile(path), true, _unpack);
            set.add(bundle);
            if (bundle.isUnpacked() && bundle.sourceIsReady()) {
                continue;
            }
            dlist.add(bundle);
        }

        // convert our array list into an array and stick it in the table
        ResourceBundle[] setvec = set.toArray(new ResourceBundle[set.size()]);
        _sets.put(setName, setvec);

        // if this is our default resource bundle, keep a reference to it
        if (DEFAULT_RESOURCE_SET.equals(setName)) {
            _default = setvec;
        }
    }

    /**
     * Loads an image from the supplied file. Supports {@link FastImageIO} files and formats
     * supported by {@link ImageIO} and will load the appropriate one based on the useFastIO param.
     */
    protected static BufferedImage loadImage (File file, boolean useFastIO)
        throws IOException
    {
        if (file == null) {
            return null;
        } else if (useFastIO) {
            return FastImageIO.read(file);
        } else {
            return ImageIO.read(file);
        }
    }

    /**
     * Loads an image from the supplied input stream. Supports formats supported by {@link ImageIO}
     * but not {@link FastImageIO}.
     */
    protected static BufferedImage loadImage (InputStream iis)
        throws IOException
    {
        BufferedImage image;

        if (iis instanceof ImageInputStream) {
            image = ImageIO.read(iis);

        } else {
            // if we don't already have an image input stream, create a memory cache image input
            // stream to avoid causing freakout if we're used in a sandbox because ImageIO
            // otherwise use FileCacheImageInputStream which tries to create a temp file
            MemoryCacheImageInputStream mciis = new MemoryCacheImageInputStream(iis);
            image = ImageIO.read(mciis);
            try {
                // this doesn't close the underlying stream
                mciis.close();
            } catch (IOException ioe) {
                // ImageInputStreamImpl.close() throws an IOException if it's already closed;
                // there's no way to find out if it's already closed or not, so we have to check
                // the exception message to determine if this is actually warning worthy
                if (!"closed".equals(ioe.getMessage())) {
                    Log.warning("Failure closing image input '" + iis + "'.");
                    Log.logStackTrace(ioe);
                }
            }
        }

        // finally close our input stream
        StreamUtil.close(iis);

        return image;
    }

    /** Used to unpack bundles on a separate thread. */
    protected static class Unpacker extends Thread
    {
        public Unpacker (List<ResourceBundle> bundles, InitObserver obs)
        {
            _bundles = bundles;
            _obs = obs;
        }

        public void run ()
        {
            try {
                // Tell the observer were starting
                if (_obs != null) {
                    _obs.progress(0, 1);
                }

                int count = 0;
                for (ResourceBundle bundle : _bundles) {
                    if (bundle instanceof FileResourceBundle &&
                        !((FileResourceBundle)bundle).sourceIsReady()) {
                        Log.warning("Bundle failed to initialize " + bundle + ".");
                    }
                    if (_obs != null) {
                        int pct = count*100/_bundles.size();
                        if (pct < 100) {
                            _obs.progress(pct, 1);
                        }
                    }
                    count++;
                }
                if (_obs != null) {
                    _obs.progress(100, 0);
                }

            } catch (Exception e) {
                if (_obs != null) {
                    _obs.initializationFailed(e);
                }
            }
        }

        protected List<ResourceBundle> _bundles;
        protected InitObserver _obs;
    }

    /** The classloader we use for classpath-based resource loading. */
    protected ClassLoader _loader;

    /** The directory that contains our resource bundles. */
    protected File _rdir;

    /** The prefix we prepend to resource paths before attempting to load them from the
     * classpath. */
    protected String _rootPath;

    /** Whether or not to unpack our resource bundles. */
    protected boolean _unpack;

    /** Our default resource set. */
    protected ResourceBundle[] _default = new ResourceBundle[0];

    /** A table of our resource sets. */
    protected HashMap _sets = new HashMap();

    /** Locale to search for locale-specific resources, if any. */
    protected String _localePrefix = null;

    /** The prefix of configuration entries that describe a resource set. */
    protected static final String RESOURCE_SET_PREFIX = "resource.set.";

    /** The name of the default resource set. */
    protected static final String DEFAULT_RESOURCE_SET = "default";
}
