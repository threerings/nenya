//
// $Id$

package com.threerings.flash.media {

public class MediaPlayerCodes
{
    public static const STATE :String = "state";

    public static const DURATION :String = "duration";

    public static const POSITION :String = "position";

    public static const METADATA :String = "metadata";

    /** Only applicable for VideoPlayer. */
    public static const SIZE :String = "size";

    public static const ERROR :String = "error";

    /** Indicates we're still loading or initializing. */
    public static const STATE_UNREADY :int = 0;

    /** Indicates we're ready to play. */
    public static const STATE_READY :int = 1;

    public static const STATE_PLAYING :int = 2;

    public static const STATE_PAUSED :int = 3;
}
}
