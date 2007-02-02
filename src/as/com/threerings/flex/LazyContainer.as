//
// $Id$

package com.threerings.flex {

import flash.display.DisplayObjectContainer;

import mx.core.ContainerCreationPolicy;
import mx.core.UIComponent;
import mx.containers.VBox;

public class LazyContainer extends VBox
{
    /**
     * Create a lazy container that will not create its children
     * until needed.
     *
     * @param creation a function that takes no args and returns
     * a UIComponent to be added to the container.
     */
    public function LazyContainer (creation :Function)
    {
        _creation = creation;
        creationPolicy = ContainerCreationPolicy.NONE;
    }

    override public function createComponentsFromDescriptors (
        recurse :Boolean = true) :void
    {
        super.createComponentsFromDescriptors(recurse);

        if (_creation != null) {
            addChild(_creation() as UIComponent);
            _creation = null; // assist gc
        }
    }

    protected var _creation :Function;
}
}
