//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
// http://code.google.com/p/nenya/
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

package com.threerings.jme.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.FloatBuffer;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.jme.bounding.BoundingVolume;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.Renderer;
import com.jme.scene.Controller;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.OutputCapsule;
import com.jme.util.export.Savable;
import com.jme.util.export.binary.BinaryExporter;
import com.jme.util.export.binary.BinaryImporter;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.ObserverList;
import com.samskivert.util.PropertiesUtil;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.StringUtil;

import com.threerings.jme.util.SpatialVisitor;

import static com.threerings.jme.Log.log;

/**
 * The base node for models.
 */
public class Model extends ModelNode
{
    /** The supported types of animation in decreasing order of complexity. */
    public enum AnimationMode {
        SKIN, MORPH, FLIPBOOK
    };

    /** Lets listeners know when animations are completed (which only happens
     * for non-repeating animations) or cancelled. */
    public interface AnimationObserver
    {
        /**
         * Called when an animation has started.
         *
         * @return true to remain on the observer list, false to remove self
         */
        public boolean animationStarted (Model model, String anim);

        /**
         * Called when a non-repeating animation has finished.
         *
         * @return true to remain on the observer list, false to remove self
         */
        public boolean animationCompleted (Model model, String anim);

        /**
         * Called when an animation has been cancelled.
         *
         * @return true to remain on the observer list, false to remove self
         */
        public boolean animationCancelled (Model model, String anim);
    }

    /** An animation for the model. */
    public static class Animation
        implements Savable
    {
        /** The rate of the animation in frames per second. */
        public int frameRate;

        /** The animation repeat type ({@link Controller#RT_CLAMP},
         * {@link Controller#RT_CYCLE}, or {@link Controller#RT_WRAP}). */
        public int repeatType;

        /** Any nodes visible that never move within the model. */
        public Spatial[] staticTargets;

        /** The transformation targets of the animation. */
        public Spatial[] transformTargets;

        /** The animation transforms (one transform per target per frame). */
        public transient Transform[][] transforms;

        /** Uniquely identifies this animation within the model. */
        public transient int animId;

        /** For each frame, whether the frame has been stored in meshes. */
        public transient boolean[] stored;

        /**
         * Returns this animation's duration in seconds.
         */
        public float getDuration ()
        {
            return (float)transforms.length / frameRate;
        }

        /**
         * Rebinds this animation for a prototype instance.
         *
         * @param pnodes a mapping from prototype nodes to instance nodes
         */
        public Animation rebind (Map<ModelSpatial, ModelSpatial> pnodes)
        {
            Animation anim = new Animation();
            anim.frameRate = frameRate;
            anim.repeatType = repeatType;
            anim.staticTargets = rebind(staticTargets, pnodes);
            anim.transformTargets = rebind(transformTargets, pnodes);
            anim.transforms = transforms;
            anim.animId = animId;
            anim.stored = stored;
            return anim;
        }

        /**
         * Applies the transforms for a frame of this animation.
         */
        public void applyFrame (int fidx)
        {
            Transform[] xforms = transforms[fidx];
            for (int ii = 0; ii < transformTargets.length; ii++) {
                xforms[ii].apply(transformTargets[ii]);
            }
        }

        /**
         * Blends the transforms between two frames of this animation.
         */
        public void blendFrames (int fidx, int nidx, float alpha)
        {
            Transform[] xforms = transforms[fidx], nxforms = transforms[nidx];
            for (int ii = 0; ii < transformTargets.length; ii++) {
                xforms[ii].blend(nxforms[ii], alpha, transformTargets[ii]);
            }
        }

        // documentation inherited
        public Class<?> getClassTag ()
        {
            return getClass();
        }

        // documentation inherited
        public void read (JMEImporter im)
            throws IOException
        {
            InputCapsule capsule = im.getCapsule(this);
            frameRate = capsule.readInt("frameRate", 0);
            repeatType = capsule.readInt("repeatType", Controller.RT_CLAMP);
            staticTargets = ArrayUtil.copy(capsule.readSavableArray(
                "staticTargets", null), new Spatial[0]);
            transformTargets = ArrayUtil.copy(capsule.readSavableArray(
                "transformTargets", null), new Spatial[0]);
            FloatBuffer pxforms = capsule.readFloatBuffer("transforms", null);
            transforms = new Transform[pxforms.capacity() /
                Transform.PACKED_SIZE / transformTargets.length][];
            for (int ii = 0; ii < transforms.length; ii++) {
                Transform[] frame = transforms[ii] =
                    new Transform[transformTargets.length];
                for (int jj = 0; jj < frame.length; jj++) {
                    frame[jj] = new Transform(pxforms);
                }
            }
        }

        // documentation inherited
        public void write (JMEExporter ex)
            throws IOException
        {
            OutputCapsule capsule = ex.getCapsule(this);
            capsule.write(frameRate, "frameRate", 0);
            capsule.write(repeatType, "repeatType", Controller.RT_CLAMP);
            capsule.write(staticTargets, "staticTargets", null);
            capsule.write(transformTargets, "transformTargets", null);
            FloatBuffer pxforms = FloatBuffer.allocate(
                transforms.length * transformTargets.length * Transform.PACKED_SIZE);
            for (Transform[] frame : transforms) {
                for (Transform xform : frame) {
                    xform.writeToBuffer(pxforms);
                }
            }
            pxforms.rewind();
            capsule.write(pxforms, "transforms", null);
        }

        protected Spatial[] rebind (Spatial[] targets, Map<ModelSpatial, ModelSpatial> pnodes)
        {
            Spatial[] ntargets = new Spatial[targets.length];
            for (int ii = 0; ii < targets.length; ii++) {
                ntargets[ii] = (Spatial)pnodes.get(targets[ii]);
            }
            return ntargets;
        }

        private static final long serialVersionUID = 1;
    }

    /** A frame element that manipulates the target's transform. */
    public static final class Transform
    {
        /** The number of floats required to store a packed transform. */
        public static final int PACKED_SIZE = 3 + 4 + 3;

        public Transform (
            Vector3f translation, Quaternion rotation, Vector3f scale)
        {
            _translation = translation;
            _rotation = rotation;
            _scale = scale;
        }

        public Transform (FloatBuffer buf)
        {
            _translation = new Vector3f(buf.get(), buf.get(), buf.get());
            _rotation = new Quaternion(buf.get(), buf.get(), buf.get(),
                buf.get());
            _scale = new Vector3f(buf.get(), buf.get(), buf.get());
        }

        public void apply (Spatial target)
        {
            target.getLocalTranslation().set(_translation);
            target.getLocalRotation().set(_rotation);
            target.getLocalScale().set(_scale);
        }

        /**
         * Blends between this transform and the next, applying the result to
         * the given target.
         *
         * @param alpha the blend factor: 0.0 for entirely this frame, 1.0 for
         * entirely the next
         */
        public void blend (Transform next, float alpha, Spatial target)
        {
            target.getLocalTranslation().interpolate(_translation,
                next._translation, alpha);
            target.getLocalRotation().slerp(_rotation, next._rotation, alpha);
            target.getLocalScale().interpolate(_scale, next._scale, alpha);
        }

        /**
         * Writes this transform to the current position in the supplied
         * buffer.
         */
        public void writeToBuffer (FloatBuffer buf)
        {
            buf.put(_translation.x);
            buf.put(_translation.y);
            buf.put(_translation.z);

            buf.put(_rotation.x);
            buf.put(_rotation.y);
            buf.put(_rotation.z);
            buf.put(_rotation.w);

            buf.put(_scale.x);
            buf.put(_scale.y);
            buf.put(_scale.z);
        }

        /** The transform at this frame. */
        protected Vector3f _translation, _scale;
        protected Quaternion _rotation;
    }

    /** Customized clone creator for models. */
    public static class CloneCreator
    {
        /** A shared seed used to select textures consistently. */
        public int random;

        /** Maps original objects to their copies. */
        public Map<ModelSpatial, ModelSpatial> originalToCopy = Maps.newHashMap();

        public CloneCreator (Model toCopy)
        {
            _toCopy = toCopy;
            addProperty("vertices");
            addProperty("colors");
            addProperty("normals");
            addProperty("texcoords");
            addProperty("vboinfo");
            addProperty("indices");
            addProperty("obbtree");
            addProperty("displaylistid");
            addProperty("bound");
        }

        /**
         * Sets the named property.
         */
        public void addProperty (String name)
        {
            _properties.add(name);
        }

        /**
         * Clears the named property.
         */
        public void removeProperty (String name)
        {
            _properties.remove(name);
        }

        /**
         * Checks whether the named property has been set.
         */
        public boolean isSet (String name)
        {
            return _properties.contains(name);
        }

        /**
         * Creates a new copy of the target model.
         */
        public Model createCopy ()
        {
            random = RandomUtil.getInt(Integer.MAX_VALUE);
            Model copy = (Model)_toCopy.putClone(null, this);
            originalToCopy.clear(); // make sure no references remain
            return copy;
        }

        /** The model to copy. */
        protected Model _toCopy;

        /** The set of added properties. */
        protected HashSet<String> _properties = Sets.newHashSet();
    }

    /**
     * Attempts to read a model from the specified file.
     */
    public static Model readFromFile (File file)
        throws IOException
    {
        // read the serialized model and its children
        FileInputStream fis = new FileInputStream(file);
        Model model = (Model)BinaryImporter.getInstance().load(fis);
        fis.close();

        // initialize the model as a prototype
        model.initPrototype();

        return model;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public Model ()
    {
    }

    /**
     * Standard constructor.
     */
    public Model (String name, Properties props)
    {
        super(name);
        _props = props;
    }

    /**
     * Returns a reference to the properties of the model.
     */
    public Properties getProperties ()
    {
        return _props;
    }

    /**
     * Initializes this model as prototype.  Only necessary when the prototype
     * was not loaded through {@link #readFromFile}.
     */
    public void initPrototype ()
    {
        // initialize shared transient animation state
        if (_anims != null) {
            int nextId = 1;
            for (Animation anim : _anims.values()) {
                anim.animId = nextId++;
                anim.stored = new boolean[anim.transforms.length];
            }
        }
        setReferenceTransforms();
        cullInvisibleNodes();
        initInstance();
    }

    /**
     * Returns the names of the model's variant configurations.
     */
    public String[] getVariantNames ()
    {
        return StringUtil.parseStringArray(_props.getProperty("variants", ""));
    }

    /**
     * Adds an animation to the model's library.  This should only be called by
     * the model compiler.
     */
    public void addAnimation (String name, Animation anim)
    {
        if (_anims == null) {
            _anims = Maps.newHashMap();
        }
        _anims.put(name, anim);

        // store the original transforms
        Transform[] oxforms = new Transform[anim.transformTargets.length];
        for (int ii = 0; ii < anim.transformTargets.length; ii++) {
            Spatial target = anim.transformTargets[ii];
            oxforms[ii] = new Transform(
                new Vector3f(target.getLocalTranslation()),
                new Quaternion(target.getLocalRotation()),
                new Vector3f(target.getLocalScale()));
        }

        // run through every frame of the animation, expanding the bounding
        // volumes of any deformable meshes
        for (int ii = 0; ii < anim.transforms.length; ii++) {
            for (int jj = 0; jj < anim.transforms[ii].length; jj++) {
                anim.transforms[ii][jj].apply(anim.transformTargets[jj]);
            }
            updateWorldData(0f);
            expandModelBounds();
        }

        // restore the original transforms
        for (int ii = 0; ii < anim.transformTargets.length; ii++) {
            oxforms[ii].apply(anim.transformTargets[ii]);
        }
        updateWorldData(0f);
    }

    /**
     * Sets the animation mode to use for this model.  This should be set
     * on the prototype before any animations are started or any instances
     * are created.
     */
    public void setAnimationMode (AnimationMode mode)
    {
        _animMode = mode;
    }

    /**
     * Returns the animation mode configured for this model.
     */
    public AnimationMode getAnimationMode ()
    {
        return _animMode;
    }

    /**
     * Returns the names of the model's animations.
     */
    public String[] getAnimationNames ()
    {
        if (_prototype != null) {
            return _prototype.getAnimationNames();
        }
        return (_anims == null) ? new String[0] :
            _anims.keySet().toArray(new String[_anims.size()]);
    }

    /**
     * Checks whether the unit has an animation with the given name.
     */
    public boolean hasAnimation (String name)
    {
        if (_prototype != null) {
            return _prototype.hasAnimation(name);
        }
        return (_anims == null) ? false : _anims.containsKey(name);
    }

    /**
     * Starts the named animation.
     *
     * @return the duration of the started animation (for looping animations,
     * the duration of one cycle), or -1 if the animation was not found
     */
    public float startAnimation (String name)
    {
        return startAnimation (name, 0, +1);
    }

    /**
     * Starts the named animation.
     *
     * @param fidx the frame to start with
     * @param fdir the direction to go (+1 forward, -1 backward)
     *
     * @return the duration of the started animation (for looping animations,
     * the duration of one cycle), or -1 if the animation was not found
     */
    public float startAnimation (String name, int fidx, int fdir)
    {
        Animation anim = getAnimation(name);
        if (anim == null) {
            return -1f;
        }
        if (_anim != null) {
            _animObservers.apply(new AnimCancelledOp(_animName));
        }

        // first cull all model nodes, then re-activate the ones in the target lists
        cullModelNodes();
        for (Spatial target : anim.staticTargets) {
            ((ModelNode)target).updateCullMode();
        }
        for (Spatial target : anim.transformTargets) {
            ((ModelNode)target).updateCullMode();
        }

        _paused = false;
        _anim = anim;
        _animName = name;
        _fidx = fidx;
        _nidx = fidx;
        _fdir = fdir;
        _elapsed = 0f;
        advanceFrameCounter();
        _animObservers.apply(new AnimStartedOp(_animName));
        return anim.getDuration() / _animSpeed;
    }

    /**
     * Fast-forwards the current animation by the given number of seconds.
     */
    public void fastForwardAnimation (float time)
    {
        updateAnimation(time);
    }

    /**
     * Gets a reference to the animation with the given name.
     */
    public Animation getAnimation (String name)
    {
        if (_anims == null) {
            return null;
        }
        Animation anim = _anims.get(name);
        if (anim != null) {
            return anim;
        }
        if (_prototype != null) {
            Animation panim = _prototype._anims.get(name);
            if (panim != null) {
                _anims.put(name, anim = panim.rebind(_pnodes));
                return anim;
            }
        }
        log.warning("Requested unknown animation [name=" + name + "].");
        return null;
    }

    /**
     * Returns a reference to the currently running animation, or
     * <code>null</code> if no animation is running.
     */
    public Animation getAnimation ()
    {
        return _anim;
    }

    /**
     * Stops the currently running animation.
     */
    public void stopAnimation ()
    {
        if (_anim == null) {
            return;
        }
        if (_outside) {
            // make sure the meshes are in the right places when we come back into view
            updateMeshes();
        }
        _paused = false;
        _anim = null;
        _animObservers.apply(new AnimCancelledOp(_animName));
    }

    /**
     * Sets the pause state of the animation.
     */
    public void pauseAnimation (boolean pause)
    {
        _paused = pause;
    }

    /**
     * Returns the pause state of the animation.
     */
    public boolean isAnimationPaused ()
    {
        return _paused;
    }

    /**
     * Causes the animation to start running in reverse.
     */
    public void reverseAnimation ()
    {
        _fdir *= -1;
    }

    /**
     * Sets the animation speed, which acts as a multiplier for the frame rate
     * of each animation.
     */
    public void setAnimationSpeed (float speed)
    {
        _animSpeed = speed;
    }

    /**
     * Returns the currently configured animation speed.
     */
    public float getAnimationSpeed ()
    {
        return _animSpeed;
    }

    /**
     * Adds an animation observer.
     */
    public void addAnimationObserver (AnimationObserver obs)
    {
        _animObservers.add(obs);
    }

    /**
     * Removes an animation observer.
     */
    public void removeAnimationObserver (AnimationObserver obs)
    {
        _animObservers.remove(obs);
    }

    /**
     * Returns a reference to the node that contains this model's emissions
     * (in world space, so the emissions do not move with the model).  This
     * node is created and added when this method is first called.
     */
    public Node getEmissionNode ()
    {
        if (_emissionNode == null) {
            attachChild(_emissionNode = new Node("emissions") {
                public void updateWorldVectors () {
                    worldTranslation.set(localTranslation);
                    worldRotation.set(localRotation);
                    worldScale.set(localScale);
                }
            });
        }
        return _emissionNode;
    }

    /**
     * Writes this model out to a file.
     */
    public void writeToFile (File file)
        throws IOException
    {
        // start out by writing this node and its children
        FileOutputStream fos = new FileOutputStream(file);
        BinaryExporter.getInstance().save(this, fos);
        fos.close();
    }

    @Override
    public void read (JMEImporter im)
        throws IOException
    {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        String[] propNames = capsule.readStringArray("propNames", null),
            propValues = capsule.readStringArray("propValues", null);
        _props = new Properties();
        for (int ii = 0; ii < propNames.length; ii++) {
            _props.setProperty(propNames[ii], propValues[ii]);
        }
        String[] animNames = capsule.readStringArray("animNames", null);
        if (animNames != null) {
            Savable[] animValues = capsule.readSavableArray(
                "animValues", null);
            _anims = Maps.newHashMap();
            for (int ii = 0; ii < animNames.length; ii++) {
                _anims.put(animNames[ii], (Animation)animValues[ii]);
            }
        } else {
            _anims = null;
        }
        List<?> controllers = capsule.readSavableArrayList("controllers", null);
        if (controllers != null) {
            for (Object ctrl : controllers) {
                addController((Controller)ctrl);
            }
        }
    }

    @Override
    public void write (JMEExporter ex)
        throws IOException
    {
        // don't serialize the emission node; it contains transient geometry
        // created by controllers
        if (_emissionNode != null) {
            detachChild(_emissionNode);
        }
        super.write(ex);
        if (_emissionNode != null) {
            attachChild(_emissionNode);
        }
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(_props.keySet().toArray(new String[_props.size()]),
            "propNames", null);
        capsule.write(_props.values().toArray(new String[_props.size()]),
            "propValues", null);
        if (_anims != null) {
            capsule.write(_anims.keySet().toArray(
                new String[_anims.size()]), "animNames", null);
            capsule.write(_anims.values().toArray(
                new Animation[_anims.size()]), "animValues", null);
        }
        capsule.writeSavableArrayList(getControllers(), "controllers", null);
    }

    @Override
    public void resolveTextures (TextureProvider tprov)
    {
        super.resolveTextures(tprov);
        for (Object ctrl : getControllers()) {
            if (ctrl instanceof ModelController) {
                ((ModelController)ctrl).resolveTextures(tprov);
            }
        }
    }

    /**
     * Creates a new prototype using the given variant configuration.
     * Use {@link #createInstance} on the returned prototype to create
     * additional instances of the variant.
     */
    public Model createPrototype (String variant)
    {
        if (_prototype != null) {
            return _prototype.createPrototype(variant);
        }
        // create an instance and rebind all animations
        final Model prototype = createInstance();
        if (_anims != null) {
            for (Map.Entry<String, Animation> entry : _anims.entrySet()) {
                prototype._anims.put(entry.getKey(),
                    entry.getValue().rebind(prototype._pnodes));
            }
        }
        prototype._prototype = null;
        prototype._pnodes = null;

        // reconfigure meshes with new variant type
        if (variant != null) {
            prototype._props = PropertiesUtil.getFilteredProperties(
                _props, variant);
            new SpatialVisitor<ModelMesh>(ModelMesh.class) {
                public void visit (ModelMesh mesh) {
                    mesh.reconfigure(PropertiesUtil.getFilteredProperties(
                        prototype._props, mesh.getParent().getName()));
                }
            }.traverse(prototype);
        }
        return prototype;
    }

    /**
     * Creates and returns a new instance of this model.
     */
    public Model createInstance ()
    {
        if (_prototype != null) {
            return _prototype.createInstance();
        }
        if (_ccreator == null) {
            _ccreator = new CloneCreator(this);
        }
        Model instance = _ccreator.createCopy();
        instance.initInstance();
        return instance;
    }

    /**
     * Locks the transforms and bounds of this model in the expectation that it
     * will never be moved from its current position.
     */
    public void lockInstance ()
    {
        // collect the instance's animation and controller targets and lock
        // recursively
        HashSet<Spatial> targets = Sets.newHashSet();
        for (String aname : getAnimationNames()) {
            Collections.addAll(targets, getAnimation(aname).transformTargets);
        }
        for (Object ctrl : getControllers()) {
            if (ctrl instanceof ModelController) {
                targets.add(((ModelController)ctrl).getTarget());
            }
        }
        lockInstance(targets);
    }

    @Override
    public Spatial putClone (Spatial store, CloneCreator properties)
    {
        Model mstore = (Model)properties.originalToCopy.get(this);
        if (mstore != null) {
            return mstore;
        } else if (store == null) {
            mstore = new Model(getName(), _props);
        } else {
            mstore = (Model)store;
        }
        // don't clone the emission node, as it contains transient geometry
        if (_emissionNode != null) {
            detachChild(_emissionNode);
        }
        super.putClone(mstore, properties);
        if (_emissionNode != null) {
            attachChild(_emissionNode);
        }
        mstore._prototype = this;
        if (_anims != null) {
            mstore._anims = Maps.newHashMap();
        }
        mstore._pnodes = Maps.newHashMap(properties.originalToCopy);
        mstore._animMode = _animMode;
        return mstore;
    }

    @Override
    public void updateGeometricState (float time, boolean initiator)
    {
        // if we were not visible the last time we were rendered, don't do a
        // full update; just update the world bound and wait until we come
        // into view
        boolean wasOutside = _outside;
        _outside = isOutsideFrustum() && worldBound != null;

        // slow evvvverything down by the animation speed
        time *= _animSpeed;
        if (_anim != null) {
            updateAnimation(time);
        }

        // update controllers and children with accumulated time
        _accum += time;
        if (_outside) {
            if ((lockedMode & LOCKED_TRANSFORMS) != 0) {
                return; // world bound will not changed
            }
            if (!wasOutside) {
                storeWorldBound();
            }
            updateWorldVectors();
            worldBound = _storedBound.transform(getWorldRotation(),
                getWorldTranslation(), getWorldScale(), worldBound);

        } else {
            super.updateGeometricState(_shouldAccumulate ? _accum : time,
                initiator);
            _accum = 0f;
        }
    }

    @Override
    public void onDraw (Renderer r)
    {
        // if we switch from invisible to visible, we have to do a last-minute
        // full update (which only works if our meshes are enqueued)
        super.onDraw(r);
        if (_outside && !isOutsideFrustum()) {
            updateWorldData(0f);
        }
    }

    /**
     * Transforms the current world bound into model space and stores it for
     * when the model is offscreen.
     */
    protected void storeWorldBound ()
    {
        // update the bounds with an identity transform (which will be
        // overwritten after this method is called)
        getWorldRotation().loadIdentity();
        getWorldTranslation().set(Vector3f.ZERO);
        getWorldScale().set(Vector3f.UNIT_XYZ);
        for (int ii = 0, nn = getQuantity(); ii < nn; ii++) {
            getChild(ii).updateGeometricState(0f, false);
        }
        updateWorldBound();

        _storedBound = worldBound.clone(_storedBound);
    }

    /**
     * Determines whether this node was determined to be entirely outside the
     * view frustum.
     */
    protected boolean isOutsideFrustum ()
    {
        for (Node node = this; node != null; node = node.getParent()) {
            if (node.getLastFrustumIntersection() == Camera.OUTSIDE_FRUSTUM) {
                return true;
            }
        }
        return false;
    }

    /**
     * Initializes the per-instance state of this model.
     */
    protected void initInstance ()
    {
        // initialize the controllers
        for (Object ctrl : getControllers()) {
            if (ctrl instanceof ModelController) {
                ModelController mctrl = (ModelController)ctrl;
                mctrl.init(this);
                _shouldAccumulate |= mctrl.shouldAccumulate();
            }
        }
    }

    /**
     * Updates the model's state according to the current animation.
     */
    protected void updateAnimation (float time)
    {
        // no need to update between frames for flipbook animation
        if (_animMode == AnimationMode.FLIPBOOK && _elapsed > 0f &&
            _elapsed < 1f) {
            _elapsed += (time * _anim.frameRate);
            return;
        }

        // advance the frame counter if necessary
        while (_elapsed > 1f) {
            if (!_paused) {
                advanceFrameCounter();
                _elapsed -= 1f;
            } else {
                _elapsed = 1f;
            }
        }

        // update the target transforms and animation frame if not outside the
        // view frustum
        if (!_outside) {
            updateMeshes();
        }

        // if the next index is the same as this one, we are finished
        if (_fidx == _nidx && !_paused) {
            _anim = null;
            _animObservers.apply(new AnimCompletedOp(_animName));
            return;
        }
        _elapsed += (time * _anim.frameRate);
    }

    /**
     * Advances the frame counter by one frame.
     */
    protected void advanceFrameCounter ()
    {
        _fidx = _nidx;
        int nframes = _anim.transforms.length;
        if (_anim.repeatType == Controller.RT_CLAMP) {
            _nidx = Math.max(0, Math.min(_nidx + _fdir, nframes - 1));

        } else if (_anim.repeatType == Controller.RT_WRAP) {
            // % is not a modulo operator, so is not guaranteed to be positive
            _nidx = (_nidx + _fdir) % nframes;
            if (_nidx < 0) {
                _nidx += nframes;
            }

        } else { // _anim.repeatType == Controller.RT_CYCLE
            if ((_nidx + _fdir) < 0 || (_nidx + _fdir) >= nframes) {
                _fdir *= -1; // reverse direction
            }
            _nidx += _fdir;
        }
    }

    /**
     * Updates the states of the model's meshes according to the animation state.
     */
    protected void updateMeshes ()
    {
        if (_animMode == AnimationMode.FLIPBOOK) {
            int frameId = (_anim.animId << 16) | _fidx;
            _anim.applyFrame(_fidx);
            if (!_anim.stored[_fidx]) {
                storeMeshFrame(frameId, false);
                updateWorldData(0f);
                _anim.stored[_fidx] = true;
            }
            setMeshFrame(frameId);

        } else if (_animMode == AnimationMode.MORPH) {
            int frameId1 = (_anim.animId << 16) | _fidx,
                frameId2 = (_anim.animId << 16) | _nidx;
            if (!_anim.stored[_fidx]) {
                storeMeshFrame(frameId1, true);
                _anim.applyFrame(_fidx);
                updateWorldData(0f);
                _anim.stored[_fidx] = true;
            }
            if (!_anim.stored[_nidx]) {
                storeMeshFrame(frameId2, true);
                _anim.applyFrame(_nidx);
                updateWorldData(0f);
                _anim.stored[_nidx] = true;
            }
            _anim.blendFrames(_fidx, _nidx, _elapsed);
            blendMeshFrames(frameId1, frameId2, _elapsed);

        } else { // _animMode == AnimationMode.SKIN
            _anim.blendFrames(_fidx, _nidx, _elapsed);
        }
    }

    /** A reference to the prototype, or <code>null</code> if this is a prototype. */
    protected Model _prototype;

    /** For prototype models, a customized clone creator used to generate instances. */
    protected CloneCreator _ccreator;

    /** The animation mode to use for this model. */
    protected AnimationMode _animMode;

    /** For instances, maps prototype nodes to their corresponding instance nodes. */
    protected Map<ModelSpatial, ModelSpatial> _pnodes;

    /** The model properties. */
    protected Properties _props;

    /** The model animations. */
    protected HashMap<String, Animation> _anims;

    /** The currently running animation, or <code>null</code> for none. */
    protected Animation _anim;

    /** The name of the currently running animation, if any. */
    protected String _animName;

    /** The current animation speed multiplier. */
    protected float _animSpeed = 1f;

    /** The index of the current and next frames. */
    protected int _fidx, _nidx;

    /** The direction for wrapping animations (+1 forward, -1 backward). */
    protected int _fdir;

    /** The frame portion elapsed since the start of the current frame. */
    protected float _elapsed;

    /** The pause status of this model. */
    protected boolean _paused;

    /** The amount of update time accumulated while outside of view frustum. */
    protected float _accum;

    /** Whether or not we should accumulate update time while out of view. */
    protected boolean _shouldAccumulate;

    /** The child node that contains the model's emissions in world space. */
    protected Node _emissionNode;

    /** The model space bounding volume stored for use when the model is
     * offscreen. */
    protected BoundingVolume _storedBound;

    /** Whether or not we were outside the frustum at the last update. */
    protected boolean _outside;

    /** Animation completion listeners. */
    protected ObserverList<AnimationObserver> _animObservers =
        new ObserverList<AnimationObserver>(ObserverList.FAST_UNSAFE_NOTIFY);

    /** Used to notify observers of animation initiation. */
    protected class AnimStartedOp
        implements ObserverList.ObserverOp<AnimationObserver>
    {
        public AnimStartedOp (String name)
        {
            _name = name;
        }

        // documentation inherited from interface ObserverOp
        public boolean apply (AnimationObserver obs)
        {
            return obs.animationStarted(Model.this, _name);
        }

        /** The name of the animation started. */
        protected String _name;
    }

    /** Used to notify observers of animation completion. */
    protected class AnimCompletedOp
        implements ObserverList.ObserverOp<AnimationObserver>
    {
        public AnimCompletedOp (String name)
        {
            _name = name;
        }

        // documentation inherited from interface ObserverOp
        public boolean apply (AnimationObserver obs)
        {
            return obs.animationCompleted(Model.this, _name);
        }

        /** The name of the animation completed. */
        protected String _name;
    }

    /** Used to notify observers of animation cancellation. */
    protected class AnimCancelledOp
        implements ObserverList.ObserverOp<AnimationObserver>
    {
        public AnimCancelledOp (String name)
        {
            _name = name;
        }

        // documentation inherited from interface ObserverOp
        public boolean apply (AnimationObserver obs)
        {
            return obs.animationCancelled(Model.this, _name);
        }

        /** The name of the animation cancelled. */
        protected String _name;
    }

    private static final long serialVersionUID = 1;
}
