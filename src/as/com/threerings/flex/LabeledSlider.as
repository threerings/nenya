package com.threerings.flex {

import mx.core.ScrollPolicy;

import mx.containers.HBox;

import mx.controls.Label;

import mx.controls.sliderClasses.Slider;

import mx.events.SliderEvent;

/**
 * A simple component that displays a label to the left of a slider.
 */
public class LabeledSlider extends HBox
{
    /** The actual slider. */
    public var slider :Slider;

    /**
     * Create a LabeledSlider holding the specified slider.
     */
    public function LabeledSlider (slider :Slider, labelWidth :int = 17)
    {
        _label = new Label();
        _label.text = String(slider.value);
        _label.width = labelWidth;

        horizontalScrollPolicy = ScrollPolicy.OFF;
        setStyle("horizontalGap", 2);

        addChild(_label);
        this.slider = slider;
        slider.showDataTip = false; // because we do it...
        addChild(slider);

        slider.addEventListener(SliderEvent.CHANGE, handleSliderChange, false, 0, true);
    }

    override public function set width (width :Number) :void
    {
        super.width = width;
        slider.width = width - _label.width - 2;
    }

    protected function handleSliderChange (event :SliderEvent) :void
    {
        _label.text = String(event.value);
    }

    protected var _label :Label;
}
}
