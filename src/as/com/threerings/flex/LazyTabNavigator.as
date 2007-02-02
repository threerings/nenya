package com.threerings.flex {

import mx.core.ContainerCreationPolicy;

import mx.core.UIComponent;

import mx.containers.TabNavigator;
import mx.containers.VBox;

/**
 * A tab navigator that is specially set up for lazy-creating the
 * content in each tab.
 */
public class LazyTabNavigator extends TabNavigator
{
    public function LazyTabNavigator ()
    {
        super();
    }

    /**
     * Add a tab to the container. The creation function takes no args and
     * returns a UIComponent.
     */
    public function addTab (label :String, creation :Function) :void
    {
        addTabAt(label, creation, numChildren);
    }

    /**
     * Add a tab to the container at the specified index.
     * The creation function takes no args and returns a UIComponent.
     */
    public function addTabAt (
        label :String, creation :Function, index :int) :void
    {
        var box :LazyContainer = new LazyContainer(creation);
        box.label = label;
        addChildAt(box, index);
    }
}
}
