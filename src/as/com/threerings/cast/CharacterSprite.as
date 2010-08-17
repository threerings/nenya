//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
// http://www.threerings.net/code/nenya/
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

package com.threerings.cast {

import flash.display.DisplayObject;
import flash.display.Sprite;

import flash.geom.Point;
import flash.geom.Rectangle;

import com.threerings.display.DisplayUtil;
import com.threerings.media.Tickable;
import com.threerings.util.DirectionCodes;
import com.threerings.util.Log;

/**
 * A character sprite is a sprite that animates itself while walking
 * about in a scene.
 */
public class CharacterSprite extends Sprite
    implements Tickable
{
    private static var log :Log = Log.getLog(CharacterSprite);

    /**
     * Initializes this character sprite with the specified character
     * descriptor and character manager. It will obtain animation data
     * from the supplied character manager.
     */
    public function init (descrip :CharacterDescriptor, charmgr :CharacterManager) :void
    {
        // keep track of this stuff
        _descrip = descrip;
        _charmgr = charmgr;

        // sanity check our values
        sanityCheckDescrip();

        // assign an arbitrary starting orientation
        _orient = DirectionCodes.SOUTHWEST;

        // pass the buck to derived classes
        didInit();

        halt();

        updateActionFrames();
    }

    public function tick (tickStamp :int) :void
    {
        if (_framesBitmap != null) {
            _framesBitmap.tick(tickStamp);
        }
    }

    /**
     * Called after this sprite has been initialized with its character
     * descriptor and character manager. Derived classes can do post-init
     * business here.
     */
    protected function didInit () :void
    {
        _mainSprite = new Sprite();
        addChild(_mainSprite);
    }

    /**
     * Reconfigures this sprite to use the specified character descriptor.
     */
    public function setCharacterDescriptor (descrip :CharacterDescriptor) :void
    {
        // keep the new descriptor
        _descrip = descrip;

        // sanity check our values
        sanityCheckDescrip();

        // update our action frames
        updateActionFrames();
    }

    /**
     * Specifies the action to use when the sprite is at rest. The default
     * is <code>STANDING</code>.
     */
    public function setRestingAction (action :String) :void
    {
        _restingAction = action;
    }

    /**
     * Returns the action to be used when the sprite is at rest. Derived
     * classes may wish to override this method and vary the action based
     * on external parameters (or randomly).
     */
    public function getRestingAction () :String
    {
        return _restingAction;
    }

    /**
     * Specifies the action to use when the sprite is following a path.
     * The default is <code>WALKING</code>.
     */
    public function setFollowingPathAction (action :String) :void
    {
        _followingPathAction = action;
    }

    /**
     * Returns the action to be used when the sprite is following a path.
     * Derived classes may wish to override this method and vary the
     * action based on external parameters (or randomly).
     */
    public function getFollowingPathAction () :String
    {
        return _followingPathAction;
    }

    /**
     * Sets the action sequence used when rendering the character, from
     * the set of available sequences.
     */
    public function setActionSequence (action :String) :void
    {
        // sanity check
        if (action == null) {
            log.warning("Refusing to set null action sequence " + this + ".", new Error());
            return;
        }

        // no need to noop
        if (action == _action) {
            return;
        }
        _action = action;
        updateActionFrames();
    }

    public function setOrientation (orient :int) :void
    {
        var oorient :int = _orient;
        _orient = orient;
        if (orient < 0 || orient >= DirectionCodes.FINE_DIRECTION_COUNT) {
            log.info("Refusing to set invalid orientation [sprite=" + this +
                     ", orient=" + orient + "].", new Error());
            return;
        }

        if (_orient != oorient) {
            updateMainSprite();
        }
    }

    public function getOrientation () :int
    {
        return _orient;
    }

    public function cancelMove () :void
    {
        halt();
    }

    public function pathBeginning () :void
    {
        // enable walking animation
        setActionSequence(getFollowingPathAction());
    }

    public function pathCompleted (timestamp :int) :void
    {
        halt();
    }

    /**
     * Rebuilds our action frames given our current character descriptor
     * and action sequence. This is called when either of those two things
     * changes.
     */
    protected function updateActionFrames () :void
    {
        // get a reference to the action sequence so that we can obtain
        // our animation frames and configure our frames per second
        var actseq :ActionSequence = _charmgr.getActionSequence(_action);
        if (actseq == null) {
            var errmsg :String = "No such action '" + _action + "'.";
            throw new Error(errmsg);
        }

        try {
            // obtain our animation frames for this action sequence
            _aframes = _charmgr.getActionFrames(_descrip, _action);

            if (_aframes == null) {
                // Once the frames are really ready to go, we'll do this...
                _charmgr.load(_descrip, updateActionFrames);
            }

            updateMainSprite();

        } catch (nsce :NoSuchComponentError) {
            log.warning("Character sprite references non-existent " +
                        "component [sprite=" + this + ", err=" + nsce + "].");

        } catch (e :Error) {
            log.warning("Failed to obtain action frames [sprite=" + this +
                        ", descrip=" + _descrip + ", action=" + _action + "].", e);
        }

        x = -actseq.origin.x;
        y = -actseq.origin.y;
    }

    protected function updateMainSprite () :void
    {
        DisplayUtil.removeAllChildren(_mainSprite);
        if (_aframes != null) {
            _aframes.getFrames(_orient, function(frames :MultiFrameBitmap) :void {
                _framesBitmap = frames;
                DisplayUtil.removeAllChildren(_mainSprite);
                _mainSprite.addChild(frames);
            });
        } else {
            _framesBitmap = null;
            updateWithUnloadedSprite();
        }
    }

    protected function updateWithUnloadedSprite () :void
    {
        // Nothing by default.
    }

    /**
     * Makes it easier to track down problems with bogus character descriptors.
     */
    protected function sanityCheckDescrip () :void
    {
        if (_descrip.getComponentIds() == null ||
            _descrip.getComponentIds().length == 0) {
            log.warning("Invalid character descriptor [sprite=" + this +
                        ", descrip=" + _descrip + "].", new Error());
        }
    }

    /**
     * Updates the sprite animation frame to reflect the cessation of
     * movement and disables any further animation.
     */
    protected function halt () :void
    {
        var rest :String = getRestingAction();
        if (rest != null) {
            setActionSequence(rest);
        }
    }

    public function hitTest (stageX :int, stageY :int) :Boolean
    {
        return _framesBitmap == null ? false : _framesBitmap.hitTest(stageX, stageY);
    }

    /** The action to use when at rest. */
    protected var _restingAction :String = StandardActions.STANDING;

    /** The action to use when following a path. */
    protected var _followingPathAction :String = StandardActions.WALKING;

    /** A reference to the descriptor for the character that we're
     * visualizing. */
    protected var _descrip :CharacterDescriptor;

    /** A reference to the character manager that created us. */
    protected var _charmgr :CharacterManager;

    /** The action we are currently displaying. */
    protected var _action :String;

    /** The animation frames for the active action sequence in each
     * orientation. */
    protected var _aframes :ActionFrames;

    /** The currently active set of bitmaps for this character. */
    protected var _framesBitmap :MultiFrameBitmap;

    /** The orientation of this sprite. */
    protected var _orient :int = DirectionCodes.NONE;

    protected var _mainSprite :Sprite;
}
}