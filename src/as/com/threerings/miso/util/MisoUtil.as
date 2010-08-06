package com.threerings.miso.util {

import com.threerings.util.MathUtil;

public class MisoUtil
{
    public static function fullToTile (fullCoord :int) :int
    {
        return MathUtil.floorDiv(fullCoord, FULL_TILE_FACTOR);
    }

    public static function tileToFull (tileCoord :int, fine :int = 0) :int
    {
        return tileCoord * FULL_TILE_FACTOR + fine;
    }

    public static function fullToFine (fullCoord :int) :int
    {
        return (fullCoord - (fullToTile(fullCoord) * FULL_TILE_FACTOR));
    }

    protected static const FULL_TILE_FACTOR :int = 100;
}
}