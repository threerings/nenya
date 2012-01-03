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

package com.threerings.miso.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.miso.data.ObjectInfo;
import com.threerings.io.ObjectOutputStream;

/**
 * A convenient base class for "virtual" scenes which do not allow editing
 * and compute the base and object tiles rather than obtain them from some
 * data structure.
 */
public /*abstract*/ class VirtualMisoSceneModel extends MisoSceneModel
{
    override public function setBaseTile (arg1 :int, arg2 :int, arg3 :int) :Boolean
    {
        throw new Error("Unsupported");
    }

    override public function addObject (arg1 :ObjectInfo) :Boolean
    {
        throw new Error("Unsupported");
    }

    override public function updateObject (arg1 :ObjectInfo) :void
    {
        throw new Error("Unsupported");
    }

    override public function removeObject (arg1 :ObjectInfo) :Boolean
    {
        throw new Error("Unsupported");
    }

}
}
