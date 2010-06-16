package com.threerings.miso.client {

import as3isolib.display.primitive.IsoBox;
import as3isolib.graphics.SolidColorFill;

public class BaseTileIsoSprite extends IsoBox
{
    public function BaseTileIsoSprite (x :int, y :int, tileId :int)
    {
        width = 1;
        height = VERT_OFFSET;
        length = 1;

        moveTo(x, y, -VERT_OFFSET);

        // TEMP
        fill = new SolidColorFill(tileId * 1000, 1.0);
    }

    protected static const VERT_OFFSET :Number = 0.01;
}
}
