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

package com.threerings.cast.bundle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import java.io.IOException;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.Tuple;

import com.threerings.util.DirectionCodes;

import com.threerings.resource.FileResourceBundle;
import com.threerings.resource.ResourceBundle;
import com.threerings.resource.ResourceManager;

import com.threerings.media.image.BufferedMirage;
import com.threerings.media.image.Colorization;
import com.threerings.media.image.ImageDataProvider;
import com.threerings.media.image.ImageManager;
import com.threerings.media.image.Mirage;
import com.threerings.media.tile.IMImageProvider;
import com.threerings.media.tile.Tile;
import com.threerings.media.tile.TileSet;
import com.threerings.media.tile.TrimmedTile;

import com.threerings.cast.ActionFrames;
import com.threerings.cast.ActionSequence;
import com.threerings.cast.CharacterComponent;
import com.threerings.cast.ComponentClass;
import com.threerings.cast.ComponentRepository;
import com.threerings.cast.FrameProvider;
import com.threerings.cast.NoSuchComponentException;
import com.threerings.cast.StandardActions;
import com.threerings.cast.TrimmedMultiFrameImage;

import static com.threerings.cast.Log.log;

/**
 * A component repository implementation that obtains information from resource bundles.
 *
 * @see ResourceManager
 */
public class BundledComponentRepository
    implements DirectionCodes, ComponentRepository
{
    /**
     * Constructs a repository which will obtain its resource set from the supplied resource
     * manager.
     *
     * @param rmgr the resource manager from which to obtain our resource set.
     * @param imgr the image manager that we'll use to decode and cache images.
     * @param name the name of the resource set from which we will be loading our component data.
     *
     * @exception IOException thrown if an I/O error occurs while reading our metadata from the
     * resource bundles.
     */
    public BundledComponentRepository (
        ResourceManager rmgr, ImageManager imgr, String name)
        throws IOException
    {
        // keep this guy around
        _imgr = imgr;

        // first we obtain the resource set from whence will come our bundles
        ResourceBundle[] rbundles = rmgr.getResourceSet(name);
        if (rbundles == null) {
            // Couldn't find a bundle with that name, so just make empty maps for safe enumerating
            _actions = Maps.newHashMap();
            _classes = Maps.newHashMap();
            return;
        }

        // look for our metadata info in each of the bundles
        try {
            for (ResourceBundle rbundle : rbundles) {
                if (_actions == null) {
                    @SuppressWarnings("unchecked") Map<String, ActionSequence> amap =
                        (Map<String, ActionSequence>)BundleUtil.loadObject(
                            rbundle, BundleUtil.ACTIONS_PATH, true);
                    _actions = amap;
                }
                if (_actionSets == null) {
                    @SuppressWarnings("unchecked") Map<String, TileSet> asets =
                        (Map<String, TileSet>)BundleUtil.loadObject(
                            rbundle, BundleUtil.ACTION_SETS_PATH, true);
                    _actionSets = asets;
                }
                if (_classes == null) {
                    @SuppressWarnings("unchecked") Map<String, ComponentClass> cmap =
                        (Map<String, ComponentClass>)BundleUtil.loadObject(
                            rbundle, BundleUtil.CLASSES_PATH, true);
                    _classes = cmap;
                }
            }

            // now go back and load up all of the component information
            for (ResourceBundle rbundle : rbundles) {
                @SuppressWarnings("unchecked") IntMap<Tuple<String, String>> comps =
                    (IntMap<Tuple<String, String>>)BundleUtil.loadObject(
                        rbundle, BundleUtil.COMPONENTS_PATH, true);
                if (comps == null) {
                    continue;
                }

                // create a frame provider for this bundle
                FrameProvider fprov = new ResourceBundleProvider(_imgr, rbundle);

                // now create char. component instances for each component in the serialized table
                Iterator<Integer> iter = comps.keySet().iterator();
                while (iter.hasNext()) {
                    int componentId = iter.next().intValue();
                    Tuple<String, String> info = comps.get(componentId);
                    createComponent(componentId, info.left, info.right, fprov);
                }
            }

        } catch (ClassNotFoundException cnfe) {
            throw (IOException) new IOException(
                "Internal error unserializing metadata").initCause(cnfe);
        }

        // if we failed to load our classes or actions, create empty hashtables so that we can
        // safely enumerate our emptiness
        if (_actions == null) {
            _actions = Maps.newHashMap();
        }
        if (_classes == null) {
            _classes = Maps.newHashMap();
        }
    }

    /**
     * Configures the bundled component repository to wipe any bundles that report certain kinds of
     * failure. In the event that an unpacked bundle becomes corrupt, this is useful in that it
     * will force the bundle to be unpacked on the next application invocation, potentially
     * remedying the problem of a corrupt unpacking.
     */
    public void setWipeOnFailure (boolean wipeOnFailure)
    {
        _wipeOnFailure = wipeOnFailure;
    }

    // documentation inherited
    public CharacterComponent getComponent (int componentId)
        throws NoSuchComponentException
    {
        CharacterComponent component = _components.get(componentId);
        if (component == null) {
            throw new NoSuchComponentException(componentId);
        }
        return component;
    }

    // documentation inherited
    public CharacterComponent getComponent (String className, String compName)
        throws NoSuchComponentException
    {
        // look up the list for that class
        ArrayList<CharacterComponent> comps = _classComps.get(className);
        if (comps != null) {
            // scan the list for the named component
            int ccount = comps.size();
            for (int ii = 0; ii < ccount; ii++) {
                CharacterComponent comp = comps.get(ii);
                if (comp.name.equals(compName)) {
                    return comp;
                }
            }
        }
        throw new NoSuchComponentException(className, compName);
    }

    // documentation inherited
    public ComponentClass getComponentClass (String className)
    {
        return _classes.get(className);
    }

    // documentation inherited
    public Iterator<ComponentClass> enumerateComponentClasses ()
    {
        return _classes.values().iterator();
    }

    // documentation inherited
    public Iterator<ActionSequence> enumerateActionSequences ()
    {
        return _actions.values().iterator();
    }

    // documentation inherited
    public Iterator<Integer> enumerateComponentIds (final ComponentClass compClass)
    {
        Predicate<Map.Entry<Integer,CharacterComponent>> pred =
            new Predicate<Map.Entry<Integer,CharacterComponent>>() {
                public boolean apply (Map.Entry<Integer,CharacterComponent> entry) {
                    return entry.getValue().componentClass.equals(compClass);
                }
            };
        Function<Map.Entry<Integer,CharacterComponent>,Integer> func =
            new Function<Map.Entry<Integer,CharacterComponent>,Integer>() {
                public Integer apply (Map.Entry<Integer,CharacterComponent> entry) {
                    return entry.getKey();
                }
            };
        return Iterators.transform(Iterators.filter(_components.entrySet().iterator(), pred), func);
    }

    /**
     * Creates a component and inserts it into the component table.
     */
    protected void createComponent (
        int componentId, String cclass, String cname, FrameProvider fprov)
    {
        // look up the component class information
        ComponentClass clazz = _classes.get(cclass);
        if (clazz == null) {
            log.warning("Non-existent component class",
                "class", cclass, "name", cname, "id", componentId);
            return;
        }

        // create the component
        CharacterComponent component = new CharacterComponent(componentId, cname, clazz, fprov);

        // stick it into the appropriate tables
        _components.put(componentId, component);

        // we have a hash of lists for mapping components by class/name
        ArrayList<CharacterComponent> comps = _classComps.get(cclass);
        if (comps == null) {
            comps = Lists.newArrayList();
            _classComps.put(cclass, comps);
        }
        if (!comps.contains(component)) {
            comps.add(component);
        } else {
            log.info("Requested to register the same component twice?", "comp", component);
        }
    }

    protected TileSetFrameImage createTileSetFrameImage (TileSet aset, ActionSequence actseq)
    {
        return new TileSetFrameImage(aset, actseq);
    }

    /**
     * Instances of these provide images to our component action tilesets and frames to our
     * components.
     */
    protected class ResourceBundleProvider extends IMImageProvider
        implements ImageDataProvider, FrameProvider
    {
        /**
         * Constructs an instance that will obtain image data from the specified resource bundle.
         */
        public ResourceBundleProvider (ImageManager imgr, ResourceBundle bundle) {
            super(imgr, (String)null);
            _dprov = this;
            _bundle = bundle;
        }

        // from interface ImageDataProvider
        public String getIdent () {
            return "bcr:" + _bundle.getIdent();
        }

        // from interface ImageDataProvider
        public BufferedImage loadImage (String path) throws IOException {
            return _bundle.getImageResource(path, true);
        }

        // from interface FrameProvider
        public ActionFrames getFrames (CharacterComponent component, String action, String type) {
            // obtain the action sequence definition for this action
            ActionSequence actseq = _actions.get(action);
            if (actseq == null) {
                log.warning("Missing action sequence definition",
                    "action", action, "component", component);
                return null;
            }

            // determine our image path name
            String imgpath = action, dimgpath = ActionSequence.DEFAULT_SEQUENCE;
            if (type != null) {
                imgpath += "_" + type;
                dimgpath += "_" + type;
            }

            String root = component.componentClass.name + "/" + component.name + "/";
            String cpath = root + imgpath + BundleUtil.TILESET_EXTENSION;
            String dpath = root + dimgpath + BundleUtil.TILESET_EXTENSION;

            // look to see if this tileset is already cached (as the custom action or the default
            // action)
            TileSet aset = _setcache.get(cpath);
            if (aset == null) {
                aset = _setcache.get(dpath);
                if (aset != null) {
                    // save ourselves a lookup next time
                    _setcache.put(cpath, aset);
                }
            }

            try {
                // then try loading up a tileset customized for this action
                if (aset == null) {
                    aset = (TileSet)BundleUtil.loadObject(_bundle, cpath, false);
                }

                // if that failed, try loading the default tileset
                if (aset == null) {
                    aset = (TileSet)BundleUtil.loadObject(_bundle, dpath, false);
                    _setcache.put(dpath, aset);
                }

                // if that failed too, we're hosed
                if (aset == null) {
                    // if this is a shadow or crop image, no need to freak out as they are optional
                    if (!StandardActions.CROP_TYPE.equals(type) &&
                        !StandardActions.SHADOW_TYPE.equals(type)) {
                        log.warning("Unable to locate tileset for action '" + imgpath + "' " +
                                    component + ".");
                        if (_wipeOnFailure && _bundle instanceof FileResourceBundle) {
                            ((FileResourceBundle)_bundle).wipeBundle(false);
                        }
                    }
                    return null;
                }

                aset.setImageProvider(this);
                _setcache.put(cpath, aset);
                return createTileSetFrameImage(aset, actseq);

            } catch (Exception e) {
                log.warning("Error loading tset for action '" + imgpath + "' " + component + ".",
                    e);
                return null;
            }
        }

        // from interface FrameProvider
        public String getFramePath (CharacterComponent component, String action, String type,
            Set<String> existentPaths) {
            String actionPath = makePath(component, action, type);
            if(!existentPaths.contains(actionPath)) {
                return makePath(component, ActionSequence.DEFAULT_SEQUENCE, type);
            }
            return actionPath;
        }

        protected String makePath(CharacterComponent component, String action, String type) {
            String imgpath = action;
            if (type != null) {
                imgpath += "_" + type;
            }
            String root = component.componentClass.name + "/" + component.name + "/";
            return _bundle.getIdent() + root + imgpath + BundleUtil.IMAGE_EXTENSION;
        }

        @Override
        public Mirage getTileImage (String path, Rectangle bounds, Colorization[] zations) {
            // we don't need our images prepared for screen rendering
            BufferedImage src = _imgr.getImage(getImageKey(path), zations);
            float percentageOfDataBuffer = 1;
            if (bounds != null) {
                percentageOfDataBuffer =
                    (bounds.height * bounds.width) / (float)(src.getHeight() * src.getWidth());
                src = src.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);
            }
            return new BufferedMirage(src, percentageOfDataBuffer);
        }

        /** The resource bundle from which we obtain image data. */
        protected ResourceBundle _bundle;

        /** Cache of tilesets loaded from our bundle. */
        protected Map<String, TileSet> _setcache = Maps.newHashMap();
    }

    /**
     * Used to provide multiframe images using data obtained from a tileset.
     */
    public static class TileSetFrameImage implements ActionFrames
    {
        /**
         * Constructs a tileset frame image with the specified tileset and for the specified
         * orientation.
         */
        public TileSetFrameImage (TileSet set, ActionSequence actseq) {
            this(set, actseq, 0, 0);
        }

        /**
         * Constructs a tileset frame image with the specified tileset and for the specified
         * orientation, with an optional translation.
         */
        public TileSetFrameImage (TileSet set, ActionSequence actseq, int dx, int dy) {
            _set = set;
            _actseq = actseq;
            _dx = dx;
            _dy = dy;

            // compute these now to avoid pointless recomputation later
            _ocount = actseq.orients.length;
            _fcount = set.getTileCount() / _ocount;

            // create our mapping from orientation to animation sequence index
            for (int ii = 0; ii < _ocount; ii++) {
                _orients.put(actseq.orients[ii], ii);
            }
        }

        // documentation inherited from interface
        public int getOrientationCount () {
            return _ocount;
        }

        // documentation inherited from interface
        public TrimmedMultiFrameImage getFrames (final int orient) {
            return new TrimmedMultiFrameImage() {
                public int getFrameCount () {
                    return _fcount;
                }

                public int getWidth (int index) {
                    return _set.getTile(getTileIndex(orient, index)).getWidth();
                }

                public int getHeight (int index) {
                    return _set.getTile(getTileIndex(orient, index)).getHeight();
                }

                public void paintFrame (Graphics2D g, int index, int x, int y) {
                    paintTile(g, orient, index, x, y);
                }

                public boolean hitTest (int index, int x, int y) {
                    return _set.getTile(getTileIndex(orient, index)).hitTest(x + _dx, y + _dy);
                }

                public void getTrimmedBounds (int index, Rectangle bounds) {
                    TileSetFrameImage.this.getTrimmedBounds(orient, index, bounds);
                }
            };
        }

        protected void paintTile(Graphics2D g, int orient, int index, int x, int y) {
            _set.getTile(getTileIndex(orient, index)).paint(g, x + _dx, y + _dy);
        }

        public void getTrimmedBounds (int orient, int index, Rectangle bounds) {
            Tile tile = getTile(orient, index);
            if (tile instanceof TrimmedTile) {
                ((TrimmedTile)tile).getTrimmedBounds(bounds);
            } else {
                bounds.setBounds(0, 0, tile.getWidth(), tile.getHeight());
            }
            bounds.translate(_dx, _dy);
        }

        // documentation inherited from interface
        public int getXOrigin (int orient, int index) {
            return _actseq.origin.x;
        }

        // documentation inherited from interface
        public int getYOrigin (int orient, int index) {
            return _actseq.origin.y;
        }

        // documentation inherited from interface
        public ActionFrames cloneColorized (Colorization[] zations) {
            return new TileSetFrameImage(_set.clone(zations), _actseq);
        }

        // documentation inherited from interface
        public ActionFrames cloneTranslated (int dx, int dy) {
            return new TileSetFrameImage(_set, _actseq, dx, dy);
        }

        protected int getTileIndex (int orient, int index) {
            return _orients.get(orient) * _fcount + index;
        }

        public Tile getTile (int orient, int index) {
            return _set.getTile(getTileIndex(orient, index));
        }

        public Mirage getTileMirage (int orient, int index) {
            return _set.getTileMirage(getTileIndex(orient, index));
        }

        /** The tileset from which we obtain our frame images. */
        protected TileSet _set;

        /** The action sequence for which we're providing frame images. */
        protected ActionSequence _actseq;

        /** A translation to apply to the images. */
        protected int _dx, _dy;

        /** Frame and orientation counts. */
        protected int _fcount, _ocount;

        /** A mapping from orientation code to animation sequence index. */
        protected IntIntMap _orients = new IntIntMap();

    }

    /** We use the image manager to decode and cache images. */
    protected ImageManager _imgr;

    /** A table of action sequences. */
    protected Map<String, ActionSequence> _actions;

    /** A table of action sequence tilesets. */
    protected Map<String, TileSet> _actionSets;

    /** A table of component classes. */
    protected Map<String, ComponentClass> _classes;

    /** A table of component lists indexed on classname. */
    protected Map<String, ArrayList<CharacterComponent>> _classComps = Maps.newHashMap();

    /** The component table. */
    protected IntMap<CharacterComponent> _components = IntMaps.newHashIntMap();

    /** Whether or not we wipe our bundles on any failure. */
    protected boolean _wipeOnFailure;
}
