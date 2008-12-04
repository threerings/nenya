//
// $Id$

package com.threerings.flex {

import flash.geom.Point;
import flash.geom.Rectangle;

import mx.core.UIComponent;

import com.threerings.flash.DisplayUtil;

/**
 * Flex popup utilities.
 */
public class PopUpUtil
{
    /**
     * Center the popup inside the stage, to which it should already be added.
     */
    public static function center (popup :UIComponent) :void
    {
        centerInRect(popup, createStageRect(popup));
    }

    /**
     * Center the popup within the specified rectangle.
     */
    public static function centerInRect (popup :UIComponent, rect :Rectangle) :void
    {
        var p :Point = DisplayUtil.centerRectInRect(new Rectangle(0, 0,
            popup.getExplicitOrMeasuredWidth(), popup.getExplicitOrMeasuredHeight()), rect);
        popup.x = p.x;
        popup.y = p.y;
    }

    /**
     * Fit the popup inside the stage.
     */
    public static function fit (popup :UIComponent) :void
    {
        fitInRect(popup, createStageRect(popup));
    }

    /**
     * Fit the popup inside the specified rectangle.
     */
    public static function fitInRect (popup :UIComponent, rect :Rectangle) :void
    {
        var p :Point = DisplayUtil.fitRectInRect(createPopupRect(popup), rect);
        popup.x = p.x;
        popup.y = p.y;
    }

    /**
     * Try to fit inside the specified rectangle, also avoiding other popups.
     *
     * @param popup the popup we'll try to move.
     * @param bounds the boundary within which to place the popup.
     * @param padding extra padding to place between popups (but not around the inside of
     *        the bounds, do that yourself if you want it).
     */
    public static function avoidOtherPopups (
        popup :UIComponent, bounds :Rectangle, padding :int = 0) :void
    {
        var avoid :Array = [];
        var r :Rectangle;

        // find the other popups we need to avoid
        for (var ii :int = popup.parent.numChildren - 1; ii >= 0; ii--) {
            var comp :UIComponent = popup.parent.getChildAt(ii) as UIComponent;
            if (comp != null && comp.isPopUp && comp.visible && comp != popup) {
                r = createPopupRect(comp);
                r.inflate(padding, padding);
                avoid.push(r);
            }
        }
        
        r = createPopupRect(popup);
        // put the rectangle in-bounds, so that even if avoiding the others fails, we're
        // at least in-bounds when fitRectInRect resets to the original position.
        var p :Point = DisplayUtil.fitRectInRect(r, bounds);
        r.x = p.x;
        r.y = p.y; // assigning to topLeft adjusts width/height too, so don't do it!
        DisplayUtil.positionRect(r, bounds, avoid);
        popup.x = r.x;
        popup.y = r.y;
    }

    /**
     * Wee utility method.
     */
    protected static function createStageRect (popup :UIComponent) :Rectangle
    {
        return new Rectangle(0, 0, popup.stage.stageWidth, popup.stage.stageHeight);
    }

    protected static function createPopupRect (popup :UIComponent) :Rectangle
    {
        return new Rectangle(popup.x, popup.y,
            popup.getExplicitOrMeasuredWidth(), popup.getExplicitOrMeasuredHeight());
    }
}
}
