package com.threerings.flash {

public class AnimationAdapter extends Animation
{
    /**
     * @param host the display object we'll be animating.
     * @param enterFrame called on every frame: function (elapsed :Number) :void
     */
    public function AnimationAdapter (enterFrame :Function)
    {
        _enterFrame = enterFrame;
    }

    override protected function enterFrame () :void
    {
        _enterFrame(_now - _start);
    }

    protected var _enterFrame :Function;
}
}
