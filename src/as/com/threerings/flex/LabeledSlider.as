//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
// http://www.threerings.net/code/nenya/
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

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
