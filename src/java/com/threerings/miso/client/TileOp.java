package com.threerings.miso.client;

import java.awt.Rectangle;

/**  
 * Exposes an operation to be applied to tiles by {@link TileOpApplicator}. 
 */
public interface TileOp
{
    public void apply (int tx, int ty, Rectangle tbounds);
}