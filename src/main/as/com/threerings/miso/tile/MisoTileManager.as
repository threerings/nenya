//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2011 Three Rings Design, Inc., All Rights Reserved
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

package com.threerings.miso.tile {

import com.threerings.media.tile.TileManager;
import com.threerings.util.Set;

/**
 * Extends the basic tile manager and provides support for automatically generating fringes in
 * between different types of base tiles in a scene.
 */
public class MisoTileManager extends TileManager
{
    public function setFringer (fringer :AutoFringer) :void
    {
        _fringer = fringer;
    }

    public function getFringer () :AutoFringer
    {
        return _fringer;
    }

    override public function ensureLoaded (tileSets :Set, completeCallback :Function,
        progressCallback :Function) :void
    {
        // Get those we'll use to fringe with us, too.
        var fringeSets :Set = _fringer.getFringeSets(tileSets);
        fringeSets.forEach(function (tsetId :int) :void {
            tileSets.add(tsetId);
        });

        super.ensureLoaded(tileSets, completeCallback, progressCallback);
    }

    protected var _fringer :AutoFringer;
}
}
