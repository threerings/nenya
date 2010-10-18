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

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.jme.bounding.BoundingBox;
import com.jme.bounding.BoundingSphere;
import com.jme.math.FastMath;
import com.jme.math.Matrix4f;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.util.geom.BufferUtils;

import com.samskivert.util.PropertiesUtil;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.jme.model.Model;
import com.threerings.jme.model.ModelController;
import com.threerings.jme.model.ModelMesh;
import com.threerings.jme.model.ModelNode;
import com.threerings.jme.model.SkinMesh;
import com.threerings.jme.util.JmeUtil;

import static com.threerings.jme.Log.log;

/**
 * An intermediate representation for models used to store data parsed from
 * XML and convert it into JME nodes.
 */
public class ModelDef
{
    /** The base class of nodes in the model. */
    public abstract static class SpatialDef
    {
        /** The node's name. */
        public String name;

        /** The name of the node's parent. */
        public String parent;

        /** The node's transformation. */
        public float[] translation;
        public float[] rotation;
        public float[] scale;

        /**
         * Stores the names of all bones referenced by this spatial in the supplied set.
         */
        public void getBoneNames (HashSet<String> bones)
        {
            // nothing by default
        }

        /** Checks whether it's possible (disregarding issues of transformation) to merge
         * the specified spatial into this one. */
        public abstract boolean canMerge (Properties props, SpatialDef other);

        /** Merges another spatial into this one. */
        public abstract void merge (SpatialDef other, Matrix4f xform);

        /** Returns a JME node for this definition. */
        public Spatial getSpatial (Properties props)
        {
            if (_spatial == null) {
                _spatial = createSpatial(
                    PropertiesUtil.getFilteredProperties(props, name));
                setTransform();
            }
            return _spatial;
        }

        /** Sets the transform of the created node. */
        protected void setTransform ()
        {
            _spatial.getLocalTranslation().set(translation[0], translation[1],
                translation[2]);
            _spatial.getLocalRotation().set(rotation[0], rotation[1],
                rotation[2], rotation[3]);
            _spatial.getLocalScale().set(scale[0], scale[1], scale[2]);
        }

        /** Creates a JME node for this definition. */
        public abstract Spatial createSpatial (Properties props);

        /** Resolves any name references using the supplied map. */
        public void resolveReferences (
            HashMap<String, Spatial> nodes, HashSet<Spatial> referenced)
        {
            Spatial pnode = nodes.get(parent);
            if (pnode instanceof ModelNode) {
                ((ModelNode)pnode).attachChild(_spatial);

            } else if (parent != null) {
                log.warning("Missing or invalid parent node [spatial=" +
                    name + ", parent=" + parent + "].");
            }
        }

        /** The JME node created for this definition. */
        protected Spatial _spatial;
    }

    /** A rigid triangle mesh. */
    public static class TriMeshDef extends SpatialDef
    {
        /** The geometry offset transform. */
        public float[] offsetTranslation;
        public float[] offsetRotation;
        public float[] offsetScale;

        /** Whether or not the mesh allows back face culling. */
        public boolean solid;

        /** The texture of the mesh, if any. */
        public String texture;

        /** Whether or not the mesh is (partially) transparent. */
        public boolean transparent;

        /** The vertices of the mesh. */
        public HashArrayList<Vertex> vertices = new HashArrayList<Vertex>();

        /** The triangle indices. */
        public ArrayList<Integer> indices = Lists.newArrayList();

        /** Whether or not any of the vertices have texture coordinates. */
        public boolean tcoords;

        public void addVertex (Vertex vertex)
        {
            int idx = vertices.indexOf(vertex);
            if (idx != -1) {
                indices.add(idx);
            } else {
                indices.add(vertices.size());
                vertices.add(vertex);
            }
            tcoords = tcoords || vertex.tcoords != null;
        }

        // documentation inherited
        public boolean canMerge (Properties props, SpatialDef other)
        {
            if (getClass() != other.getClass()) {
                return false; // require exact class match
            }
            TriMeshDef omesh = (TriMeshDef)other;
            return solid == omesh.solid && transparent == omesh.transparent &&
                Objects.equal(texture, omesh.texture) &&
                PropertiesUtil.getSubProperties(props, name).equals(
                    PropertiesUtil.getSubProperties(props, omesh.name));
        }

        // documentation inherited
        public void merge (SpatialDef other, Matrix4f xform)
        {
            TriMeshDef omesh = (TriMeshDef)other;

            // prepend the inverse of the offset transformation
            xform = getOffsetTransform().invertLocal().multLocal(xform);

            // and append the other's offset
            xform.multLocal(omesh.getOffsetTransform());

            // extract the rotation to transform normals
            Quaternion xrot = xform.toRotationQuat();

            // transform the vertices and add them in
            for (Vertex vertex : omesh.vertices) {
                vertex.transform(xform, xrot);
            }
            for (int idx : omesh.indices) {
                addVertex(omesh.vertices.get(idx));
            }
        }

        // documentation inherited
        public Spatial createSpatial (Properties props)
        {
            ModelNode node = new ModelNode(name);
            if (indices.size() > 0) {
                _mesh = createMesh();
                optimizeVertexOrder();
                configureMesh(props);
                node.attachChild(_mesh);
            }
            return node;
        }

        /** Gets the matrix representing the offset transform. */
        protected Matrix4f getOffsetTransform ()
        {
            Vector3f otrans = new Vector3f(), oscale = new Vector3f(1f, 1f, 1f);
            Quaternion orot = new Quaternion();
            if (offsetTranslation != null) {
                otrans.set(offsetTranslation[0], offsetTranslation[1], offsetTranslation[2]);
            }
            if (offsetRotation != null) {
                orot.set(offsetRotation[0], offsetRotation[1], offsetRotation[2],
                    offsetRotation[3]);
            }
            if (offsetScale != null) {
                oscale.set(offsetScale[0], offsetScale[1], offsetScale[2]);
            }
            return JmeUtil.setTransform(otrans, orot, oscale, new Matrix4f());
        }

        /** Creates the mesh to attach to the node. */
        protected ModelMesh createMesh ()
        {
            return new ModelMesh("mesh");
        }

        /** Reorders the vertices to optimize for vertex cache utilization.  Uses the algorithm
         * described in Tom Forsyth's article
         * <a href="http://home.comcast.net/~tom_forsyth/papers/fast_vert_cache_opt.html">
         * Linear-Speed Vertex Cache Optimization</a>.
         */
        protected void optimizeVertexOrder ()
        {
            // start by compiling a list of triangles cross-linked with the vertices they use
            // (we use a linked hash set to ensure consistent iteration order for serialization)
            LinkedHashSet<Triangle> triangles = new LinkedHashSet<Triangle>();
            for (int ii = 0, nn = indices.size(); ii < nn; ii += 3) {
                Vertex[] tverts = new Vertex[] {
                    vertices.get(indices.get(ii)),
                    vertices.get(indices.get(ii + 1)),
                    vertices.get(indices.get(ii + 2))
                };
                Triangle triangle = new Triangle(tverts);
                for (Vertex tvert : tverts) {
                    if (tvert.triangles == null) {
                        tvert.triangles = Lists.newArrayList();
                    }
                    tvert.triangles.add(triangle);
                }
                triangles.add(triangle);
            }

            // init the scores
            for (Vertex vertex : vertices) {
                vertex.updateScore(Integer.MAX_VALUE);
            }

            // clear the vertices and indices to prepare for readdition
            vertices.clear();
            indices.clear();

            // while there are triangles remaining, keep adding the one with the best score
            // (as determined by its LRU cache position and number of remaining triangles)
            HashArrayList<Vertex> vcache = new HashArrayList<Vertex>();
            while (!triangles.isEmpty()) {
                // first look for triangles in the cache
                Triangle bestTriangle = null;
                float bestScore = -1f;
                for (Vertex vertex : vcache) {
                    for (Triangle triangle : vertex.triangles) {
                        float score = triangle.getScore();
                        if (score > bestScore) {
                            bestTriangle = triangle;
                            bestScore = score;
                        }
                    }
                }

                // if that didn't work, scan the full list
                if (bestTriangle == null) {
                    for (Triangle triangle : triangles) {
                        float score = triangle.getScore();
                        if (score > bestScore) {
                            bestTriangle = triangle;
                            bestScore = score;
                        }
                    }
                }

                // add and update the vertices from the best triangle
                triangles.remove(bestTriangle);
                for (Vertex vertex : bestTriangle.vertices) {
                    addVertex(vertex);
                    vertex.triangles.remove(bestTriangle);
                    vcache.remove(vertex);
                    vcache.add(0, vertex);
                }

                // update the scores of the vertices in the cache
                for (int ii = 0, nn = vcache.size(); ii < nn; ii++) {
                    vcache.get(ii).updateScore(ii);
                }

                // trim the excess (if any) from the end of the cache
                while (vcache.size() > 64) {
                    vcache.remove(vcache.size() - 1);
                }
            }
        }

        /** Configures the mesh. */
        protected void configureMesh (Properties props)
        {
            // set the geometry offset
            if (offsetTranslation != null) {
                _mesh.getLocalTranslation().set(offsetTranslation[0],
                    offsetTranslation[1], offsetTranslation[2]);
            }
            if (offsetRotation != null) {
                _mesh.getLocalRotation().set(offsetRotation[0],
                    offsetRotation[1], offsetRotation[2], offsetRotation[3]);
            }
            if (offsetScale != null) {
                _mesh.getLocalScale().set(offsetScale[0], offsetScale[1],
                    offsetScale[2]);
            }

            // make sure texture is just a filename
            int sidx = (texture == null) ? -1 :
                Math.max(texture.lastIndexOf('/'), texture.lastIndexOf('\\'));
            if (sidx != -1) {
                texture = texture.substring(sidx + 1);
            }

            // configure using properties
            _mesh.configure(solid, texture, transparent, props);

            // set the various buffers
            int vsize = vertices.size();
            FloatBuffer vbuf = BufferUtils.createVector3Buffer(vsize),
                nbuf = BufferUtils.createVector3Buffer(vsize),
                tbuf = tcoords ? BufferUtils.createVector2Buffer(vsize) : null;
            for (int ii = 0; ii < vsize; ii++) {
                vertices.get(ii).setInBuffers(vbuf, nbuf, tbuf);
            }
            IntBuffer ibuf = BufferUtils.createIntBuffer(indices.size());
            for (int ii = 0, nn = indices.size(); ii < nn; ii++) {
                ibuf.put(indices.get(ii));
            }
            _mesh.reconstruct(vbuf, nbuf, null, tbuf, ibuf);

            _mesh.setModelBound("sphere".equals(props.getProperty("bound")) ?
                new BoundingSphere() : new BoundingBox());
            _mesh.updateModelBound();

            // set the mesh's origin to the center of its bounding box
            _mesh.centerVertices();
        }

        /** The mesh that contains the actual geometry. */
        protected ModelMesh _mesh;
    }

    /** A triangle mesh that deforms according to bone positions. */
    public static class SkinMeshDef extends TriMeshDef
    {
        @Override
        public void getBoneNames (HashSet<String> bones)
        {
            for (Vertex vertex : vertices) {
                bones.addAll(((SkinVertex)vertex).boneWeights.keySet());
            }
        }

        @Override
        protected ModelMesh createMesh ()
        {
            return new SkinMesh("mesh");
        }

        @Override
        public void resolveReferences (
            HashMap<String, Spatial> nodes, HashSet<Spatial> referenced)
        {
            super.resolveReferences(nodes, referenced);
            if (_mesh == null) {
                return;
            }

            // create and set the final weight groups
            SkinMesh.WeightGroup[] wgroups = new SkinMesh.WeightGroup[_groups.size()];
            HashMap<String, SkinMesh.Bone> bones = Maps.newHashMap();
            int ii = 0;
            int mweights = 0, tweights = 0;
            for (Map.Entry<Set<String>, WeightGroupDef> entry :
                _groups.entrySet()) {
                SkinMesh.WeightGroup wgroup = new SkinMesh.WeightGroup();
                wgroup.vertexCount = entry.getValue().indices.size();
                wgroup.bones = new SkinMesh.Bone[entry.getKey().size()];
                int jj = 0;
                for (String bname : entry.getKey()) {
                    SkinMesh.Bone bone = bones.get(bname);
                    if (bone == null) {
                        Spatial node = nodes.get(bname);
                        bones.put(bname,
                            bone = new SkinMesh.Bone((ModelNode)node));
                        referenced.add(node);
                    }
                    wgroup.bones[jj++] = bone;
                }
                wgroup.weights = toArray(entry.getValue().weights);
                tweights += wgroup.bones.length;
                mweights = Math.max(wgroup.bones.length, mweights);
                wgroups[ii++] = wgroup;
            }
            ((SkinMesh)_mesh).setWeightGroups(wgroups);
        }

        @Override
        protected void configureMesh (Properties props)
        {
            // divide the vertices up by weight groups
            _groups = Maps.newHashMap();
            for (int ii = 0, nn = vertices.size(); ii < nn; ii++) {
                SkinVertex svertex = (SkinVertex)vertices.get(ii);
                Set<String> bones = svertex.boneWeights.keySet();
                WeightGroupDef group = _groups.get(bones);
                if (group == null) {
                    _groups.put(bones, group = new WeightGroupDef());
                }
                group.indices.add(ii);
                for (String bone : bones) {
                    group.weights.add(svertex.boneWeights.get(bone).weight);
                }
            }

            // reorder the vertices by group
            ArrayList<Vertex> overts = vertices;
            vertices = new HashArrayList<Vertex>();
            int[] imap = new int[overts.size()];
            for (Map.Entry<Set<String>, WeightGroupDef> entry :
                _groups.entrySet()) {
                for (int idx : entry.getValue().indices) {
                    imap[idx] = vertices.size();
                    vertices.add(overts.get(idx));
                }
            }
            for (int ii = 0, nn = indices.size(); ii < nn; ii++) {
                indices.set(ii, imap[indices.get(ii)]);
            }

            super.configureMesh(props);
        }

        /** The intermediate weight groups, mapped by bone names. */
        protected HashMap<Set<String>, WeightGroupDef> _groups;
    }

    /** A generic node. */
    public static class NodeDef extends SpatialDef
    {
        // documentation inherited
        public boolean canMerge (Properties props, SpatialDef other)
        {
            return false;
        }

        // documentation inherited
        public void merge (SpatialDef other, Matrix4f xform)
        {
            throw new UnsupportedOperationException();
        }

        // documentation inherited
        public Spatial createSpatial (Properties props)
        {
            return new ModelNode(name);
        }
    }

    /** Represents a triangle for processing purposes. */
    public static class Triangle
    {
        public Vertex[] vertices;

        public Triangle (Vertex[] vertices)
        {
            this.vertices = vertices;
        }

        public float getScore ()
        {
            return vertices[0].score + vertices[1].score + vertices[2].score;
        }
    }

    /** A basic vertex. */
    public static class Vertex
    {
        public float[] location;
        public float[] normal;
        public float[] tcoords;

        public ArrayList<Triangle> triangles;
        public float score;

        public void updateScore (int cacheIdx)
        {
            float pscore;
            if (cacheIdx > 63) {
                pscore = 0f; // outside the cache
            } else if (cacheIdx < 3) {
                pscore = 0.75f; // the three most recent vertices
            } else {
                pscore = FastMath.pow((63 - cacheIdx) / 60f, 1.5f);
            }
            score = pscore + 2f * FastMath.pow(triangles.size(), -0.5f);
        }

        public void transform (Matrix4f xform, Quaternion xrot)
        {
            Vector3f xvec = new Vector3f(location[0], location[1], location[2]);
            xform.mult(xvec, xvec);
            location[0] = xvec.x;
            location[1] = xvec.y;
            location[2] = xvec.z;

            xvec.set(normal[0], normal[1], normal[2]);
            xrot.mult(xvec, xvec);
            normal[0] = xvec.x;
            normal[1] = xvec.y;
            normal[2] = xvec.z;
        }

        public void setInBuffers (
            FloatBuffer vbuf, FloatBuffer nbuf, FloatBuffer tbuf)
        {
            vbuf.put(location);
            nbuf.put(normal);

            if (tbuf != null) {
                if (tcoords != null) {
                    tbuf.put(tcoords);
                } else {
                    tbuf.put(0f);
                    tbuf.put(0f);
                }
            }
        }

        public String toString ()
        {
            return StringUtil.toString(location);
        }

        @Override
        public int hashCode ()
        {
            return Arrays.hashCode(location) ^ Arrays.hashCode(normal) ^ Arrays.hashCode(tcoords);
        }

        @Override
        public boolean equals (Object obj)
        {
            Vertex overt = (Vertex)obj;
            return Arrays.equals(location, overt.location) &&
                Arrays.equals(normal, overt.normal) &&
                Arrays.equals(tcoords, overt.tcoords);
        }
    }

    /** A vertex influenced by a number of bones. */
    public static class SkinVertex extends Vertex
    {
        /** The bones influencing the vertex, mapped by name. */
        public HashMap<String, BoneWeight> boneWeights = Maps.newHashMap();

        public void addBoneWeight (BoneWeight weight)
        {
            if (weight.weight == 0f) {
                return;
            }
            BoneWeight bweight = boneWeights.get(weight.bone);
            if (bweight != null) {
                bweight.weight += weight.weight;
            } else {
                boneWeights.put(weight.bone, weight);
            }
        }

        /** Finds the bone nodes influencing this vertex. */
        public HashSet<ModelNode> getBones (HashMap<String, Spatial> nodes)
        {
            HashSet<ModelNode> bones = Sets.newHashSet();
            for (String bone : boneWeights.keySet()) {
                Spatial node = nodes.get(bone);
                if (node instanceof ModelNode) {
                    bones.add((ModelNode)node);
                } else {
                    log.warning("Missing or invalid bone for bone weight " +
                        "[bone=" + bone + "].");
                }
            }
            return bones;
        }

        /** Returns the weight of the given bone. */
        public float getWeight (ModelNode bone)
        {
            BoneWeight bweight = boneWeights.get(bone.getName());
            return (bweight == null) ? 0f : bweight.weight;
        }
    }

    /** The influence of a bone on a vertex. */
    public static class BoneWeight
    {
        /** The name of the influencing bone. */
        public String bone;

        /** The amount of influence. */
        public float weight;
    }

    /** A group of vertices influenced by the same bone. */
    public static class WeightGroupDef
    {
        /** The indices of the affected vertex. */
        public ArrayList<Integer> indices = Lists.newArrayList();

        /** The interleaved vertex weights. */
        public ArrayList<Float> weights = Lists.newArrayList();
    }

    /** Contains the transform of a node for preprocessing. */
    public static class TransformNode extends Node
    {
        /** The source definition. */
        public SpatialDef spatial;

        /** If true, this node is referenced by name (as a bone or parent) and cannot be merged
         * into another. */
        public boolean referenced;

        /** If true, this node is a controller target; nodes beneath it can only be merged with
         * other descendants. */
        public boolean controlled;

        /** The node's current local transform. */
        public Matrix4f localTransform = new Matrix4f();

        /** The node's current world space transform. */
        public Matrix4f worldTransform = new Matrix4f();

        /** The node's local transform in the original model, or <code>null</code> if the node's
         * transform has diverged from the original. */
        public Matrix4f baseLocalTransform;

        /** The relative transforms between this and all other loosely compatible nodes not yet
         * eliminated.  As soon as the relative transform diverges in the course of preprocessing
         * an animation, the node/transform pair is removed from the list. */
        public ArrayList<Tuple<TransformNode, Matrix4f>> relativeTransforms;

        /** Marks this node as having been transformed in the course of an animation. */
        public boolean transformed;

        public TransformNode (SpatialDef spatial)
        {
            super(spatial.name);
            this.spatial = spatial;
            setLocalTransform(spatial.translation, spatial.rotation, spatial.scale);
        }

        public void setLocalTransform (float[] translation, float[] rotation, float[] scale)
        {
            getLocalTranslation().set(translation[0], translation[1], translation[2]);
            getLocalRotation().set(rotation[0], rotation[1], rotation[2], rotation[3]);
            getLocalScale().set(scale[0], scale[1], scale[2]);
            JmeUtil.setTransform(
                getLocalTranslation(), getLocalRotation(), getLocalScale(), localTransform);
        }

        @Override
        public void updateWorldVectors ()
        {
            super.updateWorldVectors();
            JmeUtil.setTransform(
                getWorldTranslation(), getWorldRotation(), getWorldScale(), worldTransform);
        }

        public boolean canMerge (Properties props, TransformNode onode)
        {
            // nodes must have same controlled ancestor
            return !onode.referenced && spatial.canMerge(props, onode.spatial) &&
                getControlledAncestor() == onode.getControlledAncestor();
        }

        protected Node getControlledAncestor ()
        {
            Node ref = this;
            while (ref instanceof TransformNode && !((TransformNode)ref).controlled) {
                ref = ref.getParent();
            }
            return ref;
        }

        public void cullDivergentTransforms ()
        {
            if (baseLocalTransform != null && !epsilonEquals(localTransform, baseLocalTransform)) {
                baseLocalTransform = null;
            }
            for (Iterator<Tuple<TransformNode, Matrix4f>> it = relativeTransforms.iterator();
                    it.hasNext(); ) {
                Tuple<TransformNode, Matrix4f> tuple = it.next();
                if (!epsilonEquals(getRelativeTransform(tuple.left), tuple.right)) {
                    it.remove();
                }
            }
        }

        public Matrix4f getRelativeTransform (TransformNode other)
        {
            // return the matrix that takes vertices from the space of the other node
            // into the space of this one
            Matrix4f inv = new Matrix4f();
            worldTransform.invert(inv);
            return inv.mult(other.worldTransform);
        }
    }

    /** The meshes and bones comprising the model. */
    public ArrayList<SpatialDef> spatials = Lists.newArrayList();

    public void addSpatial (SpatialDef spatial)
    {
        // put nodes before meshes so that bones are updated before skin
        spatials.add(spatial instanceof NodeDef ?  0 : spatials.size(),
            spatial);
    }

    /**
     * Creates and returns a transform tree representing the model for preprocessing.
     */
    public Node createTransformTree (Properties props, HashMap<String, TransformNode> nodes)
    {
        // create the nodes and map them by name
        for (SpatialDef spatial : spatials) {
            nodes.put(spatial.name, new TransformNode(spatial));
        }

        // resolve the parents and collect the names of the bones
        Node root = new Node("root");
        HashSet<String> bones = Sets.newHashSet();
        for (TransformNode node : nodes.values()) {
            if (node.spatial.parent == null) {
                root.attachChild(node);
            } else {
                TransformNode pnode = nodes.get(node.spatial.parent);
                if (pnode != null) {
                    pnode.attachChild(node);
                    pnode.referenced = true;
                }
            }
            node.spatial.getBoneNames(bones);
        }

        // mark the bones as referenced
        for (String name : bones) {
            TransformNode node = nodes.get(name);
            if (node != null) {
                node.referenced = true;
            }
        }

        // mark the controlled nodes
        String[] controllers = StringUtil.parseStringArray(props.getProperty("controllers", ""));
        for (String controller : controllers) {
            Properties subProps = PropertiesUtil.getSubProperties(props, controller);
            TransformNode node = nodes.get(subProps.getProperty("node", controller));
            if (node != null) {
                node.referenced = node.controlled = true;
            }
        }

        // store the base transforms and relative transforms for merge candidates
        root.updateGeometricState(0f, true);
        for (TransformNode node : nodes.values()) {
            node.baseLocalTransform = new Matrix4f(node.localTransform);
            node.relativeTransforms = Lists.newArrayList();
            for (TransformNode onode : nodes.values()) {
                if (node == onode || !node.canMerge(props, onode)) {
                    continue;
                }
                node.relativeTransforms.add(new Tuple<TransformNode, Matrix4f>(
                    onode, node.getRelativeTransform(onode)));
            }
        }

        return root;
    }

    /**
     * Merges compatible meshes that retain the same relative transform throughout all animations.
     */
    public void mergeSpatials (HashMap<String, TransformNode> nodes)
    {
        for (TransformNode node : nodes.values()) {
            if (!spatials.contains(node.spatial)) {
                continue;
            }
            for (Tuple<TransformNode, Matrix4f> tuple : node.relativeTransforms) {
                if (spatials.contains(tuple.left.spatial)) {
                    node.spatial.merge(tuple.left.spatial, tuple.right);
                    spatials.remove(tuple.left.spatial);
                }
            }
        }
    }

    /**
     * Creates the model node defined herein.
     *
     * @param props the properties of the model
     * @param nodes a node map to populate
     */
    public Model createModel (Properties props, HashMap<String, Spatial> nodes)
    {
        Model model = new Model(props.getProperty("name", "model"), props);

        // start by creating the spatials and mapping them to their names
        for (int ii = 0, nn = spatials.size(); ii < nn; ii++) {
            Spatial spatial = spatials.get(ii).getSpatial(props);
            nodes.put(spatial.getName(), spatial);
        }

        // then go through again, resolving any name references and attaching root children
        HashSet<Spatial> referenced = Sets.newHashSet();
        for (int ii = 0, nn = spatials.size(); ii < nn; ii++) {
            SpatialDef sdef = spatials.get(ii);
            sdef.resolveReferences(nodes, referenced);
            if (sdef.getSpatial(props).getParent() == null) {
                model.attachChild(sdef.getSpatial(props));
            }
        }

        // create any controllers listed
        String[] controllers = StringUtil.parseStringArray(
            props.getProperty("controllers", ""));
        for (int ii = 0; ii < controllers.length; ii++) {
            Properties subProps =
                PropertiesUtil.getSubProperties(props, controllers[ii]);
            String node = subProps.getProperty("node", controllers[ii]);
            Spatial target = node.equals(model.getName()) ?
                model : nodes.get(node);
            if (target == null) {
                log.warning("Missing controller node [name=" + node + "].");
                continue;
            }
            ModelController ctrl = createController(subProps, target);
            if (ctrl != null) {
                model.addController(ctrl);
                referenced.add(target);
            }
        }

        // get rid of any nodes that serve no purpose
        pruneUnusedNodes(model, nodes, referenced);

        // set the overall scale
        model.setLocalScale(Float.parseFloat(props.getProperty("scale", "1")));

        return model;
    }

    /** Creates, configures, and returns a model controller. */
    protected ModelController createController (
        Properties props, Spatial target)
    {
        // attempt to create an instance of the controller
        ModelController ctrl;
        String cname = props.getProperty("class", "");
        try {
            ctrl = (ModelController)Class.forName(cname).newInstance();
        } catch (Exception e) {
            log.warning("Error instantiating controller [class=" + cname +
                ", error=" + e + "].");
            return null;
        }
        ctrl.configure(props, target);
        return ctrl;
    }

    /** Recursively removes any unused nodes. */
    protected boolean pruneUnusedNodes (
        ModelNode node, HashMap<String, Spatial> nodes,
        HashSet<Spatial> referenced)
    {
        boolean hasValidChildren = false;
        for (int ii = node.getQuantity() - 1; ii >= 0; ii--) {
            Spatial child = node.getChild(ii);
            if (!(child instanceof ModelNode) ||
                pruneUnusedNodes((ModelNode)child, nodes, referenced)) {
                hasValidChildren = true;
            } else {
                node.detachChildAt(ii);
                nodes.remove(child.getName());
            }
        }
        return referenced.contains(node) || hasValidChildren;
    }

    /** Determines whether a pair of matrices are "close enough" to equal. */
    public static boolean epsilonEquals (Matrix4f m1, Matrix4f m2)
    {
        for (int ii = 0; ii < 4; ii++) {
            for (int jj = 0; jj < 4; jj++) {
                if (FastMath.abs(m1.get(ii, jj) - m2.get(ii, jj)) > 0.0001f) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Converts a boxed Float list to an unboxed float array. */
    protected static float[] toArray (ArrayList<Float> list)
    {
        float[] array = new float[list.size()];
        for (int ii = 0, nn = list.size(); ii < nn; ii++) {
            array[ii] = list.get(ii);
        }
        return array;
    }

    /** Accelerates {@link ArrayList#indexOf}, {@link ArrayList#contains}, and
     * {@link ArrayList#remove} using an internal hash map (assumes that all elements of the list
     * are unique and non-null). */
    protected static class HashArrayList<E> extends ArrayList<E>
    {
        @Override
        public boolean add (E element)
        {
            add(size(), element);
            return true;
        }

        @Override
        public void add (int idx, E element)
        {
            super.add(idx, element);
            remapFrom(idx);
        }

        @Override
        public E remove (int idx)
        {
            E element = super.remove(idx);
            _indices.remove(element);
            remapFrom(idx);
            return element;
        }

        @Override
        public void clear ()
        {
            super.clear();
            _indices.clear();
        }

        @Override
        public int indexOf (Object obj)
        {
            Integer idx = _indices.get(obj);
            return (idx == null ? -1 : idx);
        }

        @Override
        public boolean contains (Object obj)
        {
            return _indices.containsKey(obj);
        }

        @Override
        public boolean remove (Object obj)
        {
            Integer idx = _indices.remove(obj);
            if (idx != null) {
                super.remove(idx);
                return true;
            } else {
                return false;
            }
        }

        protected void remapFrom (int idx)
        {
            for (int ii = idx, nn = size(); ii < nn; ii++) {
                _indices.put(get(ii), ii);
            }
        }

        /** Maps elements to their indices in the list. */
        protected HashMap<Object, Integer> _indices = Maps.newHashMap();
    }
}
