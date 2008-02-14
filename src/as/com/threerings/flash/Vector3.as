//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2007 Three Rings Design, Inc., All Rights Reserved
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

package com.threerings.flash {
    

/**
 * Basic 3D vector implementation.
 */
public class Vector3
{
    /**
     * Infinite vector - often the result of normalizing a zero vector, or intersecting
     * a vector with a parallel plane.
     */
    public static const INFINITE :Vector3 = new Vector3(Infinity, Infinity, Infinity);

    /** Vector components. */
    public var x :Number = 0;
    public var y :Number = 0;
    public var z :Number = 0;

    /** Creates a new vector. All three X, Y, Z parameters are optional. */
    public function Vector3 (x :Number = 0, y :Number = 0, z :Number = 0)
    {
        set(x, y, z);
    }

    /** Assigns values to the three parameters of this vector. */
    public function set (x :Number, y :Number, z :Number) :void
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** Duplicates a vector. */
    public function clone() :Vector3
    {
        return new Vector3(this.x, this.y, this.z);
    }

    /** Returns this vector's length. */
    public function get length () :Number
    {
        return Math.sqrt(x * x + y * y + z * z);
    }

    /** Returns the dot product of this vector with vector v. */
    public function dot (v :Vector3) :Number
    {
        return x * v.x + y * v.y + z * v.z;
    }

    /** Returns a new vector that is a normalized version of this vector. */
    public function normalize () :Vector3
    {
        return this.clone().normalizeLocal();
    }

    /** Destructively normalizes this vector. Returns a reference to the modified self. */
    public function normalizeLocal () :Vector3
    {
        var len :Number = this.length;
        set(x / len, y / len, z / len);
        return this;
    }
    
    /**
     * Returns a new vector that is the cross product of this vector with vector v,
     * such that <code>result = this &#8855; v</code>.
     */
    public function cross (v :Vector3) :Vector3
    {
        return this.clone().crossLocal(v);
    }

    /**
     * Sets this vector to the result of a cross product with vector v, such that
     * <code>this = this &#8855; v</code>. Returns a reference to the modified self.
     */
    public function crossLocal (v :Vector3) :Vector3
    {
        var xx :Number = y * v.z - z * v.y;
        var yy :Number = z * v.x - x * v.z;
        var zz :Number = x * v.y - y * v.x;
        set(xx, yy, zz);

        return this;
    }

    /** Returns a new vector that is the summation of this vector with vector v. */
    public function add (v :Vector3) :Vector3
    {
        return this.clone().addLocal(v);
    }

    /**
     * Sets this vector to the result of summation with v, such that <code>this = this + v</code>.
     * Returns a reference to the modified self.
     */
    public function addLocal (v :Vector3) :Vector3
    {
        set(x + v.x, y + v.y, z + v.z);
        return this;
    }

    /** Returns a new vector that is the subtraction of vector v from this vector.*/
    public function subtract (v :Vector3) :Vector3
    {
        return this.clone().subtractLocal(v);
    }

    /**
     * Sets the vector to the result of subtraction of v, such that <code>this = this - v</code>.
     * Returns a reference to the modified self.
     */
    public function subtractLocal (v :Vector3) :Vector3
    {
        set(x - v.x, y - v.y, z - v.z);
        return this;
    }

    /**
     * Finds the intersection of a ray emitted from s along this vector,
     * with a plane passing through point p with normal n. Returns the point
     * of intersection, potentially infinite if the ray and plane are parallel.
     */
    public function intersection (s :Vector3, p :Vector3, n :Vector3) :Vector3
    {
        // formula: given ray from /s/ along vector /this/, and a plane passing
        // through /p/ with normal /n/, we find intersection parameter as:
        //   r = (n dot this) / (n dot (p - s))
        // and the intersection point is:
        //   s' = s + r * this
        var rray :Number = p.subtract(s).dot(n);
        var rplane :Number = this.dot(n);
        if (rplane == 0) {
            return INFINITE; // the two shall never meet
        } else {
            var r :Number = rray / rplane;
            return s.add(this.scale(r));
        }
    }
    
    /**
     * Returns a new vector that is the result of multiplying the current vector
     * by the specified scalar.
     */
    public function scale (value :Number) :Vector3
    {
        return this.clone().scaleLocal(value);
    }

    /**
     * Destructively multiplies this vector by the specified scalar.
     * Returns a reference to the modified self.
     */
    public function scaleLocal (value :Number) :Vector3
    {
        set(x * value, y * value, z * value);
        return this;
    }
        
    /**
     * Returns a new vector that is a copy of this vector, with each coordinate clamped
     * to within [0, 1]. Please note that this obviously does not preserve the
     * vector's original direction in space.
     */
    public function clampToUnitBox () :Vector3
    {
        return new Vector3(Math.min(Math.max(x, 0), 1), 
                           Math.min(Math.max(y, 0), 1),
                           Math.min(Math.max(z, 0), 1));
    }

    /**
     * Returns a new vector that is the linear interpolation of vectors a and b
     * at proportion p, where p is in [0, 1], p = 0 means the result is equal to a,
     * and p = 1 means the result is equal to b.
     */
    public static function interpolate (a :Vector3, b :Vector3, p :Number) :Vector3
    {
        // todo: maybe convert this into a non-static function, to fit the rest of the class?
        var q :Number = 1 - p;
        return new Vector3(q * a.x + p * b.x,
                           q * a.y + p * b.y,
                           q * a.z + p * b.z);
    }


    public function toString () :String
    {
        return "[" + x + ", " + y + ", " + z + "]";
    }
}
}
