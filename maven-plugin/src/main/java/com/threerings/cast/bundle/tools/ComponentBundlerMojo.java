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

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

import com.samskivert.util.Tuple;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Processes component source files and generates component bundles.
 */
@Mojo(name="cbundle", defaultPhase=LifecyclePhase.PROCESS_RESOURCES)
public class ComponentBundlerMojo extends AbstractMojo
{
    public static final String[] DEFAULT_INCLUDES = { "**/*.png" };
    public static final String[] DEFAULT_EXCLUDES = { "components/**" };

    public static class BundleDirectory {
        /** The root path which will be stripped from the image paths prior to parsing them to
         * obtain the component class, type and action names. This is also the top level directory
         * for finding component files. This is relative to base. */
        public String root;

        /** The path corresponding to the components we want to generate. These are relative to
         * base/root. */
        public String path;

        @Override public String toString () { return root + "//" + path; }
    }

    /**
     * The base directory for all components.
     */
    @Parameter(required=true)
    private File base;

    /**
     * Sets the target base directory. This is the analog of base, but in the target/classes
     * location.
     */
    @Parameter(required=true)
    private File targetBase;

    /**
     * Sets the path to the component map file that we'll use to obtain
     * component ids for the bundled components.
     */
    @Parameter
    private File mapFile;

    /**
     * Sets the path to the action tilesets definition file.
     */
    @Parameter
    private File actionDef;

    /**
     * Sets the root path which will be stripped from the image paths prior to parsing them to
     * obtain the component class, type and action names. This is also the top level directory for
     * finding component files. By default, any matching directories will be used.
     */
    @Parameter(required=true)
    private BundleDirectory[] bundleDirectories;

    /**
     * Note whether we are supposed to use the raw png files directly in the bundle or try to
     * re-encode them.
     */
    @Parameter(defaultValue="false")
    private boolean keepRawPngs;

    /**
     * File patterns to include during the component bundle processing.
     */
    @Parameter
    private List<String> includes = Arrays.asList(DEFAULT_INCLUDES);

    /**
     * File patterns to exclude during the component bundle processing.
     */
    @Parameter
    private List<String> excludes = Arrays.asList(DEFAULT_EXCLUDES);

    /**
     * Performs the actual work of the task.
     */
    public void execute ()
        throws MojoExecutionException
    {
        if (mapFile == null) mapFile = new File(base, "compmap.txt");
        if (actionDef == null) actionDef = new File(base, "actions.xml");

        ComponentBundler bundler = new ComponentBundler(mapFile, actionDef) {
            @Override protected boolean keepRawPngs () { return keepRawPngs; }
            @Override protected boolean uncompressed () { return false; }
            @Override protected void logInfo (String message) { getLog().info(message); }
            @Override protected void logWarn (String message) { getLog().warn(message); }
        };

        if (bundleDirectories == null) {
            bundleDirectories = findBundleDirs();
        }
        for (BundleDirectory bdir : bundleDirectories) {
            getLog().info("Processing " + bdir);
            File root = new File(base, bdir.root);

            // create the scanner
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(new File(root, bdir.path));
            scanner.setIncludes(includes.toArray(new String[0]));
            scanner.setExcludes(excludes.toArray(new String[0]));
            scanner.scan();

            File targetDir = new File(targetBase, bdir.root);
            File target = new File(new File(targetDir, bdir.path), "components.jar");
            List<Tuple<File,List<String>>> sourceDirs = Lists.newArrayList();
            sourceDirs.add(Tuple.newTuple(scanner.getBasedir(),
                                          Arrays.asList(scanner.getIncludedFiles())));
            bundler.execute(root.getPath(), target, sourceDirs);
        }
    }

    protected BundleDirectory[] findBundleDirs ()
    {
        List<BundleDirectory> bundleDirs = Lists.newArrayList();
        for (File root : base.listFiles()) {
            if (!root.isDirectory() || root.getName().equals(".svn")) {
                continue;
            }
            for (File path : root.listFiles()) {
                if (!path.isDirectory() || path.getName().equals(".svn")) {
                    continue;
                }
                BundleDirectory dir = new BundleDirectory();
                dir.root = root.getName();
                dir.path = path.getName();
                getLog().debug("Found bundle dir " + dir.root + "/" + dir.path);
                bundleDirs.add(dir);
            }
        }
        return bundleDirs.toArray(new BundleDirectory[bundleDirs.size()]);
    }
}
