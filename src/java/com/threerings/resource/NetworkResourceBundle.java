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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlException;

/**
 * Resource bundle that retrieves its contents via HTTP over the network from a root URL.
 */
public class NetworkResourceBundle extends ResourceBundle
{
    public NetworkResourceBundle (String root, String path)
    {
        if (!root.endsWith("/")) {
            root += "/";
        }
        try {
            _bundleURL = new URL(root + path);
        } catch (MalformedURLException mue) {
            Log.warning("Created malformed URL for resource. [root=" + root + ", path=" + path);
        }
        _ident = path;
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
        URL resourceUrl = new URL(_bundleURL, path);
        HttpURLConnection ucon = null;
        try {
            ucon = (HttpURLConnection) resourceUrl.openConnection();
        } catch (IOException ioe) {
            Log.warning("Unable to open connection [url=" + resourceUrl + ", ex=" + ioe + "]");
        }

        if (ucon == null) {
            return null;
        }
        try {
            ucon.connect();
            return ucon.getInputStream();
        } catch (IOException ioe) {
            Log.warning("Unable to open input stream [url=" + resourceUrl + ", ex=" + ioe + "]");
            return null;
        } catch (AccessControlException ace) {
            Log.warning("Unable to connect due to access permissions [url=" + resourceUrl + "]");
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
        return ResourceManager.loadImage(in);
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
}
