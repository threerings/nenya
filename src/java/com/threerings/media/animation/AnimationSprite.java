package com.threerings.media.animation;

import java.awt.Graphics2D;
import java.awt.Rectangle;

import com.threerings.media.AbstractMediaManager;
import com.threerings.media.MediaPanel;
import com.threerings.media.sprite.Sprite;
import com.threerings.media.animation.Animation;
import com.threerings.media.animation.AnimationAdapter;
import com.threerings.media.animation.AnimationObserver;

/**
 * A Sprite that wraps an animation so that a sequence of frames can be easily
 *  moved around the screen.  Animations embedded here should not be added
 *  directly to a media panel as this sprite will manage them.  If the enclosed
 *  animation completes, we remove the sprite from the media panel, since the
 *  animation would normally do that itself.
 */
public class AnimationSprite extends Sprite
{
    public AnimationSprite (Animation anim, MediaPanel owner)
    {
        super();
        _anim = anim;
        _owner = owner;
    }

    public void init ()
    {
        _anim.init(_mgr);
    }

    // documentation inherited
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

    // documentation inherited.
    public void setLocation (int x, int y)
    {
        _anim.setLocation(x, y);
        super.setLocation(x, y);
    }

    // documentation inherited
    public void willStart (long tickStamp)
    {
        super.willStart(tickStamp);
        _anim.willStart(tickStamp);
    }

    // documentation inherited
    public void paint (Graphics2D gfx) {
        // Nothing to paint for ourselves.

        _anim.paint(gfx);
    }

    protected Animation _anim;
    protected MediaPanel _owner;
}
