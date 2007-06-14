//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2007 Three Rings Design, Inc., All Rights Reserved
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

package com.threerings.flash {

import flash.events.TimerEvent;

import flash.text.TextField;

import flash.utils.Timer;

/**
 * A simple TextField with a method for setting text that will auto-clear.
 */
public class ClearingTextField extends TextField
{
    public function ClearingTextField ()
    {
        _timer = new Timer(1, 1);
        _timer.addEventListener(TimerEvent.TIMER, handleTimer);
    }

    /**
     * Set text that will not auto-clear.
     */
    override public function set text (str :String) :void
    {
        setText(str, 0);
    }

    /**
     * Set the specified text on this TextField, and clear the text 
     * after the specified delay. If more text is set prior to the
     * delay elapsing, the clear is pushed back to that text's delay, if any.
     */
    public function setText (str :String, secondsToClear :Number = 5) :void
    {
        super.text = str;
        _timer.reset(); // stop any running timer

        // maybe start a new countdown
        if (secondsToClear > 0) {
            _timer.delay = secondsToClear * 1000;
            _timer.start();
        }
    }

    protected function handleTimer (event :TimerEvent) :void
    {
        super.text = "";

        // this should be the fragglin' default, and not updating
        // should be the fragglin' exception. GAWD!
        event.updateAfterEvent();
    }

    protected var _timer :Timer;
}
}
