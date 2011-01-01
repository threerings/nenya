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

package com.threerings.cast {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.DisplayObject;

import flash.geom.Point;
import flash.geom.Rectangle;

import flash.events.Event;

import flash.utils.ByteArray;

import nochump.util.zip.ZipEntry;
import nochump.util.zip.ZipError;
import nochump.util.zip.ZipFile;

import com.threerings.media.image.PngRecolorUtil;
import com.threerings.media.tile.TileDataPack;
import com.threerings.media.tile.TileSet;
import com.threerings.util.DataPack;
import com.threerings.util.Log;
import com.threerings.util.Map;
import com.threerings.util.Maps;
import com.threerings.util.MultiLoader;
import com.threerings.util.Set;
import com.threerings.util.StringUtil;

/**
 * Like a normal data pack, but we don't deal with any data, and all our filenames are a direct
 *  translation, so we need no metadata file.  We also serve as a tile ImageProvider.
 */
public class ComponentDataPack extends TileDataPack
    implements FrameProvider
{
    private static var log :Log = Log.getLog(ComponentDataPack);

    public function ComponentDataPack (source :Object, actions :Map,
        completeListener :Function = null, errorListener :Function = null)
    {
        super(source, completeListener, errorListener);
        _actions = actions;
    }

    // Documentation inherited from interface
    public function getFrames (component :CharacterComponent, action :String,
        type :String) :ActionFrames
    {
        // obtain the action sequence definition for this action
        var actseq :ActionSequence = _actions.get(action);
        if (actseq == null) {
            log.warning("Missing action sequence definition [action=" + action +
                        ", component=" + component + "].");
            return null;
        }

        // determine our image path name
        var imgpath :String = action;
        var dimgpath :String = ActionSequence.DEFAULT_SEQUENCE;
        if (type != null) {
            imgpath += "_" + type;
            dimgpath += "_" + type;
        }

        var root :String = component.componentClass.name + "/" + component.name + "/";
        var cpath :String = root + imgpath + ".png";
        var dpath :String = root + dimgpath + ".png";

        // look to see if this tileset is already cached (as the custom action or the default
        // action)
        var aset :TileSet = _setcache.get(cpath);
        if (aset == null) {
            aset = _setcache.get(dpath);
            if (aset != null) {
                // save ourselves a lookup next time
                _setcache.put(cpath, aset);
            }
        }

        try {
            // then try loading up a tileset customized for this action
            if (aset == null) {
                if (_zip.getEntry(cpath)) {
                    aset = TileSet(actseq.tileset.clone());
                    aset.setImagePath(cpath);
                } else if (_zip.getEntry(dpath)) {
                    actseq = _actions.get(ActionSequence.DEFAULT_SEQUENCE);
                    aset = TileSet(actseq.tileset.clone());
                    aset.setImagePath(dpath);
                    _setcache.put(dpath, aset);
                }
            }

            // if that failed too, we're hosed
            if (aset == null) {
                // if this is a shadow or crop image, no need to freak out as they are optional
                if (!StandardActions.CROP_TYPE == type &&
                    !StandardActions.SHADOW_TYPE == type) {
                    log.warning("Unable to locate tileset for action '" + imgpath + "' " +
                                component + ".");
                }
                return null;
            }

            aset.setImageProvider(this);
            _setcache.put(cpath, aset);
            return new TileSetFrameImage(aset, actseq);

        } catch (e :Error) {
            log.warning("Error loading tset for action '" + imgpath + "' " + component + ".", e);
        }

        return null;
    }

    // Documentation inherited from interface
    public function getFramePath (
        component :CharacterComponent, action :String, type :String, existentPaths :Set) :String
    {
        var actionPath :String = makePath(component, action, type);
        if (!existentPaths.contains(actionPath)) {
            return makePath(component, ActionSequence.DEFAULT_SEQUENCE, type);
        }
        return actionPath;
    }

    protected function makePath (component :CharacterComponent, action :String,
        type :String) :String
    {
        var imgpath :String = action;
        if (type != null) {
            imgpath += "_" + type;
        }
        var root :String = component.componentClass.name + "/" + component.name + "/";
        return root + imgpath + ".png";
    }

    protected var _actions :Map;

    protected var _setcache :Map = Maps.newMapOf(String);
}
}