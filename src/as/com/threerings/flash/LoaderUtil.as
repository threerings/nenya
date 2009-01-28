//
// $Id$

package com.threerings.flash {

import flash.display.Loader;

/**
 * Contains a utility method for safely unloading a Loader.
 */
public class LoaderUtil
{
    /**
     * Safely unload the specified loader.
     */
    public static function unload (loader :Loader) :void
    {
        try {
            loader.close();
        } catch (e1 :Error) {
            // ignore
        }
        try {
            loader.unloadAndStop();
        } catch (e2 :Error) {
            // hmm, maybe they are using FP9 still
            try {
                loader.unload();
            } catch (e3 :Error) {
                // ignore
            }
        }
    }
}
}
