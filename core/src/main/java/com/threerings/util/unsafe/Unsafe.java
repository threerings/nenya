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

package com.threerings.util.unsafe;

import com.samskivert.util.RunAnywhere;

import static com.threerings.NenyaLog.log;

/**
 * A native library for doing unsafe things. Don't use this library. If
 * you must ignore that warning, then be sure you use it sparingly and
 * only in very well considered cases.
 */
public class Unsafe
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
        if (isLoaded()) {
            if (!_initialized && !(_initialized = init())) {
                // no joy initializing things
                return;
            }

            if (_initialized && enabled != _gcEnabled) {
                _gcEnabled = enabled;
                if (_gcEnabled) {
                    enableGC();
                } else {
                    disableGC();
                }
            }
        }
    }

    /**
     * Causes the current thread to block for the specified number of
     * milliseconds. This exists primarily to work around the fact that on
     * Linux, {@link Thread#sleep(long)} is only accurate to around 12ms which
     * is wholly unacceptable.
     */
    public static void sleep (int millis)
    {
        if (RunAnywhere.isLinux() && isLoaded()) {
            nativeSleep(millis);
        } else {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ie) {
                log.info("Thread.sleep(" + millis + ") interrupted.");
            }
        }
    }

    /**
     * Sets the process' uid to the specified value.
     *
     * @return true if the uid was changed, false if we were unable to do so.
     */
    public static boolean setuid (int uid)
    {
        if (!RunAnywhere.isWindows() && isLoaded()) {
            return nativeSetuid(uid);
        }
        return false;
    }

    /**
     * Sets the process' gid to the specified value.
     *
     * @return true if the gid was changed, false if we were unable to do so.
     */
    public static boolean setgid (int gid)
    {
        if (!RunAnywhere.isWindows() && isLoaded()) {
            return nativeSetgid(gid);
        }
        return false;
    }

    /**
     * Sets the process' effective uid to the specified value.
     *
     * @return true if the euid was changed, false if we were unable to do so.
     */
    public static boolean seteuid (int uid)
    {
        if (!RunAnywhere.isWindows() && isLoaded()) {
            return nativeSeteuid(uid);
        }
        return false;
    }

    /**
     * Sets the process' effective gid to the specified value.
     *
     * @return true if the egid was changed, false if we were unable to do so.
     */
    public static boolean setegid (int gid)
    {
        if (!RunAnywhere.isWindows() && isLoaded()) {
            return nativeSetegid(gid);
        }
        return false;
    }

    /**
     * Returns true if the native library was successfully loaded, and if this is the first time
     * it's been checked and it failed, reports the failure.
     */
    protected static boolean isLoaded ()
    {
        if (_loadError != null) {
            log.warning("Unable to load 'unsafe' library", "e", _loadError);
            _loadError = null;
        }
        return _loaded;
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
     * Sleeps the current thread for the specified number of milliseconds.
     */
    protected static native void nativeSleep (int millis);

    /**
     * Calls through to the native OS system call to change our uid.
     */
    protected static native boolean nativeSetuid (int uid);

    /**
     * Calls through to the native OS system call to change our gid.
     */
    protected static native boolean nativeSetgid (int gid);

    /**
     * Calls through to the native OS system call to change our euid.
     */
    protected static native boolean nativeSeteuid (int uid);

    /**
     * Calls through to the native OS system call to change our egid.
     */
    protected static native boolean nativeSetegid (int gid);

    /**
     * Called to initialize our library.
     */
    protected static native boolean init ();

    /** The current state of GC enablement. */
    protected static boolean _gcEnabled = true;

    /** Whether or not we were able to load our library. */
    protected static boolean _loaded;

    /** Whether or not we were able to initialize our library (i.e. get access to jvmpi) */
    protected static boolean _initialized;

    /**
     * The error that occurred when loading the native library, or null if none occurred or it was
     * already reported.
     */
    protected static Throwable _loadError;

    static {
        try {
            System.loadLibrary("unsafe");
            _loaded = true;
        } catch (Throwable t) {
            _loadError = t;
        }
    }
}
