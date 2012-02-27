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

package com.threerings.cast {

import flash.geom.Point;

import com.threerings.util.Hashable;
import com.threerings.util.Log;
import com.threerings.util.Map;
import com.threerings.util.StringUtil;
import com.threerings.util.Maps;
import com.threerings.util.maps.LRMap;

/**
 * The character manager provides facilities for constructing sprites that
 * are used to represent characters in a scene. It also handles the
 * compositing and caching of composited character animations.
 */
public class CharacterManager
{
    private static var log :Log = Log.getLog(CharacterManager);

    /**
     * Constructs the character manager.
     */
    public function CharacterManager (crepo :ComponentRepository)
    {
        // keep this around
        _crepo = crepo;

        for each (var action :ActionSequence in crepo.getActionSequences()) {
            _actions.put(action.name, action);
        }
    }

    /**
     * Returns a {@link CharacterSprite} representing the character
     * described by the given {@link CharacterDescriptor}, or
     * <code>null</code> if an error occurs.
     *
     * @param desc the character descriptor.
     */
    public function getCharacter (desc :CharacterDescriptor,
        charClass :Class = null) :CharacterSprite
    {
        if (charClass == null) {
            charClass = _charClass;
        }

        try {
            var sprite :CharacterSprite = new charClass();
            sprite.init(desc, this);
            return sprite;

        } catch (e :Error) {
            log.warning("Failed to instantiate character sprite.", e);
            return null;
        }
        return getCharacter(desc, charClass);
    }

    public function getActionSequence (action :String) :ActionSequence
    {
        return _actions.get(action);
    }

    public function getComponent (compId :int) :CharacterComponent
    {
        return _crepo.getComponent(compId);
    }

    public function getComponentByName (cclass :String, cname :String) :CharacterComponent
    {
        return _crepo.getComponentByName(cclass, cname);
    }

    public function getActionFrames (descrip :CharacterDescriptor, action :String) :ActionFrames
    {
        if (!isLoaded(descrip)) {
            return null;
        }

        var key :FrameKey = new FrameKey(descrip, action);

        var frames :ActionFrames = _actionFrames.get(key);
        if (frames == null) {
            // this doesn't actually composite the images, but prepares an
            // object to be able to do so
            frames = createCompositeFrames(descrip, action);
            _actionFrames.put(key, frames);
        }

        return frames;
    }

    /**
     * Returns whether all the components are loaded and ready to go.
     */
    protected function isLoaded (descrip :CharacterDescriptor) :Boolean
    {
        var cids :Array = descrip.getComponentIds();
        var ccount :int = cids.length;

        for (var ii :int = 0; ii < ccount; ii++) {
            var ccomp :CharacterComponent = _crepo.getComponent(cids[ii]);
            if (!ccomp.isLoaded()) {
                return false;
            }
        }

        return true;
    }

    public function load (descrip :CharacterDescriptor, notify :Function) :void
    {
        _crepo.load(descrip.getComponentIds(), notify);
    }

    protected function createCompositeFrames (descrip :CharacterDescriptor,
        action :String) :ActionFrames
    {
        var cids :Array = descrip.getComponentIds();
        var ccount :int = cids.length;

        var zations :Array = descrip.getColorizations();
        var xlations :Array = descrip.getTranslations();

        log.debug("Compositing action [action=" + action +
                  ", descrip=" + descrip + "].");

        // this will be used to construct any shadow layers
        var shadows :Map = null; /* of String, Array<TranslatedComponent> */

        // maps components by class name for masks
        var ccomps :Map = Maps.newMapOf(String); /* of String, ArrayList<TranslatedComponent> */

        // create colorized versions of all of the source action frames
        var sources :Array = [];
        for (var ii :int = 0; ii < ccount; ii++) {
            var cframes :ComponentFrames = new ComponentFrames();
            sources.push(cframes);
            var ccomp :CharacterComponent = cframes.ccomp = _crepo.getComponent(cids[ii]);

            // load up the main component images
            var source :ActionFrames = ccomp.getFrames(action, null);
            if (source == null) {
                var errmsg :String = "Cannot composite action frames; no such " +
                    "action for component [action=" + action +
                    ", desc=" + descrip + ", comp=" + ccomp + "]";
                throw new Error(errmsg);
            }
            source = (zations == null || zations[ii] == null) ?
                source : source.cloneColorized(zations[ii]);
            var xlation :Point = (xlations == null) ? null : xlations[ii];
            cframes.frames = (xlation == null) ?
                source : source.cloneTranslated(xlation.x, xlation.y);

            // store the component with its translation under its class for masking
            var tcomp :TranslatedComponent = new TranslatedComponent(ccomp, xlation);
            var tcomps :Array = ccomps.get(ccomp.componentClass.name);
            if (tcomps == null) {
                ccomps.put(ccomp.componentClass.name, tcomps = []);
            }
            tcomps.push(tcomp);

            // if this component has a shadow, make a note of it
            if (ccomp.componentClass.isShadowed()) {
                if (shadows == null) {
                    shadows = Maps.newMapOf(String);
                }
                var shadlist :Array = shadows.get(ccomp.componentClass.shadow);
                if (shadlist == null) {
                    shadows.put(ccomp.componentClass.shadow, shadlist = []);
                }
                shadlist.push(tcomp);
            }
        }

/* TODO - Implement shadows & masks - I don't actually know if/where we use these, though.
        // now create any necessary shadow layers
        if (shadows != null) {
            for (Map.Entry<String, ArrayList<TranslatedComponent>> entry : shadows.entrySet()) {
                ComponentFrames scf = compositeShadow(action, entry.getKey(), entry.getValue());
                if (scf != null) {
                    sources.add(scf);
                }
            }
        }

        // add any necessary masks
        for (ComponentFrames cframes : sources) {
            ArrayList<TranslatedComponent> mcomps = ccomps.get(cframes.ccomp.componentClass.mask);
            if (mcomps != null) {
                cframes.frames = compositeMask(action, cframes.ccomp, cframes.frames, mcomps);
            }
        }
*/
        // use those to create an entity that will lazily composite things
        // together as they are needed
        return new CompositedActionFrames(_frameCache, _actions.get(action), sources);
    }

    protected var _crepo :ComponentRepository;

    protected var _actions :Map = Maps.newMapOf(String);

    protected var _actionFrames :Map = Maps.newMapOf(FrameKey);

    protected var _frameCache :LRMap = new LRMap(Maps.newMapOf(CompositedFramesKey), MAX_FRAMES);

    protected var _charClass :Class;

    protected static const MAX_FRAMES :int = 1000;
}
}

import com.threerings.util.Hashable;
import com.threerings.util.StringUtil;
import com.threerings.cast.CharacterDescriptor;

class FrameKey
    implements Hashable
{
    public var desc :CharacterDescriptor;

    public var action :String;

    public function FrameKey (desc :CharacterDescriptor, action :String)
    {
        this.desc = desc;
        this.action = action;
    }

    public function hashCode () :int
    {
        return desc.hashCode() ^ StringUtil.hashCode(action);
    }

    public function equals (other :Object) :Boolean
    {
        if (other is FrameKey) {
            var okey :FrameKey = FrameKey(other);
            return okey.desc.equals(desc) && okey.action == action;
        } else {
            return false;
        }
    }
}

import flash.geom.Point;

import com.threerings.cast.ActionFrames;
import com.threerings.cast.CharacterComponent;

class TranslatedComponent
{
    public var ccomp :CharacterComponent;
    public var xlation :Point;

    public function TranslatedComponent (ccomp :CharacterComponent, xlation :Point)
    {
        this.ccomp = ccomp;
        this.xlation = xlation;
    }

    public function getFrames (action :String, type :String) :ActionFrames
    {
        var frames :ActionFrames = ccomp.getFrames(action, type);
        return (frames == null || xlation == null) ?
            frames : frames.cloneTranslated(xlation.x, xlation.y);
    }

}
