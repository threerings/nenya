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

package com.threerings.miso.client {
    
import flash.utils.getTimer;

import flash.events.TimerEvent;
import flash.utils.Timer;

/**
 * Registers objects that wish to be sent a tick() call once per frame.
 */
public class Ticker
{
    public function start () :void
    {
        _t.addEventListener(TimerEvent.TIMER, handleTimer);
        _t.start();
    }

    public function stop () :void
    {
        _t.removeEventListener(TimerEvent.TIMER, handleTimer);
        _t.stop();
        
    }

    public function handleTimer (event :TimerEvent) :void
    {
        tick(getTimer());
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
        _tickables.remove(tickable);
    }

    protected var _tickables :Array = [];

    /** A timer that will fire every "frame". */
    protected var _t :Timer = new Timer(1);
}
}