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

package com.threerings.media.util {

import flash.geom.Point;
import flash.geom.Rectangle;

import com.threerings.util.ArrayIterator;
import com.threerings.util.DirectionCodes;
import com.threerings.util.DirectionUtil;
import com.threerings.util.Iterator;
import com.threerings.util.Log;
import com.threerings.util.MathUtil;
import com.threerings.util.StringUtil;

/**
 * The line segment path is used to cause a pathable to follow a path that
 * is made up of a sequence of line segments. There must be at least two
 * nodes in any worthwhile path. The direction of the first node in the
 * path is meaningless since the pathable begins at that node and will
 * therefore never be heading towards it.
 */
public class LineSegmentPath
    implements Path
{
    private var log :Log = Log.getLog(LineSegmentPath);

    /**
     * Constructs an empty line segment path.
     */
    public function LineSegmentPath ()
    {
    }

    /**
     * Constructs a line segment path that consists of a single segment
     * connecting the point <code>(x1, y1)</code> with <code>(x2,
     * y2)</code>.  The orientation for the first node is set arbitrarily
     * and the second node is oriented based on the vector between the two
     * nodes (in top-down coordinates).
     */
    public static function createWithInts (x1 :int, y1 :int, x2 :int, y2 :int) :LineSegmentPath
    {
        var path :LineSegmentPath = new LineSegmentPath();
        path.addNode(x1, y1, DirectionCodes.NORTH);
        var p1 :Point = new Point(x1, y1);
        var p2 :Point = new Point(x2, y2);
        var dir :int = DirectionUtil.getDirectionForPts(p1, p2);
        path.addNode(x2, y2, dir);
        return path;
    }

    /**
     * Construct a line segment path between the two nodes with the
     * specified direction.
     */
    public static function createWithPoints (p1 :Point, p2 :Point, dir :int) :LineSegmentPath
    {
        var path :LineSegmentPath = new LineSegmentPath();
        path.addNode(p1.x, p1.y, DirectionCodes.NORTH);
        path.addNode(p2.x, p2.y, dir);
        return path;
    }

    /**
     * Constructs a line segment path with the specified list of points.
     * An arbitrary direction will be assigned to the starting node.
     */
    public static function createWithList (points :Array) :LineSegmentPath
    {
        var path :LineSegmentPath = new LineSegmentPath();
        path.createPath(points);
        return path;
    }

    /**
     * Returns the orientation the sprite will face at the end of the
     * path.
     */
    public function getFinalOrientation () :int
    {
        return (_nodes.length == 0) ? DirectionCodes.NORTH : _nodes[_nodes.length-1].dir;
    }

    /**
     * Add a node to the path with the specified destination point and
     * facing direction.
     *
     * @param x the x-position.
     * @param y the y-position.
     * @param dir the facing direction.
     */
    public function addNode (x :Number, y :Number, dir :int) :void
    {
        _nodes.push(new PathNode(x, y, dir));
    }

    /**
     * Return the requested node index in the path, or null if no such
     * index exists.
     *
     * @param idx the node index.
     *
     * @return the path node.
     */
    public function getNode (idx :int) :PathNode
    {
        return _nodes[idx];
    }

    /**
     * Return the number of nodes in the path.
     */
    public function size () :int
    {
        return _nodes.length;
    }

    /**
     * Sets the velocity of this pathable in pixels per millisecond. The
     * velocity is measured as pixels traversed along the path that the
     * pathable is traveling rather than in the x or y directions
     * individually.  Note that the pathable velocity should not be
     * changed while a path is being traversed; doing so may result in the
     * pathable position changing unexpectedly.
     *
     * @param velocity the pathable velocity in pixels per millisecond.
     */
    public function setVelocity (velocity :Number) :void
    {
        _vel = velocity;
    }

    /**
     * Computes the velocity at which the pathable will need to travel
     * along this path such that it will arrive at the destination in
     * approximately the specified number of milliseconds. Efforts are
     * taken to get the pathable there as close to the desired time as
     * possible, but framerate variation may prevent it from arriving
     * exactly on time.
     */
    public function setDuration (millis :int) :void
    {
        // if we have only zero or one nodes, we don't have enough
        // information to compute our velocity
        var ncount :int = _nodes.length;
        if (ncount < 2) {
            log.warning("Requested to set duration of bogus path " +
                        "[path=" + this + ", duration=" + millis + "].");
            return;
        }

        // compute the total distance along our path
        var distance :Number = 0;
        var start :PathNode = _nodes[0];
        for (var ii :int = 1; ii < ncount; ii++) {
            var end :PathNode = _nodes[ii];
            distance += MathUtil.distance(start.loc.x, start.loc.y, end.loc.x, end.loc.y);
            start = end;
        }

        // set the velocity accordingly
        setVelocity(distance/millis);
    }

    // documentation inherited
    public function init (pable :Pathable, timestamp :int) :void
    {
        // give the pathable a chance to perform any starting antics
        pable.pathBeginning();

        // if we have only one node then let the pathable know that we're
        // done straight away
        if (size() < 2) {
            // move the pathable to the location specified by the first
            // node (assuming we have a first node)
            if (size() == 1) {
                var node :PathNode = _nodes[0];
                pable.setLocation(node.loc.x, node.loc.y);
            }
            // and let the pathable know that we're done
            pable.pathCompleted(timestamp);
            return;
        }

        // and an enumeration of the path nodes
        _niter = new ArrayIterator(_nodes);

        // pretend like we were previously heading to our starting position
        _dest = getNextNode();

        // begin traversing the path
        headToNextNode(pable, timestamp, timestamp);
    }

    // documentation inherited
    public function tick (pable :Pathable, timestamp :int) :Boolean
    {
        // figure out how far along this segment we should be
        var msecs :int = timestamp - _nodestamp;
        var travpix :Number = msecs * _vel;
        var pctdone :Number = travpix / _seglength;

        // if we've moved beyond the end of the path, we need to adjust
        // the timestamp to determine how much time we used getting to the
        // end of this node, then move to the next one
        if (pctdone >= 1.0) {
            var used :int = int(_seglength / _vel);
            return headToNextNode(pable, _nodestamp + used, timestamp);
        }

        // otherwise we position the pathable along the path
        var ox :Number = pable.getX();
        var oy :Number = pable.getY();
        var nx :Number = _src.loc.x + (_dest.loc.x - _src.loc.x) * pctdone;
        var ny :Number = _src.loc.y + (_dest.loc.y - _src.loc.y) * pctdone;

//         Log.info("Moving pathable [msecs=" + msecs + ", pctdone=" + pctdone +
//                  ", travpix=" + travpix + ", seglength=" + _seglength +
//                  ", dx=" + (nx-ox) + ", dy=" + (ny-oy) + "].");

        // only update the pathable's location if it actually moved
        if (ox != nx || oy != ny) {
            pable.setLocation(nx, ny);
            return true;
        }

        return false;
    }

    // documentation inherited
    public function fastForward (timeDelta :int) :void
    {
        _nodestamp += timeDelta;
    }

    // documentation inherited from interface
    public function wasRemoved (pable :Pathable) :void
    {
        // nothing doing
    }

    /**
     * Place the pathable moving along the path at the end of the previous
     * path node, face it appropriately for the next node, and start it on
     * its way.  Returns whether the pathable position moved.
     */
    protected function headToNextNode (pable :Pathable, startstamp :int, now :int) :Boolean
    {
        if (_niter == null) {
            throw new Error("headToNextNode() called before init()");
        }

        // check to see if we've completed our path
        if (!_niter.hasNext()) {
            // move the pathable to the location of our last destination
            pable.setLocation(_dest.loc.x, _dest.loc.y);
            pable.pathCompleted(now);
            return true;
        }

        // our previous destination is now our source
        _src = _dest;

        // pop the next node off the path
        _dest = getNextNode();

        // adjust the pathable's orientation
        if (_dest.dir != DirectionCodes.NONE) {
            pable.setOrientation(_dest.dir);
        }

        // make a note of when we started traversing this node
        _nodestamp = startstamp;

        // figure out the distance from source to destination
        _seglength = MathUtil.distance(_src.loc.x, _src.loc.y, _dest.loc.x, _dest.loc.y);

        // if we're already there (the segment length is zero), we skip to
        // the next segment
        if (_seglength == 0) {
            return headToNextNode(pable, startstamp, now);
        }

        // now update the pathable's position based on our progress thus far
        return tick(pable, now);
    }

    public function toString () :String
    {
        return StringUtil.toString(_nodes);
    }

    /**
     * Populate the path with the path nodes that lead the pathable from
     * its starting position to the given destination coordinates
     * following the given list of screen coordinates.
     */
    protected function createPath (points :Array) :void
    {
        var last :Point = null;
        var size :int = points.length;
        for (var ii :int = 0; ii < size; ii++) {
            var p :Point = points[ii];

            var dir :int = (ii == 0) ? DirectionCodes.NORTH :
                DirectionUtil.getDirectionForPts(last, p);
            addNode(p.x, p.y, dir);
            last = p;
        }
    }

    /**
     * Gets the next node in the path.
     */
    protected function getNextNode () :PathNode
    {
        return PathNode(_niter.next());
    }

    /** The nodes that make up the path. */
    protected var _nodes :Array = [];

    /** We use this when moving along this path. */
    protected var _niter :Iterator;

    /** When moving, the pathable's source path node. */
    protected var _src :PathNode;

    /** When moving, the pathable's destination path node. */
    protected var _dest :PathNode;

    /** The time at which we started traversing the current node. */
    protected var _nodestamp :int;

    /** The length in pixels of the current path segment. */
    protected var _seglength :Number;

    /** The path velocity in pixels per millisecond. */
    protected var _vel :Number = DEFAULT_VELOCITY;

    /** When moving, the pathable position including fractional pixels. */
    protected var _movex :Number;
    protected var _movey :Number;

    /** When moving, the distance to move on each axis per tick. */
    protected var _incx :Number;
    protected var _incy :Number;

    /** The distance to move on the straight path line per tick. */
    protected var _fracx :Number;
    protected var _fracy :Number;

    /** Default pathable velocity. */
    protected static const DEFAULT_VELOCITY :Number= 0.2;
}
}
