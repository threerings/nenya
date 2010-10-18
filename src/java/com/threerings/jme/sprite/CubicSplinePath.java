//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
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

package com.threerings.jme.sprite;

import com.jme.math.Vector3f;

/**
 * Moves a sprite along an interpolated spline based on a series of control
 * points.
 */
public class CubicSplinePath extends Path
{
    /**
     * Creates a path for the supplied sprite traversing the supplied
     * series of points with the specified duration between points.  The
     * path assumes that all the control points exist on either the same
     * x or y axis.  The non-fixed x/y axis must be in order (either ascending
     * or descending), and the z values will be interpolated.
     *
     * @param points a list of points to interpolate between.  Currently
     * all the points along one dimension must be equal.
     * @param durations defines the elapsed time between each successive
     * traversal.  This will as a result be shorter by one element than the
     * points array.
     */
    public CubicSplinePath (
            Sprite sprite, Vector3f[] points, float[] durations)
    {
        super(sprite);
        _isX = (points[0].x != points[1].x);
        _x = new float[points.length];
        _y = new float[points.length];
        _z = (_isX ? points[0].y : points[0].x);
        for (int ii = 0; ii < points.length; ii++) {
            _x[ii] = (_isX ? points[ii].x : points[ii].y);
            _y[ii] = points[ii].z;
        }
        _end = points[points.length - 1];
        _durations = durations;
        calculateDerivatives();
    }

    // documentation inherited
    public void update (float time)
    {
        // note the accumulated time
        _accum += time;

        // if we have surpassed the time for this segment, subtract the
        // segment time and move on to the next segment
        while (_current < _durations.length && _accum > _durations[_current]) {
            _accum -= _durations[_current];
            _current++;
        }

        // if we have completed out path, move the sprite to the final
        // position and wrap everything up
        if (_current >= _durations.length) {
            _sprite.setLocalTranslation(_end);
            _sprite.pathCompleted();
            return;
        }

        // move the sprite to the appropriate position between points
        _sprite.getLocalTranslation().set(interpolate(0f));
    }

    /**
     * Calculates the second derivative values for all the control points
     * along the path.
     */
    protected void calculateDerivatives ()
    {
        float[] u = new float[_x.length];
        _y2 = new float[_x.length];
        for (int ii = 1; ii < _x.length - 1; ii++) {
            float sig = (_x[ii] - _x[ii-1])/(_x[ii+1] - _x[ii-1]);
            float p = sig * _y2[ii-1] + 2;
            _y2[ii] = (sig - 1f) / p;
            u[ii] = (6f * ((_y[ii+1] - _y[ii]) / (_x[ii+1] - _x[ii]) -
                        (_y[ii] - _y[ii-1]) / (_x[ii] - _x[ii-1])) /
                    (_x[ii+1] - _x[ii-1]) - sig * u[ii-1]) / p;
        }
        for (int ii = _x.length - 2; ii >= 0; ii--) {
            _y2[ii] = _y2[ii] * _y2[ii + 1] + u[ii];
        }
    }

    /**
     * Interpolates the z value based on the current time point on the path.
     *
     * @param delta is the difference from the current time point to
     * interpolate the point.
     */
    protected Vector3f interpolate (float delta)
    {
        int idx = _current;
        // no change, just use the current value
        if (delta == 0f) {
            delta = _accum;

        // find the control point after the current one for interpolating
        } else if (delta + _accum > _durations[idx]) {
            delta -= _durations[idx] - _accum;
            while (idx < _durations.length - 1) {
                idx++;
                if (delta < _durations[idx]) {
                    break;
                }
                delta -= _durations[idx];
            }
            if (delta > _durations[idx] || idx == _durations.length - 1) {
                return _end;
            }

        // find the control point before the current one for interpolating
        } else if (_accum + delta < 0) {
            delta += _accum;
            while (idx > 0) {
                idx--;
                delta += _durations[idx];
                if (delta > 0) {
                    break;
                }
            }
            if (delta < 0) {
                delta = 0;
            }

        // we're using the same control point, simply adjust the time point
        } else {
            delta += _accum;
        }

        float h = _x[idx + 1] - _x[idx];
        // no different between these points, just return the current value
        if (h == 0) {
            if (_sprite != null) {
                return new Vector3f(_sprite.getLocalTranslation());
            }
            return new Vector3f();
        }

        float x = _x[idx] + h * (delta / _durations[idx]);
        float a = (_x[idx + 1] - x) / h;
        float b = (x - _x[idx]) / h;
        float a3 = a * a * a, b3 = b * b * b;
        float y = a * _y[idx] + b * _y[idx + 1] +
            ((a3 - a) * _y2[idx] + (b3 - b) * _y2[idx + 1]) *
            (h * h) / 6;
        float z = y;
        if (_isX) {
            y = _z;
        } else {
            y = x;
            x = _z;
        }
        return new Vector3f(x, y, z);
    }

    /** The final path point */
    protected Vector3f _end;
    /** The array of control points, derivatives and durations. */
    protected float[] _x, _y, _y2, _durations;

    protected float _accum, _z;
    protected int _current;
    protected boolean _isX;
}
