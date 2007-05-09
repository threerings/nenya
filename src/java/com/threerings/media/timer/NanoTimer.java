//
// $Id$
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

package com.threerings.media.timer;

/**
 * Uses the {@link System#nanoTime} method introduced in 1.5 to try to obtain higher resolution
 * timestamps than those available via {@link System#currentTimeMillis}.
 */
public class NanoTimer implements MediaTimer
{
    // documentation inherited from interface
    public void reset ()
    {
        _resetStamp = System.nanoTime();
    }

    // documentation inherited from interface
    public long getElapsedMillis ()
    {
        return (System.nanoTime() - _resetStamp) / 1000000L;
    }

    // documentation inherited from interface
    public long getElapsedMicros ()
    {
        return (System.nanoTime() - _resetStamp) / 1000L;
    }

    /** The time at which this timer was last reset. */
    protected long _resetStamp = System.nanoTime();
}
