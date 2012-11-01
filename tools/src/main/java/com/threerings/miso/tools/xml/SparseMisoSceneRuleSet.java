//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// https://github.com/threerings/nenya
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

package com.threerings.miso.tools.xml;

import org.xml.sax.Attributes;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;

import com.samskivert.xml.SetFieldRule;
import com.samskivert.xml.SetPropertyFieldsRule;

import com.threerings.miso.data.ObjectInfo;
import com.threerings.miso.data.SparseMisoSceneModel;
import com.threerings.miso.data.SparseMisoSceneModel.Section;

import com.threerings.tools.xml.NestableRuleSet;

/**
 * Used to parse a {@link SparseMisoSceneModel} from XML.
 */
public class SparseMisoSceneRuleSet implements NestableRuleSet
{
    // documentation inherited from interface
    public String getOuterElement ()
    {
        return SparseMisoSceneWriter.OUTER_ELEMENT;
    }

    // documentation inherited from interface
    public void addRuleInstances (String prefix, Digester dig)
    {
        // this creates the appropriate instance when we encounter our
        // prefix tag
        dig.addRule(prefix, new Rule() {
            @Override public void begin (String namespace, String name,
                               Attributes attributes) throws Exception {
                digester.push(createMisoSceneModel());
            }
            @Override public void end (String namespace, String name) throws Exception {
                digester.pop();
            }
        });

        // set up rules to parse and set our fields
        dig.addRule(prefix + "/swidth", new SetFieldRule("swidth"));
        dig.addRule(prefix + "/sheight", new SetFieldRule("sheight"));
        dig.addRule(prefix + "/defTileSet", new SetFieldRule("defTileSet"));

        String sprefix = prefix + "/sections/section";
        dig.addObjectCreate(sprefix, Section.class.getName());
        dig.addRule(sprefix, new SetPropertyFieldsRule());
        dig.addRule(sprefix + "/base", new SetFieldRule("baseTileIds"));
        addObjectExtractor(dig, "objects", sprefix, "addObject");
        dig.addSetNext(sprefix, "setSection", Section.class.getName());
    }

    /**
     * Adds a set of rules to <code>dig</code> to create an Object info from the element at
     * base/type/object and calls <code>methodName</code> on the object on dig's stack.
     */
    public static void addObjectExtractor (Digester dig, String type, String base,
        String methodName)
    {
        String prefix = base + "/" + type + "/object";
        dig.addObjectCreate(prefix, ObjectInfo.class);
        dig.addRule(prefix, new SetPropertyFieldsRule());
        dig.addSetNext(prefix, methodName, ObjectInfo.class.getName());
    }

    protected SparseMisoSceneModel createMisoSceneModel ()
    {
        return new SparseMisoSceneModel();
    }
}
