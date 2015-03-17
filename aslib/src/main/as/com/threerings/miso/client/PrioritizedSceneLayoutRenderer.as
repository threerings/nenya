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

package com.threerings.miso.client {

import flash.utils.Dictionary;
import flash.utils.getTimer;

import as3isolib.display.renderers.ISceneLayoutRenderer;
import as3isolib.bounds.IBounds;
import as3isolib.core.as3isolib_internal;
import as3isolib.core.IsoDisplayObject;
import as3isolib.display.scene.IIsoScene;

/**
 * Adapted from the as3isolib DefaultSceneLayoutRenderer, but thanks to its private vars, I cannot
 *  simply extend that class.
 */
public class PrioritizedSceneLayoutRenderer
    implements ISceneLayoutRenderer
{
    /**
     * Places all of our children in their appropriate rendering order based on their iso bounds
     *  and as necessary, by priority.  Priority only comes into play if the iso bounds overlap.
     */
    public function renderScene (scene:IIsoScene):void
    {
        // This is a little brute-force.  Some suggestions from DefaultSceneLayoutRenderer:
        // - cache dependencies between frames, only adjust invalidated objects, keeping old
        //   ordering as best as possible
        // - screen space subdivision to limit dependency scan
        // - set the invalidated children first, then do a rescan to make sure everything else is
        //   where it needs to be, too?  probably need to order the invalidated children sets from
        //   low to high index

        // Reset everything for this rendering pass.
        _scene = scene;
        _dependencies = new Dictionary();
        _depth = 0;
        _visited = new Dictionary();

        var startTime:uint = getTimer();

        // Use the non-rearranging display list so that the dependency sort will tend to
        //  create similar output each pass
        var children:Array = _scene.displayListChildren;

        // Full naive cartesian scan, see what objects are behind/in front of child[ii]
        var max:uint = children.length;
        for (var ii :uint = 0; ii < max; ii++)
        {
            var objA:IsoDisplayObject = children[ii];

            var rightA:Number = objA.x + objA.width;
            var frontA:Number = objA.y + objA.length;
            var topA:Number = objA.z + objA.height;

            var prioA :int = objA is PriorityIsoDisplayObject ?
                PriorityIsoDisplayObject(objA).getPriority() : 0;

            for (var jj :uint = ii + 1; jj < max; jj++)
            {
                var objB:IsoDisplayObject = children[jj];

                var prioB :int = objB is PriorityIsoDisplayObject ?
                    PriorityIsoDisplayObject(objB).getPriority() : 0;

                var rightB:Number = objB.x + objB.width;
                var frontB:Number = objB.y + objB.length;
                var topB:Number = objB.z + objB.height;

                // See if B should go behind A and vice-versa.
                var bBehindA :Boolean = ((objB.x < rightA) &&
                                         (objB.y < frontA) &&
                                         (objB.z < topA));
                var aBehindB :Boolean = ((objA.x < rightB) &&
                                         (objA.y < frontB) &&
                                         (objA.z < topB));

                if ((bBehindA && aBehindB)) {
                    // Overlap means we need to use the priority...
                    if (prioA > prioB) {
                        addDependency(objA, objB);
                    } else {
                        addDependency(objB, objA);
                    }
                } else if (bBehindA) {
                    addDependency(objA, objB);
                } else if (aBehindB) {
                    addDependency(objB, objA);
                }
                // Note - if we find that neither is behind the other, we don't add a dependency
                //  ordering.  If they actually do visually overlap due to sticking outside their
                //  bounds, this can sometimes cause inconsistent visuals.
            }
        }

        //trace("dependency scan time", getTimer() - startTime, "ms");

        // Set the childrens' depth, using dependency ordering
        for each (var obj:IsoDisplayObject in children) {
            if (!_visited[obj]) {
                place(obj);
            }
        }

        // Clear these out so we're not retaining memory between calls
        _visited = null;
        _dependencies = null;

        //trace("scene layout render time", getTimer() - startTime, "ms (manual sort)");
    }

    /** Not used. */
    public function get collisionDetection () :Function {
        return null;
    }
    public function set collisionDetection (fn :Function) :void {
    }

    /**
     * Adds the front-to-back dependency to our map of all such dependencies.
     */
    protected function addDependency (front :IsoDisplayObject, back :IsoDisplayObject) :void
    {
        var deps :Array = _dependencies[front];
        if (deps == null) {
            deps = [];
            _dependencies[front] = deps;
        }
        deps.push(back);
    }

    /**
     * Dependency-ordered depth placement of the given objects and its dependencies.
     */
    protected function place (obj:IsoDisplayObject):void
    {
        _visited[obj] = true;

        for each (var inner:IsoDisplayObject in _dependencies[obj]) {
            if (!_visited[inner]) {
                place(inner);
            }
        }

        if (_depth != obj.depth)
        {
            _scene.setChildIndex(obj, _depth);
        }
        _depth++;
    };

    /** The depth we're currently placing children at. */
    protected var _depth :uint;

    /** Any objects we've already placed. */
    protected var _visited :Dictionary;

    /** The scene on which we're operating. */
    protected var _scene :IIsoScene;

    /** All the front-back dependencies we know about.  This maps an object to a list of all
     * the objects that should be rendered behind (and therefore before) it. */
    protected var _dependencies :Dictionary;
}
}
