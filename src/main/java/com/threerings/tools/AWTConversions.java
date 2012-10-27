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

package com.threerings.tools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import net.sf.json.JSONArray;

import com.threerings.tools.JSONConversion.Config;
import com.threerings.tools.JSONConversion.Converter;

/**
 * Defines some common formats for converting nenya's uses of java.awt geometric types to json.
 */
public class AWTConversions
{
    /**
     * Adds all our conversion to the given config and returns it for chaining.
     */
    public static Config addAll (Config config)
    {
        return config.
            addClassConverter(Rectangle.class, RECTANGLE).
            addClassConverter(Point.class, POINT).
            addClassConverter(Dimension.class, DIMENSION).
            addClassConverter(Color.class, COLOR);
    }

    /**
     * Converts instances of color to a standard RGB byte triplet.
     */
    public static final Converter<Color> COLOR = new Converter<Color>() {
        public Object convert (Color c, Config cfg) {
            return new Integer((c.getRed() << 16) | (c.getGreen()<<8) | c.getBlue());
        }
    };

    /**
     * Converts a rectangle into an json array with entries x, y, width, height.
     */
    public static final Converter<Rectangle> RECTANGLE = new Converter<Rectangle>() {
        public Object convert (Rectangle r, Config cfg) {
            JSONArray out = new JSONArray();
            out.add(r.x);
            out.add(r.y);
            out.add(r.width);
            out.add(r.height);
            return out;
        }
    };

    /**
     * Converts a point into a json array with entries x, y.
     */
    public static final Converter<Point> POINT = new Converter<Point>() {
        public Object convert (Point p, Config cfg) {
            JSONArray out = new JSONArray();
            out.add(p.x);
            out.add(p.y);
            return out;
        }
    };

    /**
     * Converts a dimension into an array with entries width, height.
     */
    public static final Converter<Dimension> DIMENSION = new Converter<Dimension>() {
        public Object convert (Dimension d, Config cfg) {
            JSONArray out = new JSONArray();
            out.add(d.width);
            out.add(d.height);
            return out;
        }
    };
}
