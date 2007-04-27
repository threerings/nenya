package com.threerings.flash {

import flash.filters.GlowFilter;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFieldType;
import flash.text.TextFormat;
import flash.text.TextFormatAlign;

public class TextFieldUtil
{
    /**
     * Create a TextField.
     *
     * args: contains properties with which to initialize the TextField
     */
    public static function createField (text :String, args :Object = null) :TextField
    {
        var tf :TextField = new TextField();
        tf.background = getArg(args, "background", false);
        tf.border = getArg(args, "border", false);
        tf.multiline = getArg(args, "multiline", false);
        tf.type = getArg(args, "type", TextFieldType.DYNAMIC);

        var format :* = getArg(args, "format");
        if (format !== undefined) {
            tf.defaultTextFormat = format;
        }

        var outColor :* = getArg(args, "outlineColor");
        if (outColor !== undefined) {
            tf.filters = [ new GlowFilter(uint(outColor), 1, 2, 2, 255) ];
        }

        tf.autoSize = getArg(args, "autoSize", TextFieldAutoSize.LEFT);
        tf.text = text;
        if (tf.autoSize != null) {
            tf.width = tf.textWidth + 5;
            tf.height = tf.textHeight + 4;
        }

        return tf;
    }

    /**
     * Create a TextFormat.
     */
    public static function createFormat (args :Object) :TextFormat
    {
        var f :TextFormat = new TextFormat();
        f.align = getArg(args, "align", TextFormatAlign.LEFT);
        f.blockIndent = getArg(args, "blockIndent", null);
        f.bold = getArg(args, "bold", false);
        f.bullet = getArg(args, "bullet", null);

        f.size = getArg(args, "size", 18);
        f.font = getArg(args, "font", "Arial");
        f.color = getArg(args, "color", 0x000000);

        return f;
    }

    protected static function getArg (args :Object, name :String, defVal :* = undefined) :*
    {
        if (args != null && (name in args)) {
            return args[name];
        }
        return defVal;
    }
}
}
