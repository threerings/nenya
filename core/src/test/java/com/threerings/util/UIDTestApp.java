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

package com.threerings.util;

import com.threerings.util.unsafe.Unsafe;

/**
 * Does something extraordinary.
 */
public class UIDTestApp
{
    public static void main (String[] args)
    {
        if (Unsafe.setuid(1000)) {
            System.err.println("Yay! My uid is changed.");
        } else {
            System.err.println("Boo hoo! I couldn't change my uid.");
        }
        if (Unsafe.setgid(60)) {
            System.err.println("Yay! My gid is changed.");
        } else {
            System.err.println("Boo hoo! I couldn't change my gid.");
        }
        try {
            Thread.sleep(60*1000L);
        } catch (Exception e) {
        }
    }
}
