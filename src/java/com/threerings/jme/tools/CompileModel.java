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

package com.threerings.jme.tools;

import java.io.File;
import java.io.FileInputStream;

import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;

import com.google.common.collect.Maps;

import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.util.LoggingSystem;
import com.jmex.model.XMLparser.Converters.DummyDisplaySystem;

import com.samskivert.util.PropertiesUtil;
import com.samskivert.util.StringUtil;

import com.threerings.jme.model.Model;
import com.threerings.jme.tools.ModelDef.TransformNode;
import com.threerings.jme.tools.xml.AnimationParser;
import com.threerings.jme.tools.xml.ModelParser;

/**
 * An application for compiling 3D models defined in XML to fast-loading binary files.
 */
public class CompileModel
{
    /**
     * Loads the model described by the given properties file and compiles it to a
     * <code>.dat</code> file in the same directory.
     *
     * @return the loaded model, or <code>null</code> if the compiled version is up-to-date
     */
    public static Model compile (File source)
        throws Exception
    {
        return compile(source, source.getParentFile());
    }

    /**
     * Loads the model described by the given properties file and compiles it into a
     * <code>.dat</code> file in the specified directory.
     *
     * @return the loaded model, or <code>null</code> if the compiled version is up-to-date
     */
    public static Model compile (File source, File targetDir)
        throws Exception
    {
        String sname = source.getName();
        int didx = sname.lastIndexOf('.');
        String root = (didx == -1) ? sname : sname.substring(0, didx);
        File target = new File(targetDir, root + ".dat");
        File content = new File(source.getParentFile(), root + ".mxml");

        boolean needsUpdate = false;
        if (source.lastModified() >= target.lastModified() ||
            content.lastModified() >= target.lastModified()) {
            needsUpdate = true;
        }

        // load the model properties
        Properties props = new Properties();
        FileInputStream in = new FileInputStream(source);
        props.load(in);
        in.close();

        // locate the animations, if any
        String[] anims = StringUtil.parseStringArray(props.getProperty("animations", ""));
        File[] afiles = new File[anims.length];
        File dir = source.getParentFile();
        for (int ii = 0; ii < anims.length; ii++) {
            afiles[ii] = new File(dir, anims[ii] + ".mxml");
            if (afiles[ii].lastModified() >= target.lastModified()) {
                needsUpdate = true;
            }
        }
        if (!needsUpdate) {
            return null;
        }

        System.out.println("Compiling to " + target + "...");

        // parse the model and animations
        ModelDef mdef = _mparser.parseModel(content.toString());
        AnimationDef[] adefs = new AnimationDef[anims.length];
        for (int ii = 0; ii < adefs.length; ii++) {
            System.out.println("  Adding " + afiles[ii] + "...");
            adefs[ii] = _aparser.parseAnimation(afiles[ii].toString());
        }

        // preprocess the model and animations to determine which nodes never move, which never
        // move within an animation, and which never move with respect to others
        HashMap<String, TransformNode> tnodes = Maps.newHashMap();
        Node troot = mdef.createTransformTree(props, tnodes);
        for (AnimationDef adef : adefs) {
            adef.filterTransforms(troot, tnodes);
        }
        mdef.mergeSpatials(tnodes);

        // load the model content
        HashMap<String, Spatial> nodes = Maps.newHashMap();
        Model model = mdef.createModel(props, nodes);
        model.initPrototype();

        // load the animations, if any
        for (int ii = 0; ii < anims.length; ii++) {
            model.addAnimation(anims[ii], adefs[ii].createAnimation(
                PropertiesUtil.getSubProperties(props, anims[ii]), nodes, tnodes));
        }

        // write and return the model
        File parent = target.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            System.err.println("Unable to create target directory '" + parent + "'.");
        } else {
            model.writeToFile(target);
        }
        return model;
    }

    public static void main (String[] args)
    {
        if (args.length < 1) {
            System.err.println("Usage: CompileModel source.properties");
            System.exit(-1);
        }

        // create a dummy display system
        new DummyDisplaySystem();
        LoggingSystem.getLogger().setLevel(Level.WARNING);

        try {
            compile(new File(args[0]));
        } catch (Exception e) {
            System.err.println("Error compiling model: " + e);
        }
    }

    /** A parser for the model definitions. */
    protected static ModelParser _mparser = new ModelParser();

    /** A parser for the animation definitions. */
    protected static AnimationParser _aparser = new AnimationParser();
}
