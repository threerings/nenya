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

import com.threerings.media.sprite.PathObserver;
import com.threerings.media.sprite.Sprite;
import com.threerings.media.util.Path;

/**
 * Wraps the dirty work of moving a sprite along a {@link Path} in an Animation.
 */
public class SpritePathAnimation extends Animation
    implements PathObserver
{
    public SpritePathAnimation (Sprite sprite, Path path)
    {
        super(new Rectangle(0, 0, 0, 0)); // We don't render ourselves
        _sprite = sprite;
        _path = path;
    }

    @Override
    public void paint (Graphics2D gfx) { }

    @Override
    public void tick (long tickStamp) { }

    public void pathCancelled (Sprite sprite, Path path)
    {
        _finished = true;
        sprite.removeSpriteObserver(this);
    }

    public void pathCompleted (Sprite sprite, Path path, long when)
    {
        _finished = true;
        sprite.removeSpriteObserver(this);
    }

    @Override
    protected void willStart (long tickStamp)
    {
        super.willStart(tickStamp);
        _sprite.addSpriteObserver(this);
        _sprite.move(_path);
    }

    final protected Sprite _sprite;
    final protected Path _path;
}
