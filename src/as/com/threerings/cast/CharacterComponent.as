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

package com.threerings.cast {

import com.threerings.util.Hashable;
import com.threerings.util.Equalable;
import com.threerings.util.Set;
import com.threerings.util.StringUtil;

/**
 * The character component represents a single component that can be composited with other
 * character components to generate an image representing a complete character displayable in any
 * of the eight compass directions as detailed in the {@link Sprite} class direction constants.
 */
public class CharacterComponent
    implements Equalable, Hashable
{
    /** The unique component identifier. */
    public var componentId :int;

    /** The component's name. */
    public var name :String;

    /** The class of components to which this one belongs. */
    public var componentClass :ComponentClass;

    /**
     * Constructs a character component with the specified id of the specified class.
     */
    public function CharacterComponent (
        componentId :int, name :String, compClass :ComponentClass, fprov :FrameProvider)
    {
        this.componentId = componentId;
        this.name = name;
        this.componentClass = compClass;
        _frameProvider = fprov;
    }

    /**
     * Returns the render priority appropriate for this component at the specified action and
     * orientation.
     */
    public function getRenderPriority (action :String, orientation :int) :int
    {
        return componentClass.getRenderPriority(action, name, orientation);
    }

    /**
     * Returns the image frames for the specified action animation or null if no animation for the
     * specified action is available for this component.
     *
     * @param type null for the normal action frames or one of the custom action sub-types:
     * {@link StandardActions#SHADOW_TYPE}, etc.
     */
    public function getFrames (action :String, type :String) :ActionFrames
    {
        return _frameProvider.getFrames(this, action, type);
    }

    /**
     * Returns the path to the image frames for the specified action animation or null if no
     * animation for the specified action is available for this component.
     *
     * @param type null for the normal action frames or one of the custom action sub-types:
     * {@link StandardActions#SHADOW_TYPE}, etc.
     *
     * @param existentPaths the set of all paths for which there are valid frames.
     */
    public function getFramePath (action :String, type :String, existentPaths :Set) :String
    {
        return _frameProvider.getFramePath(this, action, type, existentPaths);
    }

    public function equals (other :Object) :Boolean
    {
        if (other is CharacterComponent) {
            return componentId == (CharacterComponent(other)).componentId;
        } else {
            return false;
        }
    }

    public function hashCode () :int
    {
    	return componentId;
    }

    public function toString () :String
    {
        return StringUtil.toString(this);
    }

    /** The entity from which we obtain our animation frames. */
    protected var _frameProvider :FrameProvider;
}
}