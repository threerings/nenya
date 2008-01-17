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

package com.threerings.util.unsafe;

import com.samskivert.util.RunAnywhere;

import static com.threerings.NenyaLog.log;

/**
 * A native library for doing unsafe things involving garbage collection.
 * Don't use this library. If you must ignore that warning, then be sure you use it 
 * sparingly and only in very well considered cases.
 */
public class UnsafeGC
{
    /**
     * Enables or disables garbage collection. <em>Warning:</em> you will
     * be fucked if you leave it disabled for too long. Do not do this
     * unless you are dang sure about what you're doing and are prepared
     * to test your code on every platform this side of Nantucket.
     *
     * <p> Calls to this method do not nest. Regardless of how many times
     * you disable GC, only one call is required to reenable it.
     */
    public static void setGCEnabled (boolean enabled)
    {
        // we don't support nesting, NOOP if the state doesn't change
        if (_loaded && enabled != _gcEnabled) {
            if (_gcEnabled = enabled) {
                enableGC();
            } else {
                disableGC();
            }
        }
    }


    /**
     * Reenable garbage collection after a call to {@link #disableGC}.
     */
    protected static native void enableGC ();

    /**
     * Disables garbage collection.
     */
    protected static native void disableGC ();

    /**
     * Called to initialize our library.
     */
    protected static native boolean init ();

    /** The current state of GC enablement. */
    protected static boolean _gcEnabled = true;

    /** Whether or not we were able to load and initialize our library. */
    protected static boolean _loaded;

    static {
        try {
            System.loadLibrary("unsafegc");
            _loaded = init();
        } catch (SecurityException se) {
            log.warning("Failed to load 'unsafegc' library: " + se + ".");
        } catch (UnsatisfiedLinkError e) {
            log.warning("Failed to load 'unsafegc' library: " + e + ".");
        }
    }
}
