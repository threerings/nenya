package com.threerings.miso.client {

import as3isolib.display.primitive.IsoBox;
import as3isolib.graphics.SolidColorFill;

public class ObjectTileIsoSprite extends IsoBox
{
    public function ObjectTileIsoSprite (x :int, y :int, tileId :int)
    {
        width = 1;
        height = 1;
        length = 1;

        moveTo(x, y, 0);

        // TEMP
        fill = new SolidColorFill(tileId * 1000, 1.0);
    }
}
}
