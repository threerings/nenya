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

package com.threerings.flex {

import mx.effects.IEffectInstance;
import mx.effects.Effect;

public class FunctionEffect extends Effect
{
    /** The function to call. */
    public var func :Function;

    /** The arguments to pass to the function. */
    public var args :Array;

    public function FunctionEffect (target :Object = null)
    {
        super(target);

        instanceClass = FunctionEffectInstance;
    }

    // documentation inherited
    override protected function initInstance (instance :IEffectInstance) :void
    {
        super.initInstance(instance);

        var fe :FunctionEffectInstance = (instance as FunctionEffectInstance);
        fe.func = func;
        fe.args = args;
    }
}
}
