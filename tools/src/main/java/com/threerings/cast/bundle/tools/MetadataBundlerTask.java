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

package com.threerings.cast.bundle.tools;

import java.util.ArrayList;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.Deflater;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.apache.commons.digester.Digester;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.util.Tuple;

import com.threerings.media.tile.TileSet;
import com.threerings.media.tile.tools.xml.SwissArmyTileSetRuleSet;
import com.threerings.media.tile.tools.xml.TileSetRuleSet;
import com.threerings.media.tile.tools.xml.UniformTileSetRuleSet;

import com.threerings.cast.ActionSequence;
import com.threerings.cast.ComponentClass;
import com.threerings.cast.bundle.BundleUtil;
import com.threerings.cast.tools.xml.ActionRuleSet;
import com.threerings.cast.tools.xml.ClassRuleSet;

/**
 * Ant task for creating metadata bundles, which contain action sequence
 * and component class definition information. This task must be
 * configured with a number of parameters:
 *
 * <pre>
 * actiondef=[path to actions.xml]
 * classdef=[path to classes.xml]
 * file=[path to metadata bundle, which will be created]
 * </pre>
 */
public class MetadataBundlerTask extends Task
{
    public void setActiondef (File actiondef)
    {
        _actionDef = actiondef;
    }

    public void setClassdef (File classdef)
    {
        _classDef = classdef;
    }

    public void setTarget (File target)
    {
        _target = target;
    }

    /**
     * Performs the actual work of the task.
     */
    @Override
    public void execute ()
        throws BuildException
    {
        // make sure everythign was set up properly
        ensureSet(_actionDef, "Must specify the action sequence " +
                  "definitions via the 'actiondef' attribute.");
        ensureSet(_classDef, "Must specify the component class definitions " +
                  "via the 'classdef' attribute.");
        ensureSet(_target, "Must specify the path to the target bundle " +
                  "file via the 'target' attribute.");

        // make sure we can write to the target bundle file
        OutputStream fout = null;
        try {

            // parse our metadata
            Tuple<Map<String, ActionSequence>, Map<String, TileSet>> tuple = parseActions();
            Map<String, ActionSequence> actions = tuple.left;
            Map<String, TileSet> actionSets = tuple.right;
            Map<String, ComponentClass> classes = parseClasses();

            fout = createOutputStream(_target);

            // throw the serialized actions table in there
            fout = nextEntry(fout, BundleUtil.ACTIONS_PATH);
            ObjectOutputStream oout = new ObjectOutputStream(fout);
            oout.writeObject(actions);
            oout.flush();

            // throw the serialized action tilesets table in there
            fout = nextEntry(fout, BundleUtil.ACTION_SETS_PATH);
            oout = new ObjectOutputStream(fout);
            oout.writeObject(actionSets);
            oout.flush();

            // throw the serialized classes table in there
            fout =  nextEntry(fout, BundleUtil.CLASSES_PATH);
            oout = new ObjectOutputStream(fout);
            oout.writeObject(classes);
            oout.flush();

            // close it up and we're done
            fout.close();

        } catch (IOException ioe) {
            String errmsg = "Unable to output to target bundle " +
                "[path=" + _target.getPath() + "].";
            throw new BuildException(errmsg, ioe);

        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException ioe) {
                    // nothing to complain about here
                }
            }
        }
    }

    /**
     * Creates the base output stream to which to write our bundle's files.
     */
    protected OutputStream createOutputStream (File target)
        throws IOException
    {
        JarOutputStream jout = new JarOutputStream(new FileOutputStream(target));
        jout.setLevel(Deflater.BEST_COMPRESSION);
        return jout;
    }

    /**
     * Advances to the next named entry in the bundle and returns the stream to which to write
     *  that entry.
     */
    protected OutputStream nextEntry (OutputStream lastEntry, String path)
        throws IOException
    {
        ((JarOutputStream)lastEntry).putNextEntry(new JarEntry(path));
        return lastEntry;
    }

    /**
     * Configures <code>ruleSet</code> and hooks it into <code>digester</code>.
     */
    protected static void addTileSetRuleSet (Digester digester, TileSetRuleSet ruleSet)
    {
        ruleSet.setPrefix("actions" + ActionRuleSet.ACTION_PATH);
        digester.addRuleSet(ruleSet);
        digester.addSetNext(ruleSet.getPath(), "add", Object.class.getName());
    }

    protected Tuple<Map<String, ActionSequence>, Map<String, TileSet>> parseActions ()
        throws BuildException
    {
        // scan through the XML once to read the actions
        Digester digester = new Digester();
        ActionRuleSet arules = new ActionRuleSet();
        arules.setPrefix("actions");
        digester.addRuleSet(arules);
        digester.addSetNext("actions" + ActionRuleSet.ACTION_PATH, "add", Object.class.getName());
        ArrayList<?> actlist = parseList(digester, _actionDef);

        // now go through a second time reading the tileset info
        digester = new Digester();
        addTileSetRuleSet(digester, new SwissArmyTileSetRuleSet());
        addTileSetRuleSet(digester, new UniformTileSetRuleSet("/uniformTileset"));
        ArrayList<?> setlist = parseList(digester, _actionDef);

        // sanity check
        if (actlist.size() != setlist.size()) {
            String errmsg = "An action is missing its tileset " +
                "definition, or something even wackier is going on.";
            throw new BuildException(errmsg);
        }

        // now create our mappings
        Map<String, ActionSequence> actmap = Maps.newHashMap();
        Map<String, TileSet> setmap = Maps.newHashMap();

        // create the action map
        for (int ii = 0; ii < setlist.size(); ii++) {
            TileSet set = (TileSet)setlist.get(ii);
            ActionSequence act = (ActionSequence)actlist.get(ii);
            // make sure nothing was missing in the action sequence
            // definition parsed from XML
            String errmsg = ActionRuleSet.validate(act);
            if (errmsg != null) {
                errmsg = "Action sequence invalid [seq=" + act +
                    ", error=" + errmsg + "].";
                throw new BuildException(errmsg);
            }
            actmap.put(act.name, act);
            setmap.put(act.name, set);
        }

        return new Tuple<Map<String, ActionSequence>, Map<String, TileSet>>(actmap, setmap);
    }

    protected Map<String, ComponentClass> parseClasses ()
        throws BuildException
    {
        // load up our action and class info
        Digester digester = new Digester();

        // add our action rule set and a a rule to grab parsed actions
        ClassRuleSet crules = new ClassRuleSet();
        crules.setPrefix("classes");
        digester.addRuleSet(crules);
        digester.addSetNext("classes" + ClassRuleSet.CLASS_PATH,
                            "add", Object.class.getName());

        ArrayList<?> setlist = parseList(digester, _classDef);
        Map<String, ComponentClass> clmap = Maps.newHashMap();

        // create the action map
        for (int ii = 0; ii < setlist.size(); ii++) {
            ComponentClass cl = (ComponentClass)setlist.get(ii);
            clmap.put(cl.name, cl);
        }

        return clmap;
    }

    protected ArrayList<?> parseList (Digester digester, File path)
        throws BuildException
    {
        try {
            FileInputStream fin = new FileInputStream(path);
            BufferedInputStream bin = new BufferedInputStream(fin);

            ArrayList<Object> setlist = Lists.newArrayList();
            digester.push(setlist);

            // now fire up the digester to parse the stream
            try {
                digester.parse(bin);
            } catch (Exception e) {
                throw new BuildException("Parsing error.", e);
            }

            return setlist;

        } catch (FileNotFoundException fnfe) {
            String errmsg = "Unable to load metadata definition file " +
                "[path=" + path + "].";
            throw new BuildException(errmsg, fnfe);
        }
    }

    protected void ensureSet (Object value, String errmsg)
        throws BuildException
    {
        if (value == null) {
            throw new BuildException(errmsg);
        }
    }

    protected File _actionDef;
    protected File _classDef;
    protected File _target;
}
