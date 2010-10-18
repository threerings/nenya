//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
// http://code.google.com/p/nenya/
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

package com.threerings.media {

import flash.events.TimerEvent;
import flash.utils.Timer;

import com.threerings.util.ArrayUtil;

/**
 * Registers objects that wish to be sent a tick() call once per frame.
 */
public class Ticker
{
    public function start () :void
    {
        _t.addEventListener(TimerEvent.TIMER, handleTimer);
        _start = new Date().time;
        _t.start();
    }

    public function stop () :void
    {
        _t.removeEventListener(TimerEvent.TIMER, handleTimer);
        _t.stop();

    }

    public function handleTimer (event :TimerEvent) :void
    {
        tick(int(new Date().time - _start));
    }

    protected function tick (tickStamp :int) :void
    {
        for each (var tickable :Tickable in _tickables) {
            tickable.tick(tickStamp);
        }
    }

    public function registerTickable (tickable :Tickable) :void
    {
        _tickables.push(tickable);
    }

    public function removeTickable (tickable :Tickable) :void
    {
        ArrayUtil.removeFirst(_tickables, tickable);
    }

    /** Everyone who wants to hear about our ticks. */
    protected var _tickables :Array = [];

    /** A timer that will fire every "frame". */
    protected var _t :Timer = new Timer(1);

    /** What time our ticker started running - tickStamps will be relative to this time. */
    protected var _start :Number;
}
}