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

package com.threerings.openal;

import java.nio.ByteBuffer;

import org.lwjgl.openal.AL10;
import org.lwjgl.util.WaveData;

/**
 * Contains data for a single sampled sound.
 */
public class Clip
{
    public Clip ()
    {}

    /**
     * Fills in a clip from the given wave data.
     */
    public Clip (WaveData data)
    {
        format = data.format;
        frequency = data.samplerate;
        this.data = data.data;
    }

    /** The OpenAL format of this clip: {@link AL10#AL_FORMAT_MONO8}, etc. */
    public int format;

    /** The frequency of this clip in samples per second. */
    public int frequency;

    /** The audio data. */
    public ByteBuffer data;
}
