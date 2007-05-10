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

#if BONES_PER_VERTEX == 4
    #define ATTRIB_TYPE vec4
#elif BONES_PER_VERTEX == 3
    #define ATTRIB_TYPE vec3
#elif BONES_PER_VERTEX == 2
    #define ATTRIB_TYPE vec2
#else
    #define ATTRIB_TYPE float
#endif

/** The bone transforms. */
uniform mat4 boneTransforms[MAX_BONE_COUNT];

/** The bone indices. */
attribute ATTRIB_TYPE boneIndices;

/** The bone weights. */
attribute ATTRIB_TYPE boneWeights;

/**
 * Vertex shader for skinned meshes.
 */
void main ()
{
    vec4 normal4 = vec4(gl_Normal, 0.0);

    // add up the vertex as transformed by each bone and scaled by each weight
    #if BONES_PER_VERTEX == 1
        vec4 skinVertex = boneTransforms[int(boneIndices)] * gl_Vertex * boneWeights;
        vec4 skinNormal = boneTransforms[int(boneIndices)] * normal4 * boneWeights;
    #else
        vec4 skinVertex = boneTransforms[int(boneIndices[0])] * gl_Vertex * boneWeights[0];
        vec4 skinNormal = boneTransforms[int(boneIndices[0])] * normal4 * boneWeights[0];
        for (int ii = 1; ii < BONES_PER_VERTEX; ii++) {
            skinVertex += boneTransforms[int(boneIndices[ii])] * gl_Vertex * boneWeights[ii];
            skinNormal += boneTransforms[int(boneIndices[ii])] * normal4 * boneWeights[ii];
        }
    #endif

    // copy the texture coordinates from attribute to varying
    gl_TexCoord[0] = gl_MultiTexCoord0;
    gl_TexCoord[1] = gl_MultiTexCoord1;

    // apply the standard transformation
    gl_Position = gl_ModelViewProjectionMatrix * skinVertex;

    // eye space 'z' is the standard fog coordinate
    gl_FogFragCoord = -dot(gl_ModelViewMatrixTranspose[2], skinVertex);

    // transform the normal into eye space
    vec3 eyeNormal = normalize(vec3(gl_ModelViewMatrixInverseTranspose * skinNormal));

    // set gl_FrontColor based on normal and light parameters
    gl_FrontColor.rgb =
        gl_FrontLightProduct[0].ambient.rgb +
        gl_FrontLightProduct[0].diffuse.rgb *
            max(dot(eyeNormal, gl_LightSource[0].position.xyz), 0.0) +
        gl_FrontLightProduct[1].ambient.rgb +
        gl_FrontLightProduct[1].diffuse.rgb *
            max(dot(eyeNormal, gl_LightSource[1].position.xyz), 0.0);
    gl_FrontColor.a = gl_FrontMaterial.diffuse.a;
}
