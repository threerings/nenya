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

package com.threerings.cast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import java.awt.Point;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.util.LRUHashMap;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Throttle;
import com.samskivert.util.Tuple;

import com.samskivert.swing.RuntimeAdjust;

import com.threerings.util.DirectionCodes;

import com.threerings.media.image.Colorization;
import com.threerings.media.image.ImageManager;

import com.threerings.cast.CompositedActionFrames.ComponentFrames;
import com.threerings.cast.CompositedActionFrames.CompositedFramesKey;

import static com.threerings.cast.Log.log;

/**
 * The character manager provides facilities for constructing sprites that
 * are used to represent characters in a scene. It also handles the
 * compositing and caching of composited character animations.
 */
public class CharacterManager
    implements DirectionCodes
{
    /**
     * Sets the size of the cache used for composited animation frames. This must be called before
     * the CharacterManager is created.
     */
    public static void setCacheSize (int cacheKilobytes)
    {
        _runCacheSize = cacheKilobytes;
    }

    /**
     * Constructs the character manager.
     */
    public CharacterManager (ImageManager imgr, ComponentRepository crepo)
    {
        // keep these around
        _imgr = imgr;
        _crepo = crepo;

        // populate our actions table
        Iterator<ActionSequence> iter = crepo.enumerateActionSequences();
        while (iter.hasNext()) {
            ActionSequence action = iter.next();
            _actions.put(action.name, action);
        }

        // create a cache for our composited action frames
        log.debug("Creating action cache [size=" + _runCacheSize + "k].");
        _frameCache = new LRUHashMap<CompositedFramesKey, CompositedMultiFrameImage>(
                _runCacheSize * 1024, new LRUHashMap.ItemSizer<CompositedMultiFrameImage>() {
            public int computeSize (CompositedMultiFrameImage value) {
                return (int)value.getEstimatedMemoryUsage();
            }
        });
        _frameCache.setTracking(true); // TODO
    }

    /**
     * Returns the component repository being used by this manager.
     */
    public ComponentRepository getComponentRepository ()
    {
        return _crepo;
    }

    /**
     * Instructs the character manager to construct instances of this
     * derived class of {@link CharacterSprite} when creating new sprites.
     *
     * @exception IllegalArgumentException thrown if the supplied class
     * does not derive from {@link CharacterSprite}.
     */
    public void setCharacterClass (Class<? extends CharacterSprite> charClass)
    {
        // make a note of it
        _charClass = charClass;
    }

    /**
     * Instructs the character manager to use the provided cache for
     * composited action animations.
     */
    public void setActionCache (ActionCache cache)
    {
        _acache = cache;
    }

    /**
     * Returns a {@link CharacterSprite} representing the character
     * described by the given {@link CharacterDescriptor}, or
     * <code>null</code> if an error occurs.
     *
     * @param desc the character descriptor.
     */
    public CharacterSprite getCharacter (CharacterDescriptor desc)
    {
        return getCharacter(desc, _charClass);
    }

    /**
     * Returns a {@link CharacterSprite} representing the character
     * described by the given {@link CharacterDescriptor}, or
     * <code>null</code> if an error occurs.
     *
     * @param desc the character descriptor.
     * @param charClass the {@link CharacterSprite} derived class that
     * should be instantiated instead of the configured default (which is
     * set via {@link #setCharacterClass}).
     */
    public <T extends CharacterSprite> T getCharacter (CharacterDescriptor desc,
        Class<T> charClass)
    {
        try {
            T sprite = charClass.newInstance();
            sprite.init(desc, this);
            return sprite;

        } catch (Exception e) {
            log.warning("Failed to instantiate character sprite.", e);
            return null;
        }
    }

    /**
     * Obtains the composited animation frames for the specified action for a
     * character with the specified descriptor. The resulting composited
     * animation will be cached.
     *
     * @exception NoSuchComponentException thrown if any of the components in
     * the supplied descriptor do not exist.
     * @exception IllegalArgumentException thrown if any of the components
     * referenced in the descriptor do not support the specified action.
     */
    public ActionFrames getActionFrames (
        CharacterDescriptor descrip, String action)
        throws NoSuchComponentException
    {
        Tuple<CharacterDescriptor, String> key = new Tuple<CharacterDescriptor, String>(descrip, action);
        ActionFrames frames = _actionFrames.get(key);
        if (frames == null) {
            // this doesn't actually composite the images, but prepares an
            // object to be able to do so
            frames = createCompositeFrames(descrip, action);
            _actionFrames.put(key, frames);
        }

        // periodically report our frame image cache performance
        if (!_cacheStatThrottle.throttleOp()) {
            long size = getEstimatedCacheMemoryUsage();
            int[] eff = _frameCache.getTrackedEffectiveness();
            log.debug("CharacterManager LRU [mem=" + (size / 1024) + "k" +
                      ", size=" + _frameCache.size() + ", hits=" + eff[0] +
                      ", misses=" + eff[1] + "].");
        }

        return frames;
    }

    /**
     * Informs the character manager that the action sequence for the
     * given character descriptor is likely to be needed in the near
     * future and so any efforts that can be made to load it into the
     * action sequence cache in advance should be undertaken.
     *
     * <p> This will eventually be revamped to spiffily load action
     * sequences in the background.
     */
    public void resolveActionSequence (CharacterDescriptor desc, String action)
    {
        try {
            if (getActionFrames(desc, action) == null) {
                log.warning("Failed to resolve action sequence " +
                            "[desc=" + desc + ", action=" + action + "].");
            }

        } catch (NoSuchComponentException nsce) {
            log.warning("Failed to resolve action sequence " +
                        "[nsce=" + nsce + "].");
        }
    }

    /**
     * Returns the action sequence instance with the specified name or
     * null if no such sequence exists.
     */
    public ActionSequence getActionSequence (String action)
    {
        return _actions.get(action);
    }

    /**
     * Returns the estimated memory usage in bytes for all images
     * currently cached by the cached action frames.
     */
    protected long getEstimatedCacheMemoryUsage ()
    {
        long size = 0;
        Iterator<CompositedMultiFrameImage> iter = _frameCache.values().iterator();
        while (iter.hasNext()) {
            size += iter.next().getEstimatedMemoryUsage();
        }
        return size;
    }

    /**
     * Generates the composited animation frames for the specified action
     * for a character with the specified descriptor.
     *
     * @exception NoSuchComponentException thrown if any of the components
     * in the supplied descriptor do not exist.
     * @exception IllegalArgumentException thrown if any of the components
     * referenced in the descriptor do not support the specified action.
     */
    protected ActionFrames createCompositeFrames (
        CharacterDescriptor descrip, String action)
        throws NoSuchComponentException
    {
        int[] cids = descrip.getComponentIds();
        int ccount = cids.length;
        Colorization[][] zations = descrip.getColorizations();
        Point[] xlations = descrip.getTranslations();

        log.debug("Compositing action [action=" + action +
                  ", descrip=" + descrip + "].");

        // this will be used to construct any shadow layers
        HashMap<String, ArrayList<TranslatedComponent>> shadows = null;

        // maps components by class name for masks
        HashMap<String, ArrayList<TranslatedComponent>> ccomps =
            Maps.newHashMap();

        // create colorized versions of all of the source action frames
        ArrayList<ComponentFrames> sources = Lists.newArrayListWithCapacity(ccount);
        for (int ii = 0; ii < ccount; ii++) {
            ComponentFrames cframes = new ComponentFrames();
            sources.add(cframes);
            CharacterComponent ccomp = (cframes.ccomp = _crepo.getComponent(cids[ii]));

            // load up the main component images
            ActionFrames source = ccomp.getFrames(action, null);
            if (source == null) {
                String errmsg = "Cannot composite action frames; no such " +
                    "action for component [action=" + action +
                    ", desc=" + descrip + ", comp=" + ccomp + "]";
                throw new RuntimeException(errmsg);
            }
            source = (zations == null || zations[ii] == null) ?
                source : source.cloneColorized(zations[ii]);
            Point xlation = (xlations == null) ? null : xlations[ii];
            cframes.frames = (xlation == null) ?
                source : source.cloneTranslated(xlation.x, xlation.y);

            // store the component with its translation under its class for masking
            TranslatedComponent tcomp = new TranslatedComponent(ccomp, xlation);
            ArrayList<TranslatedComponent> tcomps = ccomps.get(ccomp.componentClass.name);
            if (tcomps == null) {
                ccomps.put(ccomp.componentClass.name, tcomps = Lists.newArrayList());
            }
            tcomps.add(tcomp);

            // if this component has a shadow, make a note of it
            if (ccomp.componentClass.isShadowed()) {
                if (shadows == null) {
                    shadows = Maps.newHashMap();
                }
                ArrayList<TranslatedComponent> shadlist = shadows.get(ccomp.componentClass.shadow);
                if (shadlist == null) {
                    shadows.put(ccomp.componentClass.shadow, shadlist = Lists.newArrayList());
                }
                shadlist.add(tcomp);
            }
        }

        // now create any necessary shadow layers
        if (shadows != null) {
            for (Map.Entry<String, ArrayList<TranslatedComponent>> entry : shadows.entrySet()) {
                ComponentFrames scf = compositeShadow(action, entry.getKey(), entry.getValue());
                if (scf != null) {
                    sources.add(scf);
                }
            }
        }

        // add any necessary masks
        for (ComponentFrames cframes : sources) {
            ArrayList<TranslatedComponent> mcomps = ccomps.get(cframes.ccomp.componentClass.mask);
            if (mcomps != null) {
                cframes.frames = compositeMask(action, cframes.ccomp, cframes.frames, mcomps);
            }
        }

        // use those to create an entity that will lazily composite things
        // together as they are needed
        ComponentFrames[] cfvec = sources.toArray(new ComponentFrames[sources.size()]);
        return new CompositedActionFrames(_imgr, _frameCache, action, cfvec);
    }

    protected ComponentFrames compositeShadow (
        String action, String sclass, ArrayList<TranslatedComponent> scomps)
    {
        final ComponentClass cclass = _crepo.getComponentClass(sclass);
        if (cclass == null) {
            log.warning("Components reference non-existent shadow layer " +
                        "class [sclass=" + sclass +
                        ", scomps=" + StringUtil.toString(scomps) + "].");
            return null;
        }

        ComponentFrames cframes = new ComponentFrames();
        // create a fake component for the shadow layer
        cframes.ccomp = new CharacterComponent(-1, "shadow", cclass, null);

        ArrayList<ComponentFrames> sources = Lists.newArrayList();
        for (TranslatedComponent scomp : scomps) {
            ComponentFrames source = new ComponentFrames();
            source.ccomp = scomp.ccomp;
            source.frames = scomp.getFrames(action, StandardActions.SHADOW_TYPE);
            if (source.frames == null) {
                // skip this shadow component
                continue;
            }
            sources.add(source);
        }

        // if we ended up with no shadow, no problem!
        if (sources.size() == 0) {
            return null;
        }

        // create custom action frames that use a special compositing
        // multi-frame image that does the necessary shadow magic
        ComponentFrames[] svec = sources.toArray(new ComponentFrames[sources.size()]);
        cframes.frames = new CompositedActionFrames(_imgr, _frameCache, action, svec) {
            @Override
            protected CompositedMultiFrameImage createFrames (int orient) {
                return new CompositedShadowImage(
                    _imgr, _sources, _action, orient, cclass.shadowAlpha);
            }
        };

        return cframes;
    }

    protected ActionFrames compositeMask (
        String action, CharacterComponent ccomp, ActionFrames cframes,
        ArrayList<TranslatedComponent> mcomps)
    {
        ArrayList<ComponentFrames> sources = Lists.newArrayList();
        sources.add(new ComponentFrames(ccomp, cframes));
        for (TranslatedComponent mcomp : mcomps) {
            ActionFrames mframes = mcomp.getFrames(action, StandardActions.CROP_TYPE);
            if (mframes != null) {
                sources.add(new ComponentFrames(mcomp.ccomp, mframes));
            }
        }
        if (sources.size() == 1) {
            return cframes;
        }
        ComponentFrames[] mvec = sources.toArray(new ComponentFrames[sources.size()]);
        return new CompositedActionFrames(_imgr, _frameCache, action, mvec) {
            @Override
            protected CompositedMultiFrameImage createFrames (int orient) {
                return new CompositedMaskedImage(_imgr, _sources, _action, orient);
            }
        };
    }

    /** Combines a component with an optional translation for shadowing or masking. */
    protected static class TranslatedComponent
    {
        public CharacterComponent ccomp;
        public Point xlation;

        public TranslatedComponent (CharacterComponent ccomp, Point xlation)
        {
            this.ccomp = ccomp;
            this.xlation = xlation;
        }

        public ActionFrames getFrames (String action, String type)
        {
            ActionFrames frames = ccomp.getFrames(action, type);
            return (frames == null || xlation == null) ?
                frames : frames.cloneTranslated(xlation.x, xlation.y);
        }
    }

    /** The image manager with whom we interact. */
    protected ImageManager _imgr;

    /** The component repository. */
    protected ComponentRepository _crepo;

    /** A table of our action sequences. */
    protected Map<String, ActionSequence> _actions = Maps.newHashMap();

    /** A table of composited action sequences (these don't reference the
     * actual image data directly and thus take up little memory). */
    protected Map<Tuple<CharacterDescriptor, String>, ActionFrames> _actionFrames =
        Maps.newHashMap();

    /** A cache of composited animation frames. */
    protected LRUHashMap<CompositedFramesKey, CompositedMultiFrameImage> _frameCache;

    /** The character class to be created. */
    protected Class<? extends CharacterSprite> _charClass = CharacterSprite.class;

    /** The action animation cache, if we have one. */
    protected ActionCache _acache;

    /** Throttle our cache status logging to once every 30 seconds. */
    protected Throttle _cacheStatThrottle = new Throttle(1, 30000L);

    /** Register our image cache size with the runtime adjustments
     * framework. */
    protected static RuntimeAdjust.IntAdjust _cacheSize =
        new RuntimeAdjust.IntAdjust(
            "Size (in kb of memory used) of the character manager LRU " +
            "action cache [requires restart]", "narya.cast.action_cache_size",
            CastPrefs.config, 32768);

    /**
     * Cache size to be used in this run.  Adjusted by setCacheSize without affecting
     * the stored value.
     */
    protected static int _runCacheSize = _cacheSize.getValue();
}
