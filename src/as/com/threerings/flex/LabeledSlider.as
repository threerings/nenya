package com.threerings.flex {

import mx.containers.HBox;

import mx.controls.Label;

import mx.controls.sliderClasses.Slider;

import mx.events.SliderEvent;

/**
 * A simple component that displays a label to the left of a slider.
 */
public class LabeledSlider extends HBox
{
    /** The slider, all public and accessable. Don't fuck it up! */
    public var slider :Slider;

    /**
     * Create a LabeledSlider holding the specified slider.
     */
    public function LabeledSlider (slider :Slider)
    {
        _label = new Label();
        _label.text = String(slider.value);

        addChild(_label);
        this.slider = slider;
        slider.showDataTip = false; // because we do it...
        addChild(slider);

        slider.addEventListener(SliderEvent.CHANGE, handleSliderChange, false, 0, true);
    }

    protected function handleSliderChange (event :SliderEvent) :void
    {
        _label.text = String(event.value);
    }

    protected var _label :Label;
}
}
