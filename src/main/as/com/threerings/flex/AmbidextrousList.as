//
// $Id$
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

package com.threerings.flex {

import mx.controls.List;

import mx.core.EdgeMetrics;

/**
 * A list that can be configured with the vertical scrollbar on the left or right side.
 */
public class AmbidextrousList extends List
{
    /**
     * Get whether the scrollbar is on the left side.
     */
    public function get scrollBarOnLeft () :Boolean
    {
        return _scrollLeft;
    }

    /**
     * Sets whether to place the scrollbar on the left or right side.
     */
    public function set scrollBarOnLeft (scrollLeft :Boolean) :void
    {
        _scrollLeft = scrollLeft;

        scrollAreaChanged = true;
        invalidateDisplayList();
    }

    /** @inheritDoc */
    override public function get viewMetrics () :EdgeMetrics
    {
        var em :EdgeMetrics = super.viewMetrics;

        if (_scrollLeft && verticalScrollBar && verticalScrollBar.visible) {
            em.right -= verticalScrollBar.minWidth;
            em.left += verticalScrollBar.minWidth;
        }

        return em;
    }

    /** @inheritDoc */
    override protected function updateDisplayList (uw :Number, uh :Number) :void
    {
        super.updateDisplayList(uw, uh);

        if (_scrollLeft) {
            var vm :EdgeMetrics = viewMetrics;
            verticalScrollBar.move(vm.left - verticalScrollBar.minWidth, vm.top);
        }
    }

    /** Do we want to have the scrollbar on the left? */
    protected var _scrollLeft :Boolean;
}
}
