//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
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

package com.threerings.util {

/**
 * A single, top-level location for the definition of compass direction
 * constants, which are used by a variety of Narya services.
 */
public class DirectionCodes
{
    /** A direction code indicating no direction. */
    public static const NONE :int = -1;

    /** A direction code indicating moving left. */
    public static const LEFT :int = 0;

    /** A direction code indicating moving right. */
    public static const RIGHT :int = 1;

    /** A direction code indicating moving up. */
    public static const UP :int = 2;

    /** A direction code indicating moving down. */
    public static const DOWN :int = 3;

    /** A direction code indicating a counter-clockwise rotation. */
    public static const CCW :int = 0;

    /** A direction code indicating a clockwise rotation. */
    public static const CW :int = 1;

    /** A direction code indicating horizontal movement. */
    public static const HORIZONTAL :int = 0;

    /** A direction code indicating vertical movement. */
    public static const VERTICAL :int = 1;

    /** A direction code indicating southwest. */
    public static const SOUTHWEST :int = 0;

    /** A direction code indicating west. */
    public static const WEST :int = 1;

    /** A direction code indicating northwest. */
    public static const NORTHWEST :int = 2;

    /** A direction code indicating north. */
    public static const NORTH :int = 3;

    /** A direction code indicating northeast. */
    public static const NORTHEAST :int = 4;

    /** A direction code indicating east. */
    public static const EAST :int = 5;

    /** A direction code indicating southeast. */
    public static const SOUTHEAST :int = 6;

    /** A direction code indicating south. */
    public static const SOUTH :int = 7;

    /** The number of basic compass directions. */
    public static const DIRECTION_COUNT :int = 8;

    /** A direction code indicating west by southwest. */
    public static const WESTSOUTHWEST :int = 8;

    /** A direction code indicating west by northwest. */
    public static const WESTNORTHWEST :int = 9;

    /** A direction code indicating north by northwest. */
    public static const NORTHNORTHWEST :int = 10;

    /** A direction code indicating north by northeast. */
    public static const NORTHNORTHEAST :int = 11;

    /** A direction code indicating east by northeast. */
    public static const EASTNORTHEAST :int = 12;

    /** A direction code indicating east by southeast. */
    public static const EASTSOUTHEAST :int = 13;

    /** A direction code indicating south by southeast. */
    public static const SOUTHSOUTHEAST :int = 14;

    /** A direction code indicating south by southwest. */
    public static const SOUTHSOUTHWEST :int = 15;

    /** The number of fine compass directions. */
    public static const FINE_DIRECTION_COUNT :int = 16;

    /** The four points of the compass. */
    public static const CARDINAL_DIRECTIONS :Array = [ NORTH, EAST, SOUTH, WEST ];
}
}
