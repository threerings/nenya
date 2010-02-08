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
