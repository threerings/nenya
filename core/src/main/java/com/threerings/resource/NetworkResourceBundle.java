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

package com.threerings.resource;

import static com.threerings.resource.Log.log;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessControlException;
import java.util.Set;

import com.samskivert.util.Logger;

/**
 * Resource bundle that retrieves its contents via HTTP over the network from a root URL.
 */
public class NetworkResourceBundle extends KnownAvailabilityResourceBundle
{
    public NetworkResourceBundle (String root, String path, Set<String> rsrcList)
    {
        super(rsrcList);
        if (!root.endsWith("/")) {
            root += "/";
        }
        try {
            _bundleURL = new URL(root + path);
        } catch (MalformedURLException mue) {
            log.warning("Created malformed URL for resource. [root=" + root + ", path=" + path);
        }
        _ident = path;
    }

    @Override
    public String getIdent ()
    {
        return _ident;
    }

    @Override
    public InputStream getResource (String path)
        throws IOException
    {
        // If we can reject it before opening a connection, then save the network latency.
        if (!isPossiblyAvailable(_ident + path)) {
            return null;
        }

        URL resourceUrl = new URL(_bundleURL, path);
        return getResource(resourceUrl);
    }

    protected static InputStream getResource(URL resourceUrl)
    {
        URLConnection ucon;
        try {
            ucon = resourceUrl.openConnection();
        } catch (IOException ioe) {
            log.warning("Unable to open connection [url=" + resourceUrl + ", ex=" + ioe + "]");
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

    @Override
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
     * Returns a string representation of this resource bundle.
     */
    @Override
    public String toString ()
    {
        return Logger.format(getClass().getSimpleName(), "url", _bundleURL, "ident", _ident,
            "knownResources", (_rsrcs != null));
    }

    /** Our identifier for this bundle. */
    protected String _ident;

    /** Our root url to the resources in this bundle. */
    protected URL _bundleURL;
}
