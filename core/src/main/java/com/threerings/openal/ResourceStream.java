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

import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;

import static com.threerings.openal.Log.log;

/**
 * An audio stream read from one or more resources (via "resource://" URLs, so
 * {@link com.threerings.resource.ResourceManager#activateResourceProtocol} must be called).
 */
public class ResourceStream extends URLStream
{
    /**
     * Creates a new stream for the specified resource (from the default bundle).
     *
     * @param loop whether or not to play the file in a continuous loop
     * if there's nothing on the queue
     */
    public ResourceStream (SoundManager soundmgr, String resource, boolean loop)
        throws IOException
    {
        this(soundmgr, "", resource, loop);
    }

    /**
     * Creates a new stream for the specified resource.
     *
     * @param loop whether or not to play the file in a continuous loop
     * if there's nothing on the queue
     */
    public ResourceStream (SoundManager soundmgr, String bundle, String resource, boolean loop)
        throws IOException
    {
        super(soundmgr, new URL("resource://" + bundle + "/" + resource), loop);
    }

    /**
     * Adds a resource (from the default bundle) to the queue of files to play.
     *
     * @param loop if true, play this file in a loop if there's nothing else
     * on the queue
     */
    public void queueResource (String resource, boolean loop)
    {
        queueResource("", resource, loop);
    }

    /**
     * Adds a resource to the queue of files to play.
     *
     * @param loop if true, play this file in a loop if there's nothing else
     * on the queue
     */
    public void queueResource (String bundle, String resource, boolean loop)
    {
        try {
            queueURL(new URL("resource://" + bundle + "/" + resource), loop);
        } catch (MalformedURLException e) {
            log.warning("Invalid resource url.", "resource", resource, e);
        }
    }
}
