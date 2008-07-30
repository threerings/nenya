//
// $Id: ImageUtil.as 189 2007-04-07 00:25:46Z dhoover $
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

import flash.display.*;
import flash.events.*;
import flash.utils.Timer;

import com.threerings.util.ValueEvent;

/**
 * Dispatched when the jpeg is complete.
 * The 'value' property will contain the received data.
 *
 * @eventType flash.events.Event.COMPLETE
 */
[Event(name="complete", type="com.threerings.util.ValueEvent")]

/**
 * Class that will encode a jpeg in the background and fire an event when it's done.
 */
public class BackgroundJPGEncoder extends EventDispatcher {
    
    /**
     * Construct a jpeg encoder.  Once started, the encoder will encode a jpeg over the course
     * of multiple frames, generating an event to deliver the finished jpeg when it is done.
     *
     * @param image The bitmap to encode.
     * @param quality The jpeg quality from 1 to 100 which determines the compression level.
     * @param timeSlice The number of milliseconds of processing to do per frame.  
     */
	public function BackgroundJPGEncoder (
	    image :BitmapData, quality :Number = 50, timeSlice :int = 100)
	{
	    _timeSlice = timeSlice;
	    _encoder = new JPGEncoder(image, quality, PIXEL_GRANULARITY);
	    _timer = new Timer(1);
	    _timer.addEventListener(TimerEvent.TIMER, timerHandler);
	}
	
	/**
	 * Start encoding
	 */
	public function start () :void 
	{
	    _timer.start();
	}
	
	/**
	 * Cancel encoding and discard any intermediate results.
	 */	
	public function cancel () :void
	{
	    _timer.stop();
	    _timer = null;
	    _encoder = null;
	}
	
	protected function timerHandler (event :TimerEvent) :void
	{
	    if (_encoder.process(_timeSlice)) {
	        _timer.stop();
	        // The jpeg is ready so we fire an event passing it to the consumer.
	        dispatchEvent(new ValueEvent(Event.COMPLETE, _encoder.getJpeg()));
	    } 
	}
	
	protected var _timeSlice :int;
	protected var _timer :Timer;
	protected var _encoder :JPGEncoder; 
	
	protected const PIXEL_GRANULARITY :int = 100;
}
}
