//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2011 Three Rings Design, Inc., All Rights Reserved
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

import com.threerings.util.Arrays;
import com.threerings.util.Hashable;
import com.threerings.util.Equalable;
import com.threerings.util.StringUtil;

/**
 * Denotes a class of components to which {@link CharacterComponent}
 * objects belong. Examples include "Hat", "Head", and "Feet". A component
 * class dictates a component's rendering priority so that components can
 * be rendered in an order that causes them to overlap properly.
 *
 * <p> Components support render priority overrides for particular
 * actions, orientations or combinations of actions and orientations. The
 * system is currently structured with the expectation that the overrides
 * will be relatively few (less than fifteen, say) for any given component
 * class. A system that relied on many overrides for its components would
 * want to implement a more scalable algorithm for determining which, if
 * any, override matches a particular action and orientation combination.
 */
public class ComponentClass
    implements Equalable, Hashable
{
    /** The component class name. */
    public var name :String;

    /** The default render priority. */
    public var renderPriority :int;

    /** The color classes to use when recoloring components of this class. May
     * be null if a system does not use recolorable components. */
    public var colors :Array;

    /** The class name of the layer from which this component class obtains a
     * mask to limit rendering to certain areas. */
    public var mask :String;

    /** Indicates the class name of the shadow layer to which this component
     * class contributes a shadow. */
    public var shadow :String;

    /** 1.0 for a normal component, the alpha value of the pre-composited
     * shadow for the special "shadow" component class. */
    public var shadowAlpha :Number = 1.0;

    /** Whether or not components of this class will have translations applied. */
    public var translate :Boolean;

    /**
     * Creates an uninitialized instance suitable for unserialization or
     * population during XML parsing.
     */
    public function ComponentClass ()
    {
    }

    public static function fromXml (xml :XML) :ComponentClass
    {
        var compClass :ComponentClass = new ComponentClass();
        compClass.name = xml.@name;
        compClass.renderPriority = xml.@renderPriority;
        compClass.colors = toStrArray(xml.@colors);
        compClass.mask = xml.@mask;
        compClass.shadow = xml.@shadow;
        compClass.shadowAlpha = xml.@shadowAlpha;
        compClass.translate = xml.@translate;
        for each (var overrideXml :XML in xml.override) {
            compClass.addPriorityOverride(PriorityOverride.fromXml(overrideXml));
        }
        return compClass;
    }

    protected static function toStrArray (str :String) :Array
    {
        if (str == null || str.length == 0) {
            return null;
        }

        return str.split(",").map(function(element :String, index :int, arr :Array) :String {
                return StringUtil.trim(element);
            });
    }

    /**
     * Returns the render priority appropriate for the specified action, orientation and
     * component.
     */
    public function getRenderPriority (action :String, component :String, orientation :int) :int
    {
        // because we expect there to be relatively few priority overrides, we simply search
        // linearly through the list for the closest match
        var ocount :int = (_overrides != null) ? _overrides.length : 0;
        for (var ii :int = 0; ii < ocount; ii++) {
            var over :PriorityOverride = _overrides[ii];
            // based on the way the overrides are sorted, the first match
            // is the most specific and the one we want
            if (over.matches(action, component, orientation)) {
                return over.renderPriority;
            }
        }

        return renderPriority;
    }

    /**
     * Adds the supplied render priority override record to this component class.
     */
    public function addPriorityOverride (override :PriorityOverride) :void
    {
        if (_overrides == null) {
            _overrides = [];
        }
        Arrays.sortedInsert(_overrides, override);
    }

    /**
     * Returns true if this component class contributes a shadow to a
     * particular shadow layer. Note: this is different from <em>being</em> a
     * shadow layer which is determined by calling {@link #isShadow}.
     */
    public function isShadowed () :Boolean
    {
        return (shadow != null);
    }

    /**
     * Returns true if this component class is a shadow layer rather than a normal component class.
     */
    public function isShadow () :Boolean
    {
        return (shadowAlpha != 1.0);
    }

    /**
     * Classes with the same name are the same.
     */
    public function equals (other :Object) :Boolean
    {
        if (other is ComponentClass) {
            return name == (ComponentClass(other)).name;
        } else {
            return false;
        }
    }

    /**
     * Hashcode is based on component class name.
     */
    public function hashCode () :int
    {
        return StringUtil.hashCode(name);
    }

    public function toString () :String
    {
        var buf :String = "[";
        buf = buf.concat("name=", name);
        buf = buf.concat(", pri=", renderPriority);
        if (colors != null) {
            buf = buf.concat(", colors=", StringUtil.toString(colors));
        }
        if (mask != null) {
            buf = buf.concat(", mask=", mask);
        }
        if (shadowAlpha != 1.0) {
            buf = buf.concat(", shadow=", shadowAlpha);
        } else if (shadow != null) {
            buf = buf.concat(", shadow=", shadow);
        }
        return buf.concat("]");
    }

    /** A list of render priority overrides. */
    protected var _overrides :Array = [];
}
}