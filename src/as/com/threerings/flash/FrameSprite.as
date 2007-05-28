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
    /**
     * @param renderFrameUponAdding if true, the handleFrame() method
     *        is called whenever an ADDED_TO_STAGE event is received.
     */
    public function FrameSprite (renderFrameUponAdding :Boolean = true)
    {
        _renderOnAdd = renderFrameUponAdding;

        addEventListener(Event.ADDED_TO_STAGE, handleAdded);
        addEventListener(Event.REMOVED_FROM_STAGE, handleRemoved);
    }

    /**
     * Called when we're added to the stage.
     */
    protected function handleAdded (... ignored) :void
    {
        addEventListener(Event.ENTER_FRAME, handleFrame);
        if (_renderOnAdd) {
            handleFrame(); // update immediately
        }
    }

    /**
     * Called when we're added to the stage.
     */
    protected function handleRemoved (... ignored) :void
    {
        removeEventListener(Event.ENTER_FRAME, handleFrame);
    }

    /**
     * Called to update our visual appearance prior to each frame.
     */
    protected function handleFrame (... ignored) :void
    {
        // nothing here. Override in yor subclass.
    }

    /** Should we call handleFrame() when we get ADDED_TO_STAGE? */
    protected var _renderOnAdd :Boolean;
}

}
