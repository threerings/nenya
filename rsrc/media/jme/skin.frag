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

/** The diffuse texture map. */
uniform sampler2D diffuseMap;

/** The emissive texture map. */
#ifdef EMISSIVE_MAPPED
    uniform sampler2D emissiveMap;
#endif

/** The amount of fog. */
#ifdef FOG
    varying float fogAlpha;
#endif

/**
 * Fragment shader for skinned meshes.
 */
void main ()
{
    // start with the diffuse color
    vec4 fragColor = texture2D(diffuseMap, gl_TexCoord[0].st);

    // modulate by the light color
    #ifdef EMISSIVE_MAPPED
        fragColor *= (gl_Color + vec4(texture2D(emissiveMap, gl_TexCoord[0].st).rgb, 0.0));
    #else
        fragColor *= gl_Color;
    #endif

    // blend between the computed color and the fog color
    #ifdef FOG
        gl_FragColor = mix(gl_Fog.color, fragColor, fogAlpha);
    #else
        gl_FragColor = fragColor;
    #endif
}
