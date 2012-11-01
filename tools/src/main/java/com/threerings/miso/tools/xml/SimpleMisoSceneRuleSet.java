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

import java.util.ArrayList;

import org.xml.sax.Attributes;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;

import com.google.common.collect.Lists;

import com.samskivert.xml.CallMethodSpecialRule;
import com.samskivert.xml.SetFieldRule;
import com.samskivert.xml.SetPropertyFieldsRule;

import com.threerings.miso.data.ObjectInfo;
import com.threerings.miso.data.SimpleMisoSceneModel;

import com.threerings.tools.xml.NestableRuleSet;

/**
 * Used to parse a {@link SimpleMisoSceneModel} from XML.
 */
public class SimpleMisoSceneRuleSet implements NestableRuleSet
{
    // documentation inherited from interface
    public String getOuterElement ()
    {
        return SimpleMisoSceneWriter.OUTER_ELEMENT;
    }

    // documentation inherited from interface
    public void addRuleInstances (String prefix, Digester dig)
    {
        // this creates the appropriate instance when we encounter our
        // prefix tag
        dig.addRule(prefix, new Rule() {
            @Override
            public void begin (String namespace, String name,
                               Attributes attributes) throws Exception {
                digester.push(createMisoSceneModel());
            }
            @Override
            public void end (String namespace, String name) throws Exception {
                digester.pop();
            }
        });

        // set up rules to parse and set our fields
        dig.addRule(prefix + "/width", new SetFieldRule("width"));
        dig.addRule(prefix + "/height", new SetFieldRule("height"));
        dig.addRule(prefix + "/viewwidth", new SetFieldRule("vwidth"));
        dig.addRule(prefix + "/viewheight", new SetFieldRule("vheight"));
        dig.addRule(prefix + "/base", new SetFieldRule("baseTileIds"));

        dig.addObjectCreate(prefix + "/objects", ArrayList.class.getName());
        dig.addObjectCreate(prefix + "/objects/object",
                            ObjectInfo.class.getName());
        dig.addSetNext(prefix + "/objects/object", "add",
                       Object.class.getName());

        dig.addRule(prefix + "/objects/object", new SetPropertyFieldsRule());

        dig.addRule(prefix + "/objects", new CallMethodSpecialRule() {
            @Override
            public void parseAndSet (String bodyText, Object target)
                throws Exception
            {
                @SuppressWarnings("unchecked") ArrayList<ObjectInfo> ilist =
                    (ArrayList<ObjectInfo>)target;
                ArrayList<ObjectInfo> ulist = Lists.newArrayList();
                SimpleMisoSceneModel model = (SimpleMisoSceneModel)
                    digester.peek(1);

                // filter interesting and uninteresting into two lists
                for (int ii = 0; ii < ilist.size(); ii++) {
                    ObjectInfo info = ilist.get(ii);
                    if (!info.isInteresting()) {
                        ilist.remove(ii--);
                        ulist.add(info);
                    }
                }

                // now populate the model
                SimpleMisoSceneModel.populateObjects(model, ilist, ulist);
            }
        });
    }

    protected SimpleMisoSceneModel createMisoSceneModel ()
    {
        return new SimpleMisoSceneModel(0, 0, 0, 0);
    }
}
