//
// $Id$

package com.threerings.flash {

import flash.display.Sprite;

import flash.events.Event;

/**
 * Convenience superclass to use for sprites that need to update every frame.
 * (One must be very careful to remove all ENTER_FRAME listeners when not needed, as they
 * will prevent an object from being garbage collected!)
 */
public class FrameSprite extends Sprite
{
    public function FrameSprite ()
    {
        addEventListener(Event.ADDED_TO_STAGE, handleAdded);
        addEventListener(Event.REMOVED_FROM_STAGE, handleRemoved);
    }

    /**
     * Called when we're added to the stage.
     */
    protected function handleAdded (... ignored) :void
    {
        addEventListener(Event.ENTER_FRAME, handleFrame);
        handleFrame(); // update immediately
    }

    /**
     * Called when we're added to the stage.
     */
    protected function handleRemoved (... ignored) :void
    {
        removeEventListener(Event.ENTER_FRAME, handleFrame);
    }

    /**
     * Called to update our visual appearance prior to the flash player each frame.
     */
    protected function handleFrame (... ignored) :void
    {
        // nothing here. Override in yor subclass.
    }
}

}
