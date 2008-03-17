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
        var p :Point = DisplayUtil.fitRectInRect(new Rectangle(popup.x, popup.y,
            popup.getExplicitOrMeasuredWidth(), popup.getExplicitOrMeasuredHeight()), rect);
        popup.x = p.x;
        popup.y = p.y;
    }

    /**
     * Wee utility method.
     */
    protected static function createStageRect (popup :UIComponent) :Rectangle
    {
        return new Rectangle(0, 0, popup.stage.stageWidth, popup.stage.stageHeight);
    }
}
}
