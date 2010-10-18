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

package com.threerings.jme.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.Charset;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.lwjgl.opengl.ARBShaderObjects;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.jme.scene.state.GLSLShaderObjectsState;
import com.jme.system.DisplaySystem;
import com.jme.util.ShaderAttribute;
import com.jme.util.ShaderUniform;
import com.jme.util.geom.BufferUtils;

import com.samskivert.util.ArrayUtil;

import com.threerings.resource.ResourceManager;

import static com.threerings.jme.Log.*;

/**
 * Caches shaders under their names and preprocessor definitions, ensuring that identical shaders
 * are only compiled once.
 */
public class ShaderCache
{
    /**
     * Create a shader cache that will obtain shader source from the supplied resource manager.
     */
    public ShaderCache (ResourceManager rsrcmgr)
    {
        _rsrcmgr = rsrcmgr;
    }

    /**
     * Creates a new shader state with the supplied vertex shader, fragment shader,
     * and preprocessor definitions.  If a program with the given parameters has already been
     * compiled, the state will use the program id of the existing shader.
     *
     * @return the newly created state, or <code>null</code> if there was an error and the
     * program could not be compiled.
     */
    public GLSLShaderObjectsState createState (String vert, String frag, String... defs)
    {
        GLSLShaderObjectsState sstate =
            DisplaySystem.getDisplaySystem().getRenderer().createGLSLShaderObjectsState();
        return (configureState(sstate, vert, frag, defs) ? sstate : null);
    }

    /**
     * (Re)configures an existing shader state with the supplied parameters.
     *
     * @return true if the shader was successfully configured, false if the shader could not
     * be compiled.
     */
    public boolean configureState (
        GLSLShaderObjectsState sstate, String vert, String frag, String... defs)
    {
        return configureState(sstate, vert, frag, defs, null);
    }

    /**
     * (Re)configures an existing shader state with the supplied parameters.
     *
     * @param ddefs an optional array of derived preprocessor definitions that, unlike the
     * principal definitions, need not be compared when differentiating between cached shaders.
     * @return true if the shader was successfully configured, false if the shader could not
     * be compiled.
     */
    public boolean configureState (
        GLSLShaderObjectsState sstate, String vert, String frag, String[] defs, String[] ddefs)
    {
        ShaderKey key = new ShaderKey(vert, frag, defs);
        Integer programId = _programIds.get(key);
        if (programId == null) {
            if (ddefs != null) {
                defs = ArrayUtil.concatenate(defs, ddefs);
            }
            GLSLShaderObjectsState pstate = loadShaders(vert, frag, defs);
            if (pstate == null) {
                return false;
            }
            _programIds.put(key, programId = pstate.getProgramID());
        }
        if (sstate.getProgramID() == programId) {
            return true;
        }
        sstate.setProgramID(programId);
        for (ShaderAttribute attrib : sstate.attribs.values()) {
            attrib.attributeID = -1;
        }
        for (ShaderUniform uniform : sstate.uniforms.values()) {
            uniform.uniformID = -1;
        }
        sstate.setNeedsRefresh(true);
        return true;
    }

    /**
     * Checks whether the specified shader is already loaded.  This is useful in order to avoid
     * generating complex derived definitions when they won't be needed.
     */
    public boolean isLoaded (String vert, String frag, String... defs)
    {
        return _programIds.containsKey(new ShaderKey(vert, frag, defs));
    }

    /**
     * Loads the specified shaders (prepending the supplied preprocessor definitions)
     * and returns a GLSL shader state.  One of the supplied names may be <code>null</code>
     * in order to use the fixed-function pipeline for that part.  The method returns
     * <code>null</code> if the shaders fail to compile (JME will log an error).
     *
     * @param defs a number of preprocessor definitions to be #defined in both shaders
     * (e.g., "ENABLE_FOG", "NUM_LIGHTS 2").
     */
    protected GLSLShaderObjectsState loadShaders (String vert, String frag, String[] defs)
    {
        GLSLShaderObjectsState sstate =
            DisplaySystem.getDisplaySystem().getRenderer().createGLSLShaderObjectsState();
        sstate.load(
            (vert == null) ? null : getSource(vert, defs),
            (frag == null) ? null : getSource(frag, defs));

        // check its link status
        IntBuffer ibuf = BufferUtils.createIntBuffer(1);
        ARBShaderObjects.glGetObjectParameterARB(sstate.getProgramID(),
            ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB, ibuf);
        if (ibuf.get(0) == 0) {
            return null; // failed to link
        }

        // check the info log
        ARBShaderObjects.glGetObjectParameterARB(sstate.getProgramID(),
            ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB, ibuf);
        ByteBuffer bbuf = BufferUtils.createByteBuffer(ibuf.get(0));
        ARBShaderObjects.glGetInfoLogARB(sstate.getProgramID(), ibuf, bbuf);
        String log = Charset.forName("US-ASCII").decode(bbuf).toString();

        // if it runs in software mode, that counts as a failure
        return (log.indexOf("software") == -1) ? sstate : null;
    }

    /**
     * Retrieves the source code to a shader as a single {@link String}, either by fetching it from
     * the cache or loading it from the resource manager (and caching it).  Returns
     * <code>null</code> (after logging a warning) if the shader couldn't be found.
     *
     * @param defs an array of definitions to prepend to the result.
     */
    protected String getSource (String shader, String[] defs)
    {
        // fetch the shader source
        String source = _sources.get(shader);
        if (source == null) {
            StringBuilder buf = new StringBuilder();
            try {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(_rsrcmgr.getResource(shader)));
                String line;
                while ((line = reader.readLine()) != null) {
                    buf.append(line).append('\n');
                }
            } catch (IOException e) {
                log.warning("Failed to load shader [name=" + shader + ", error=" + e + "].");
                return null;
            }
            _sources.put(shader, (source = buf.toString()));
        }

        // prepend the definitions (the version directive comes before anything else)
        StringBuilder buf = new StringBuilder();
        buf.append("#version 110\n");
        for (String def : defs) {
            buf.append("#define ").append(def).append('\n');
        }
        buf.append(source);
        return buf.toString();
    }

    /** Identifies a cached shader. */
    protected static class ShaderKey
    {
        /** The name of the vertex shader (or <code>null</code> for none). */
        public String vert;

        /** The name of the fragment shader (or <code>null</code> for none). */
        public String frag;

        /** The set of preprocessor definitions. */
        public HashSet<String> defs = Sets.newHashSet();

        public ShaderKey (String vert, String frag, String[] defs)
        {
            this.vert = vert;
            this.frag = frag;
            Collections.addAll(this.defs, defs);
        }

        @Override
        public int hashCode ()
        {
            return (vert == null ? 0 : vert.hashCode()) + (frag == null ? 0 : frag.hashCode()) +
                defs.hashCode();
        }

        @Override
        public boolean equals (Object obj)
        {
            ShaderKey okey = (ShaderKey)obj;
            return Objects.equal(vert, okey.vert) && Objects.equal(frag, okey.frag) &&
                defs.equals(okey.defs);
        }

        @Override
        public String toString ()
        {
            return "[vert=" + vert + ", frag=" + frag + ", defs=" + defs + "]";
        }
    }

    /** Provides access to shader source. */
    protected ResourceManager _rsrcmgr;

    /** Maps shader names to source strings. */
    protected HashMap<String, String> _sources = Maps.newHashMap();

    /** Maps shader keys to linked program ids. */
    protected HashMap<ShaderKey, Integer> _programIds = Maps.newHashMap();
}
