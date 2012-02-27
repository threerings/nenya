//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// http://code.google.com/p/nenya/
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

package com.threerings.cast {

import com.threerings.media.image.Colorization;
import com.threerings.util.Arrays;
import com.threerings.util.Equalable;
import com.threerings.util.Hashable;
import com.threerings.util.StringUtil;

/**
 * The character descriptor object details the components that are
 * pieced together to create a single character image.
 */
public class CharacterDescriptor
    implements Equalable, Hashable
{
    /**
     * Constructs a character descriptor.
     *
     * @param components the component ids of the individual components
     * that make up this character.
     * @param zations the colorizations to apply to each of the character
     * component images when compositing actions (an array of
     * colorizations for each component, elements of which can be null to
     * make it easier to support per-component specialized colorizations).
     * This can be null if image recolorization is not desired.
     */
    public function CharacterDescriptor (components :Array, zations :Array)
    {
        _components = components;
        _zations = zations;
    }

    /**
     * Returns an array of the component identifiers comprising the
     * character described by this descriptor.
     */
    public function getComponentIds () :Array
    {
        return _components;
    }

    /**
     * Returns an array of colorization arrays to be applied to the
     * components when compositing action images (one array per
     * component).
     */
    public function getColorizations () :Array
    {
        return _zations;
    }

    /**
     * Updates the colorizations to be used by this character descriptor.
     */
    public function setColorizations (zations :Array) :void
    {
        _zations = zations;
    }

    /**
     * Returns the array of translations to be applied to the components
     * when compositing action images.
     */
    public function getTranslations () :Array
    {
        return _xlations;
    }

    /**
     * Updates the translations to be used by this character descriptor.
     */
    public function setTranslations (xlations :Array) :void
    {
        _xlations = xlations;
    }

    public function hashCode () :int
    {
        var code :int = 0;
        var clength :int = _components.length;
        for (var ii :int = 0; ii < clength; ii++) {
            code ^= _components[ii];
        }
        return code;
    }

    public function equals (other :Object) :Boolean
    {
        if (!(other is CharacterDescriptor)) {
            return false;
        }

        // both the component ids and the colorizations must be equal
        var odesc :CharacterDescriptor = CharacterDescriptor(other);
        if (!Arrays.equals(_components, odesc._components)) {
            return false;
        }

        var zations :Array = odesc._zations;
        if (zations == null && _zations == null) {
            // if neither has colorizations, we're clear
            return true;

        } else if (zations == null || _zations == null) {
            // if one has colorizations whilst the other doesn't, they
            // can't be equal
            return false;
        }

        // otherwise, all of the colorizations must be equal as well
        var zlength :int= zations.length;
        if (zlength != _zations.length) {
            return false;
        }
        for (var ii :int= 0; ii < zlength; ii++) {
            if (!Arrays.equals(_zations[ii], zations[ii])) {
                return false;
            }
        }

        return Arrays.equals(_xlations, odesc._xlations);
    }

    public function toString () :String
    {
        return "[cids=" + StringUtil.toString(_components) +
            ", colors=" + StringUtil.toString(_zations) + "]";
    }

    /** The component identifiers comprising the character. */
    protected var _components :Array;

    /** The colorizations to apply when compositing this character. */
    protected var _zations :Array;

    /** The translations to apply when compositing this character. */
    protected var _xlations :Array;
}
}