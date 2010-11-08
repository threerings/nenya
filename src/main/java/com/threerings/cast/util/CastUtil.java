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

package com.threerings.cast.util;

import java.util.ArrayList;
import java.util.Iterator;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import com.samskivert.util.RandomUtil;

import com.threerings.media.image.ColorPository;
import com.threerings.media.image.Colorization;

import com.threerings.cast.CharacterDescriptor;
import com.threerings.cast.ComponentClass;
import com.threerings.cast.ComponentRepository;

import static com.threerings.cast.Log.log;

/**
 * Miscellaneous cast utility routines.
 */
public class CastUtil
{
    /**
     * Returns a new character descriptor populated with a random set of components.
     */
    public static CharacterDescriptor getRandomDescriptor (
        String gender, ComponentRepository crepo, ColorPository cpos)
    {
        // get all available classes
        ArrayList<ComponentClass> classes = Lists.newArrayList();
        for (String element : CLASSES) {
            String cname = gender + "/" + element;
            ComponentClass cclass = crepo.getComponentClass(cname);

            // make sure the component class exists
            if (cclass == null) {
                log.warning("Missing definition for component class " +
                            "[class=" + cname + "].");
                continue;
            }

            // make sure there are some components in this class
            Iterator<Integer> iter = crepo.enumerateComponentIds(cclass);
            if (!iter.hasNext()) {
                log.info("Skipping class for which we have no components [class=" + cclass + "].");
                continue;
            }

            classes.add(cclass);
        }

        // select the components
        int[] components = new int[classes.size()];
        Colorization[][] zations = new Colorization[components.length][];
        for (int ii = 0; ii < components.length; ii++) {
            ComponentClass cclass = classes.get(ii);

            // get the components available for this class
            ArrayList<Integer> choices = Lists.newArrayList();
            Iterators.addAll(choices, crepo.enumerateComponentIds(cclass));

            zations[ii] = new Colorization[COLOR_CLASSES.length];
            for (int zz = 0; zz < COLOR_CLASSES.length; zz++) {
                zations[ii][zz] = cpos.getRandomStartingColor(COLOR_CLASSES[zz]).getColorization();
            }

            // choose a random component
            if (choices.size() > 0) {
                int idx = RandomUtil.getInt(choices.size());
                components[ii] = choices.get(idx).intValue();
            } else {
                log.info("Have no components in class [class=" + cclass + "].");
            }
        }

        return new CharacterDescriptor(components, zations);
    }

    // these are all specific to our test resourcse
    protected static final String[] CLASSES = {
        "legs", "feet", "hand_left", "hand_right", "torso",
        "head", "hair", "hat", "eyepatch" };
    protected static final String[] COLOR_CLASSES = {
        "skin", "hair", "textile_p", "textile_s" };
}
