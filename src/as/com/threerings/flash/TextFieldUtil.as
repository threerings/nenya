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

package com.threerings.flash {

import com.threerings.util.StringUtil;
import com.threerings.util.Util;

import flash.events.Event;
import flash.events.MouseEvent;
import flash.filters.GlowFilter;
import flash.system.Capabilities;
import flash.system.System;
import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

public class TextFieldUtil
{
    /** A fudge factor that must be added to a TextField's textWidth when setting the width. */
    public static const WIDTH_PAD :int = 5;

    /** A fudge factor that must be added to a TextField's textHeight when setting the height. */
    public static const HEIGHT_PAD :int = 4;

    /**
     * Ensures that a single-line TextField is not wider than the specified width, and
     * truncates it with the truncation string if is. If the TextField is truncated,
     * it will be resized to its new textWidth.
     *
     * @param width the maximum pixel width of the TextField. If tf.width > width,
     * the text inside the TextField will be truncated, and will have the truncation string
     * appended.
     * @param truncationString the string to append to the end of the TextField if it exceeds
     * the specified width.
     * @return true if truncation took place
     */
    public static function setMaximumTextWidth (tf :TextField, width :Number, truncationString :String = "...") :Boolean
    {
        if (tf.numLines > 1) {
            // We only operate on single-line TextFields
            return false;
        }

        // TextField.textWidth doesn't account for scale, so account for it here
        width /= tf.scaleX;

        var truncated :Boolean;
        var setText :String = tf.text;
        while (tf.textWidth + WIDTH_PAD > width && setText.length > 0) {
            // Drop characters from our string until we hit our target width.
            // Flash doesn't appear to provide a nicer way to get text metrics than
            // sticking stuff in a TextField and calling textWidth.
            setText = setText.substr(0, setText.length - 1);
            tf.text = setText + truncationString;

            truncated = true;
        }

        if (truncated) {
            // strip whitespace characters from the end of the truncated string
            setText = StringUtil.trimEnd(setText);
            tf.text = setText + truncationString;

            // resize the TextField
            tf.width = tf.textWidth + WIDTH_PAD;
            tf.height = tf.textHeight + HEIGHT_PAD;
        }

        return truncated;
    }

    /**
     * Create a TextField.
     *
     * @param initProps contains properties with which to initialize the TextField.
     * Additionally it may contain the following properties:
     *    outlineColor: uint
     * @param formatProps contains properties with which to initialize the defaultTextFormat.
     */
    public static function createField (
        text :String, initProps :Object = null, formatProps :Object = null, clazz :Class = null)
        :TextField
    {
        var tf :TextField = (clazz == null) ? new TextField() : TextField(new clazz());

        if ((initProps != null) && ("outlineColor" in initProps)) {
            tf.filters = [ new GlowFilter(uint(initProps["outlineColor"]), 1, 2, 2, 255) ];
        }

        Util.init(tf, initProps, null, MASK_FIELD_PROPS);
        if (formatProps != null) {
            tf.defaultTextFormat = createFormat(formatProps);
        }
        updateText(tf, text);

        return tf;
    }

    /**
     * Create a TextFormat using initProps.
     * If unspecified, the following properties have default values:
     *  size: 18
     *  font: _sans
     */
    public static function createFormat (initProps :Object) :TextFormat
    {
        var f :TextFormat = new TextFormat();
        Util.init(f, initProps, DEFAULT_FORMAT_PROPS);
        return f;
    }

    /**
     * Update the defaultTextFormat for the specified field, as well as all text therein.
     */
    public static function updateFormat (field :TextField, props :Object) :void
    {
        var f :TextFormat = field.defaultTextFormat; // this gets a clone of the default fmt
        Util.init(f, props); // update the clone
        field.defaultTextFormat = f; // set it as the new default
        updateText(field, field.text); // jiggle the text to update it
    }

    /**
     * Update the text in the field, automatically resizing it if appropriate.
     */
    public static function updateText (field :TextField, text :String) :void
    {
        field.text = text;
        if (field.autoSize != TextFieldAutoSize.NONE) {
            field.width = field.textWidth + WIDTH_PAD;
            field.height = field.textHeight + HEIGHT_PAD;
        }
    }

    /**
     * Include the specified TextField in a set of TextFields in which only
     * one may have a selection at a time.
     */
    public static function trackSingleSelectable (textField :TextField) :void
    {
        textField.addEventListener(MouseEvent.MOUSE_MOVE, handleTrackedSelection);

        // immediately put the kibosh on any selection
        textField.setSelection(0, 0);
    }

    /**
     * Internal method related to tracking a single selectable TextField.
     */
    protected static function handleTrackedSelection (event :MouseEvent) :void
    {
        if (event.buttonDown) {
            var field :TextField = event.target as TextField;
            if (field == _lastSelected) {
                updateSelection(field);

            } else if (field.selectionBeginIndex != field.selectionEndIndex) {
                // clear the last one..
                if (_lastSelected != null) {
                    handleLastSelectedRemoved();
                }
                _lastSelected = field;
                _lastSelected.addEventListener(Event.REMOVED_FROM_STAGE, handleLastSelectedRemoved);
                updateSelection(field);
            }
        }
    }

    /**
     * Process the selection.
     */
    protected static function updateSelection (field :TextField) :void
    {
        if (-1 != Capabilities.os.indexOf("Linux")) {
            var str :String = field.text.substring(
                field.selectionBeginIndex, field.selectionEndIndex);
            System.setClipboard(str);
        }
    }

    /**
     * Internal method related to tracking a single selectable TextField.
     */
    protected static function handleLastSelectedRemoved (... ignored) :void
    {
        _lastSelected.setSelection(0, 0);
        _lastSelected.removeEventListener(Event.REMOVED_FROM_STAGE, handleLastSelectedRemoved);
        _lastSelected = null;
    }

    /** The last tracked TextField to be selected. */
    protected static var _lastSelected :TextField;

    protected static const MASK_FIELD_PROPS :Object = { outlineColor: true };

    protected static const DEFAULT_FORMAT_PROPS :Object = { size: 18, font: "_sans" };
}
}
