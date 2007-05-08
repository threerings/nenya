//
// $Id: ImageCache.java 158 2007-02-24 00:38:17Z mdb $
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

package com.threerings.jme.util;

import java.io.IOException;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import com.jme.scene.state.GLSLShaderObjectsState;
import com.jme.system.DisplaySystem;
import com.jme.util.ShaderAttribute;
import com.jme.util.ShaderUniform;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.ObjectUtil;

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
            GLSLShaderObjectsState pstate;
            try {
                pstate = JmeUtil.loadShaders(
                    (vert == null) ? null : _rsrcmgr.getResource(vert),
                    (frag == null) ? null : _rsrcmgr.getResource(frag), defs);
            } catch (IOException e) {
                log.warning("Failed to load shaders [vert=" + vert + ", frag=" + frag +
                    ", error=" + e + "].");
                return false;
            }
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

    /** Identifies a cached shader. */
    protected static class ShaderKey
    {
        /** The name of the vertex shader (or <code>null</code> for none). */
        public String vert;

        /** The name of the fragment shader (or <code>null</code> for none). */
        public String frag;

        /** The set of preprocessor definitions. */
        public HashSet<String> defs = new HashSet<String>();

        public ShaderKey (String vert, String frag, String[] defs)
        {
            this.vert = vert;
            this.frag = frag;
            Collections.addAll(this.defs, defs);
        }

        @Override // documentation inherited
        public int hashCode ()
        {
            return (vert == null ? 0 : vert.hashCode()) + (frag == null ? 0 : frag.hashCode()) +
                defs.hashCode();
        }

        @Override // documentation inherited
        public boolean equals (Object obj)
        {
            ShaderKey okey = (ShaderKey)obj;
            return ObjectUtil.equals(vert, okey.vert) && ObjectUtil.equals(frag, okey.frag) &&
                defs.equals(okey.defs);
        }

        @Override // documentation inherited
        public String toString ()
        {
            return "[vert=" + vert + ", frag=" + frag + ", defs=" + defs + "]";
        }
    }

    /** Provides access to shader source. */
    protected ResourceManager _rsrcmgr;

    /** Maps shader keys to linked program ids. */
    protected HashMap<ShaderKey, Integer> _programIds = new HashMap<ShaderKey, Integer>();
}
