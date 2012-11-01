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

import java.awt.Graphics2D;
import java.awt.Rectangle;

import com.threerings.media.MediaPanel;
import com.threerings.media.sprite.Sprite;

/**
 * A Sprite that wraps an animation so that a sequence of frames can be easily moved around the
 * screen. Animations embedded here should not be added directly to a media panel as this sprite
 * will manage them. If the enclosed animation completes, we remove the sprite from the media
 * panel, since the animation would normally do that itself.
 */
public class AnimationSprite extends Sprite
{
    public AnimationSprite (Animation anim, MediaPanel owner)
    {
        super();
        _anim = anim;
        _owner = owner;
    }

    @Override
    public void init ()
    {
        _anim.init(_mgr);
    }

    @Override
    public void tick (long tickStamp)
    {
        super.tick(tickStamp);
        _anim.tick(tickStamp);

        if (_anim.isFinished()) {
            _anim.willFinish(tickStamp);
            _owner.removeSprite(AnimationSprite.this);
            _anim.didFinish(tickStamp);
        } else {
            _bounds = (Rectangle)_anim.getBounds().clone();
        }
    }

    @Override
    public void setLocation (int x, int y)
    {
        _anim.setLocation(x - _oxoff, y - _oyoff);
        super.setLocation(x, y);
    }

    @Override
    public void willStart (long tickStamp)
    {
        super.willStart(tickStamp);
        _anim.willStart(tickStamp);
    }

    @Override
    public void paint (Graphics2D gfx)
    {
        // Nothing to paint for ourselves.

        _anim.paint(gfx);
    }

    protected Animation _anim;
    protected MediaPanel _owner;
}
