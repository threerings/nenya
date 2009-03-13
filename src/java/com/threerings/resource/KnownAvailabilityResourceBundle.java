package com.threerings.resource;

import java.util.Set;

/**
 * Keeps a set of known resources and allows implementors to ask if a resource is in it before
 * performing a potentially expensive failed lookup.
 */
public abstract class KnownAvailabilityResourceBundle extends ResourceBundle
{
    public KnownAvailabilityResourceBundle (Set<String> availableResources)
    {
        _rsrcs = availableResources;
    }

    /**
     * Returns whether we believe the path to be available. If we have no list, we assume
     * everything may be available.
     */
    protected boolean isPossiblyAvailable (String path)
    {
        return _rsrcs == null || _rsrcs.contains(path);
    }

    /** All the resources included in this bundle. */
    protected Set<String> _rsrcs;
}
