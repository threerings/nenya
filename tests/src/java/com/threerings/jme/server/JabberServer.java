//
// $Id$

package com.threerings.jme.server;

import com.google.inject.Guice;
import com.google.inject.Injector;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.CrowdServer;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.jme.data.JabberConfig;

import static com.threerings.crowd.Log.log;

/**
 * A basic server that creates a single room and sticks everyone in it where they can chat with one
 * another.
 */
public class JabberServer extends CrowdServer
{
    @Override // from CrowdServer
    public void init (Injector injector)
        throws Exception
    {
        super.init(injector);

        // create a single location
        _place = plreg.createPlace(new JabberConfig());
        log.info("Created chat room " + _place.where() + ".");
    }

    public static void main (String[] args)
    {
        Injector injector = Guice.createInjector(new Module());
        JabberServer server = injector.getInstance(JabberServer.class);
        try {
            server.init(injector);
            server.run();
        } catch (Exception e) {
            log.warning("Unable to initialize server.", e);
        }
    }

    protected PlaceManager _place;
}
