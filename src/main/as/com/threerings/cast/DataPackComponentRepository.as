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

import flash.events.Event;

import com.threerings.cast.ComponentDataPack;
import com.threerings.media.tile.TileDataPack;
import com.threerings.util.ArrayUtil;
import com.threerings.util.DataPack;
import com.threerings.util.Log;
import com.threerings.util.Map;
import com.threerings.util.Maps;
import com.threerings.util.StringUtil;

public class DataPackComponentRepository
    implements ComponentRepository
{
    private static var log :Log = Log.getLog(DataPackComponentRepository);

    public function DataPackComponentRepository (compUrl :String)
    {
        _rootUrl = compUrl;
        loadComponentMeta(compUrl + "metadata.jar");
    }

    protected function loadComponentMeta (compMetaUrl :String) :void
    {
        _metaPack = new TileDataPack(compMetaUrl, metaLoadingComplete);
    }

    /**
     * Handle the successful completion of datapack loading.
     *
     * @private
     */
    protected function metaLoadingComplete (event :Event) :void
    {
        var actionXml :XML = _metaPack.getFileAsXML("actions.xml");
        var classesXml :XML = _metaPack.getFileAsXML("classes.xml");

        for each (var aXml :XML in actionXml.action) {
            var action :ActionSequence = ActionSequence.fromXml(aXml);
            _actions.put(action.name, action);
        }

        for each (var cXml :XML in classesXml.elements("class")) {
            var compClass :ComponentClass = ComponentClass.fromXml(cXml);
            _classes.put(compClass.name, compClass);
        }

        parseComponentMap(_metaPack.getFileAsString("compmap.txt"));

        // Notify them all and clear our list.
        for each (var func :Function in _notifyOnLoad) {
            func();
        }
        _notifyOnLoad = [];
    }

    protected function parseComponentMap (map :String) :void
    {
        var lines :Array = map.split("\n");
        var tileCount :int = int(lines[0]);
        for (var ii :int = 1; ii < lines.length; ii++) {
            var line :String = lines[ii];
            var toks :Array = line.split(" := ");
            if (toks.length >= 3) {
                var id :int = int(toks[2]);
                var cclass :String = String(toks[0]);
                var cname :String = String(toks[1]);
                createComponent(id, cclass, cname);
            }
        }
    }

    /**
     * Creates a component and inserts it into the component table.
     */
    protected function createComponent (componentId :int, cclass :String, cname :String) :void
    {
        // look up the component class information
        var clazz :ComponentClass = _classes.get(cclass);
        if (clazz == null) {
            log.warning("Non-existent component class",
                "class", cclass, "name", cname, "id", componentId);
            return;
        }

        // create the component - we'll set the frame provider as soon as we can.
        var component :CharacterComponent = new CharacterComponent(componentId, cname, clazz, null);

        // stick it into the appropriate tables
        _components.put(componentId, component);

        // we have a hash of lists for mapping components by class/name
        var comps :Array = _classComps.get(cclass);
        if (comps == null) {
            comps = [];
            _classComps.put(cclass, comps);
        }
        if (!ArrayUtil.contains(comps, component)) {
            comps.push(component);
        } else {
            log.info("Requested to register the same component twice?", "comp", component);
        }
    }

    public function notifyOnLoad (func :Function) :void
    {
        _notifyOnLoad.push(func);
    }

    /**
     * Returns the {@link CharacterComponent} object for the given component identifier.
     */
    public function getComponent (componentId :int) :CharacterComponent
    {
        var component :CharacterComponent = _components.get(componentId);
        if (component == null) {
            throw new NoSuchComponentError(componentId);
        }
        return component;
    }

    /**
     * Returns the {@link CharacterComponent} object with the given component class and name.
     */
    public function getComponentByName (className :String, compName :String) :CharacterComponent
    {
        // look up the list for that class
        var comps :Array = _classComps.get(className);
        if (comps != null) {
            for each (var comp :CharacterComponent in comps) {
                if (comp.name == compName) {
                    return comp;
                }
            }
        }
        throw new NoSuchComponentError(className + "/" + compName);
    }

    public function load (compIds :Array, notify :Function) :void
    {
        var complete :Boolean = true;

        var toLoad :Array = [];

        for each (var cid :int in compIds) {
            var comp :CharacterComponent = getComponent(cid);
            if (!comp.isLoaded()) {
                toLoad.push(comp);
            }
        }

        var waiting :int = toLoad.length;

        if (waiting == 0) {
            notify();
        } else {
            for each (var load :CharacterComponent in toLoad) {
                load.notifyOnLoad(function() :void {
                    waiting--;
                    if (waiting == 0) {
                        notify();
                    }
                });
                loadComponent(load);
            }
        }
    }

    protected function loadComponent (comp :CharacterComponent) :void
    {
        var packName :String = comp.componentClass.name + "/" + comp.name;
        var pack :ComponentDataPack = _packs.get(packName);
        if (pack == null || !pack.isComplete()) {
            loadPack(packName, comp);
        } else {
            comp.setFrameProvider(pack);
        }
    }

    protected function loadPack (packName :String, comp :CharacterComponent) :void
    {
        var pack :ComponentDataPack = _packs.get(packName);

        if (pack != null && pack.isComplete()) {
            // Already loaded
            return;
        }

        var completeListener :Function = function () :void {
            comp.setFrameProvider(_packs.get(packName));
        }

        var listeners :Array = _packListeners.get(packName);
        if (listeners == null) {
            listeners = [];
            _packListeners.put(packName, listeners);

            var packCompleteListener :Function = function () :void {
                for each (var listener :Function in listeners) {
                    listener();
                }
                _packListeners.remove(packName);
            }

            pack = new ComponentDataPack(getPackUrl(packName), _actions, packCompleteListener);
            _packs.put(packName, pack);
        }

        listeners.push(completeListener);
    }

    protected function getPackUrl (packName :String) :String
    {
        return _rootUrl + packName + "/components.jar";
    }

    /**
     * Returns the {@link ComponentClass} with the specified name or null if none exists with that
     * name.
     */
    public function getComponentClass (className :String) :ComponentClass
    {
        return _classes.get(className);
    }

    /**
     * Iterates over the {@link ComponentClass} instances representing all available character
     * component classes.
     */
    public function getComponentClasses () :Array
    {
        return _classes.values();
    }

    /**
     * Iterates over the {@link ActionSequence} instances representing every available action
     * sequence.
     */
    public function getActionSequences () :Array
    {
        return _actions.values();
    }

    /**
     * Iterates over the component ids of all components in the specified class.
     */
    public function getComponentIds (compClass :ComponentClass) :Array
    {
        var result :Array = [];

        _classes.forEach(function (key :int, value :CharacterComponent) :Boolean {
            if (value.componentClass.equals(compClass)) {
                result.push(key);
            }
            return false;
        });

        return result;
    }

    /** Everyone who cares when we're loaded. */
    protected var _notifyOnLoad :Array = [];

    /** Contains the component meta-data for all components. */
    protected var _metaPack : DataPack;

    /** The URL for our components. */
    protected var _rootUrl :String;

    /** Map of pack name to listeners waiting for that pack to resolve. */
    protected var _packListeners :Map = Maps.newMapOf(String);

    /** The map of packs we've already loaded up and resolved. */
    protected var _packs :Map = Maps.newMapOf(String);

    /** All our component classes by name. */
    protected var _classes :Map = Maps.newMapOf(String);

    /** All our action sequences by name. */
    protected var _actions :Map = Maps.newMapOf(String);

    /** The map of componentId to CharacterComponent */
    protected var _components :Map = Maps.newMapOf(int);

    /** A table of component lists indexed on classname. */
    protected var _classComps :Map = Maps.newMapOf(String);
}
}