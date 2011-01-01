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

import com.threerings.util.ClassUtil;
import com.threerings.util.Comparable;
import com.threerings.util.DirectionUtil;
import com.threerings.util.Hashable;
import com.threerings.util.Equalable;
import com.threerings.util.Set;
import com.threerings.util.Sets;
import com.threerings.util.StringUtil;
import com.threerings.util.XmlUtil;

/** Used to effect custom render orders for particular actions, orientations, etc. */
public class PriorityOverride
    implements Comparable, Hashable, Equalable
{
    /** The overridden render priority value. */
    public var renderPriority :int;

    /** The action, if any, for which this override is appropriate. */
    public var action :String = null;

    /** The component, if any, for which this override is appropriate. */
    public var component :String = null;

    /** The orientations, if any, for which this override is appropriate. */
    public var orients :Set;

    public static function fromXml (xml :XML) :PriorityOverride
    {
        var override :PriorityOverride = new PriorityOverride;
        override.renderPriority = XmlUtil.getIntAttr(xml, "renderPriority");
        override.action = XmlUtil.getStringAttr(xml, "action", null);
        override.component = XmlUtil.getStringAttr(xml, "component", null);
        
        override.orients = Sets.newSetOf(int);
        for each (var orient :int in toOrientArray(
            XmlUtil.getStringAttr(xml, "orients", null))) {
            override.orients.add(orient);
        }
        return override;
    }

    protected static function toOrientArray (str :String) :Array
    {
        if (str == null || str.length == 0) {
            return null;
        }

        return str.split(",").map(function(element :String, index :int, arr :Array) :int {
                return DirectionUtil.fromShortString(StringUtil.trim(element));
            });
    }

    /**
     * Determines whether this priority override matches the specified
     * action, orientation ant component combination.
     */
    public function matches (action :String, component :String, orient :int) :Boolean
    {
        return (((this.orients == null) || orients.contains(orient)) &&
                ((this.component == null) || this.component == component) &&
                ((this.action == null) || this.action == action));
    }

    // documentation inherited from interface
    public function compareTo (po :Object) :int
    {
        // overrides with both an action and an orientation should come first in the list
        var pri :int = priority();
        var opri :int = PriorityOverride(po).priority();
        if (pri == opri) {
            return hashCode() - PriorityOverride(po).hashCode();
        } else {
            return pri - opri;
        }
    }

    public function hashCode () :int
    {
        return StringUtil.hashCode(action) ^ StringUtil.hashCode(component) ^ renderPriority;
    }

    public function equals (other :Object) :Boolean
    {
        if (!(other is PriorityOverride)) {
            return false;
        }
        var otherOverride :PriorityOverride = PriorityOverride(other);

        return otherOverride.renderPriority == renderPriority && otherOverride.action == action &&
            otherOverride.component == component;
    }

    protected function priority () :int
    {
        var priority :int = 0;
        if (component != null) {
            priority += 3; // Extra priority to things that are for specific components
        }
        if (action != null) {
            priority++;
        }
        if (orients != null) {
            priority++;
        }
        return priority;
    }

    public function toString () :String
    {
        return "[pri=" + renderPriority + ", action=" + action + ", component=" + component +
            ", orients=" + orients + "]";
    }

}
}