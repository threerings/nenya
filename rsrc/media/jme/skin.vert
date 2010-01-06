//
// $Id: ImageCache.java 158 2007-02-24 00:38:17Z mdb $
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
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

/** The bone transforms. */
uniform mat4 boneTransforms[MAX_BONE_COUNT];

/** The bone indices. */
attribute vec4 boneIndices;

/** The bone weights. */
attribute vec4 boneWeights;

/** The amount of fog. */
#ifdef FOG
  varying float fogAlpha;
#endif

/**
 * Vertex shader for skinned meshes.
 */
void main ()
{
    vec4 normal4 = vec4(gl_Normal, 0.0);

    // add up the vertex as transformed by each bone and scaled by each weight
    vec4 modelVertex = vec4(0.0, 0.0, 0.0, 0.0);
    vec4 modelNormal = vec4(0.0, 0.0, 0.0, 0.0);
    for (int ii = 0; ii < 4; ii++) {
        mat4 boneTransform = boneTransforms[int(boneIndices[ii])];
        modelVertex += boneTransform * gl_Vertex * boneWeights[ii];
        modelNormal += boneTransform * normal4 * boneWeights[ii];
    }

    // apply the standard transformation
    gl_Position = gl_ModelViewProjectionMatrix * modelVertex;

    // transform the vertex and normal into eye space
    vec3 eyeVertex = vec3(gl_ModelViewMatrix * modelVertex);
    vec3 eyeNormal = normalize(vec3(gl_ModelViewMatrixInverseTranspose * modelNormal));

    // set gl_FrontColor based on vertex, normal and light parameters
    SET_FRONT_COLOR

    // set the varying texture coordinates
    SET_TEX_COORDS

    // set the fog alpha based on the eye space vertex
    SET_FOG_ALPHA
}
