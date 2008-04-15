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
