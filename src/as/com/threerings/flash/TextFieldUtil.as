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

import flash.events.Event;
import flash.events.MouseEvent;

import flash.filters.GlowFilter;

import flash.system.Capabilities;
import flash.system.System;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFieldType;
import flash.text.TextFormat;
import flash.text.TextFormatAlign;

import com.threerings.util.Util;

public class TextFieldUtil
{
    /** A fudge factor that must be added to a TextField's textWidth when setting the width. */
    public static const WIDTH_PAD :int = 5;

    /** A fudge factor that must be added to a TextField's textHeight when setting the height. */
    public static const HEIGHT_PAD :int = 4;

    /**
     * Create a TextField.
     *
     * @param initProps contains properties with which to initialize the TextField.
     * Additionally it may contain the following properties:
     *    outlineColor: uint
     *
     * initProps may be destructively modified.
     */
    public static function createField (
        text :String, initProps :Object = null, clazz :Class = null) :TextField
    {
        var tf :TextField = (clazz == null) ? new TextField() : TextField(new clazz());

        if ("outlineColor" in initProps) {
            tf.filters = [ new GlowFilter(uint(initProps["outlineColor"]), 1, 2, 2, 255) ];
        }

        Util.init(tf, initProps, null, MASK_FIELD_PROPS);
        tf.text = text;
        if (tf.autoSize != null) {
            tf.width = tf.textWidth + WIDTH_PAD;
            tf.height = tf.textHeight + HEIGHT_PAD;
        }

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
