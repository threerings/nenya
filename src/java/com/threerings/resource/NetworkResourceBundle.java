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

package com.threerings.resource;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessControlException;

import java.util.HashSet;

import java.io.IOException;
import java.io.InputStream;

import java.awt.image.BufferedImage;

import static com.threerings.resource.Log.log;

/**
 * Resource bundle that retrieves its contents via HTTP over the network from a root URL.
 */
public class NetworkResourceBundle extends ResourceBundle
{
    public NetworkResourceBundle (String root, String path)
    {
        this(root, path, null);
    }

    public NetworkResourceBundle (String root, String path, HashSet<String> rsrcList)
    {
        if (!root.endsWith("/")) {
            root += "/";
        }
        try {
            _bundleURL = new URL(root + path);
        } catch (MalformedURLException mue) {
            log.warning("Created malformed URL for resource. [root=" + root + ", path=" + path);
        }
        _ident = path;

        _rsrcList = rsrcList;
    }

    @Override // documentation inherited
    public String getIdent ()
    {
        return _ident;
    }

    @Override // documentation inherited
    public InputStream getResource (String path)
        throws IOException
    {
        // If we can reject it before opening a connection, then save the network latency.
        if (!inResourceList(path)) {
            return null;
        }

        URL resourceUrl = new URL(_bundleURL, path);
        return getResource(resourceUrl);
    }

    protected static InputStream getResource(URL resourceUrl)
    {
        URLConnection ucon = null;
        try {
            ucon = resourceUrl.openConnection();
        } catch (IOException ioe) {
            log.warning("Unable to open connection [url=" + resourceUrl + ", ex=" + ioe + "]");
        }

        if (ucon == null) {
            return null;
        }

        try {
            ucon.connect();
            return ucon.getInputStream();
        } catch (IOException ioe) {
            log.warning("Unable to open input stream [url=" + resourceUrl + ", ex=" + ioe + "]");
            return null;
        } catch (AccessControlException ace) {
            log.warning("Unable to connect due to access permissions [url=" + resourceUrl + "]");
            throw ace;
        }
    }

    @Override // documentation inherited
    public BufferedImage getImageResource (String path, boolean useFastIO)
        throws IOException
    {
        InputStream in = getResource(path);
        if (in == null) {
            return null;
        }
        return ResourceManager.loadImage(in, false);
    }

    /**
     * Returns whether we believe the path to be available.  If we have no list, we assume
     *  everything may be available.
     */
    protected boolean inResourceList (String path)
    {
        return _rsrcList == null || _rsrcList.contains(_ident + path);
    }

    /**
     * Returns a string representation of this resource bundle.
     */
    @Override
    public String toString ()
    {
        return "[url=" + _bundleURL + "]";
    }

    /** Our identifier for this bundle. */
    protected String _ident;

    /** Our root url to the resources in this bundle. */
    protected URL _bundleURL;

    /** A list of all the resources included in this bundle. */
    protected HashSet<String> _rsrcList;
}
