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

package com.threerings.jme.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.jme.math.Matrix4f;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Controller;
import com.jme.scene.Node;
import com.jme.scene.Spatial;

import com.samskivert.util.Tuple;

import com.threerings.jme.model.Model;
import com.threerings.jme.util.JmeUtil;

import com.threerings.jme.tools.ModelDef.TransformNode;

/**
 * A basic representation for keyframe animations.
 */
public class AnimationDef
{
    /** The rate of the animation in frames per second. */
    public int frameRate;

    /** A single frame of the animation. */
    public static class FrameDef
    {
        /** Transform for affected nodes. */
        public ArrayList<TransformDef> transforms = Lists.newArrayList();

        public void addTransform (TransformDef transform)
        {
            transforms.add(transform);
        }

        /** Adds all transform targets in this frame to the supplied sets. */
        public void addTransformTargets (
            HashMap<String, Spatial> nodes, HashMap<String, TransformNode> tnodes,
            HashSet<Spatial> staticTargets, HashSet<Spatial> transformTargets)
        {
            for (int ii = 0, nn = transforms.size(); ii < nn; ii++) {
                String name = transforms.get(ii).name;
                Spatial target = nodes.get(name);
                if (target == null) {
                    continue;
                }
                TransformNode tnode = tnodes.get(name);
                if (tnode.baseLocalTransform != null) {
                    staticTargets.add(target);
                } else {
                    transformTargets.add(target);
                }
            }
        }

        /** Returns the array of transforms for this frame. */
        public Model.Transform[] getTransforms (Spatial[] targets)
        {
            Model.Transform[] mtransforms =
                new Model.Transform[targets.length];
            for (int ii = 0; ii < targets.length; ii++) {
                mtransforms[ii] = getTransform(targets[ii]);
            }
            return mtransforms;
        }

        /** Returns the transform for the supplied target. */
        protected Model.Transform getTransform (Spatial target)
        {
            String name = target.getName();
            for (int ii = 0, nn = transforms.size(); ii < nn; ii++) {
                TransformDef transform = transforms.get(ii);
                if (name.equals(transform.name)) {
                    return transform.getTransform();
                }
            }
            return null;
        }
    }

    /** A transform for a single node. */
    public static class TransformDef
    {
        /** The name of the affected node. */
        public String name;

        /** The transformation parameters. */
        public float[] translation;
        public float[] rotation;
        public float[] scale;

        /** Returns the live transform object. */
        public Model.Transform getTransform ()
        {
            return new Model.Transform(
                new Vector3f(translation[0], translation[1], translation[2]),
                new Quaternion(rotation[0], rotation[1], rotation[2], rotation[3]),
                new Vector3f(scale[0], scale[1], scale[2]));
        }
    }

    /** The individual frames of the animation. */
    public ArrayList<FrameDef> frames = Lists.newArrayList();

    public void addFrame (FrameDef frame)
    {
        frames.add(frame);
    }

    /**
     * Runs through each frame of the animation, updating the transforms of the preprocessing
     * node to keep track of which transforms diverge from their original states.
     */
    public void filterTransforms (Node root, HashMap<String, TransformNode> nodes)
    {
        // clear the nodes' transformed flags
        for (TransformNode node : nodes.values()) {
            node.transformed = false;
        }

        // run through all animation frames
        for (FrameDef frame : frames) {
            for (TransformDef transform : frame.transforms) {
                TransformNode node = nodes.get(transform.name);
                if (node != null) {
                    node.setLocalTransform(
                        transform.translation, transform.rotation, transform.scale);
                    node.transformed = true;
                }
            }
            root.updateGeometricState(0f, true);
            for (TransformNode node : nodes.values()) {
                node.cullDivergentTransforms();
            }
        }

        // remove from merge candidates any pairs where one is visible and the other isn't
        for (TransformNode node : nodes.values()) {
            for (Iterator<Tuple<TransformNode, Matrix4f>> it = node.relativeTransforms.iterator();
                    it.hasNext(); ) {
                if (it.next().left.transformed != node.transformed) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Creates the "live" animation object that will be serialized with the
     * object.
     *
     * @param props the animation properties
     * @param nodes the nodes in the model, mapped by name
     */
    public Model.Animation createAnimation (
        Properties props, HashMap<String, Spatial> nodes, HashMap<String, TransformNode> tnodes)
    {
        // find all affected nodes
        HashSet<Spatial> staticTargets = Sets.newHashSet(),
            transformTargets = Sets.newHashSet();
        for (int ii = 0, nn = frames.size(); ii < nn; ii++) {
            frames.get(ii).addTransformTargets(nodes, tnodes, staticTargets, transformTargets);
        }

        // create and configure the animation
        Model.Animation anim = new Model.Animation();
        anim.frameRate = frameRate;
        anim.repeatType = JmeUtil.parseRepeatType(props.getProperty("repeat_type"),
            Controller.RT_CLAMP);

        // collect all transforms
        anim.staticTargets = staticTargets.toArray(new Spatial[staticTargets.size()]);
        anim.transformTargets = transformTargets.toArray(new Spatial[transformTargets.size()]);
        anim.transforms = new Model.Transform[frames.size()][transformTargets.size()];
        for (int ii = 0; ii < anim.transforms.length; ii++) {
            anim.transforms[ii] =
                frames.get(ii).getTransforms(anim.transformTargets);
        }

        return anim;
    }
}
