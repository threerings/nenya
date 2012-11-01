//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// https://github.com/threerings/nenya
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

package com.threerings.media.animation;

import java.util.Iterator;

import com.samskivert.util.SortableArrayList;

import com.threerings.media.AbstractMedia;
import com.threerings.media.AbstractMediaManager;

/**
 * Manages a collection of animations, ticking them when the animation manager itself is ticked and
 * generating events when animations finish and suchlike.
 */
public class AnimationManager extends AbstractMediaManager
    implements Iterable<Animation>
{
    /**
     * Registers the given {@link Animation} with the animation manager for ticking and painting.
     */
    public void registerAnimation (Animation anim)
    {
        insertMedia(anim);
    }

    /**
     * Un-registers the given {@link Animation} from the animation manager. The bounds of the
     * animation will automatically be invalidated so that they are properly rerendered in the
     * absence of the animation.
     */
    public void unregisterAnimation (Animation anim)
    {
        removeMedia(anim);
    }

    public Iterator<Animation> iterator ()
    {
        return _anims.iterator();
    }

    @Override
    protected void tickAllMedia (long tickStamp)
    {
        super.tickAllMedia(tickStamp);

        for (int ii = _anims.size() - 1; ii >= 0; ii--) {
            Animation anim = _anims.get(ii);
            if (!anim.isFinished()) {
                continue;
            }

            // as the anim is finished, remove it and notify observers
            anim.willFinish(tickStamp);
            unregisterAnimation(anim);
            anim.didFinish(tickStamp);
            // Log.info("Removed finished animation " + anim + ".");
        }
    }

    @Override
    protected SortableArrayList<? extends AbstractMedia> createMediaList ()
    {
        return (_anims = new SortableArrayList<Animation>());
    }

    protected SortableArrayList<Animation> _anims;
}
