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

package com.threerings.flex {

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;

import flash.geom.Rectangle;

import mx.controls.Menu;
import mx.core.mx_internal;
import mx.core.Application;
import mx.core.ScrollPolicy;
import mx.events.MenuEvent;

import mx.utils.ObjectUtil;

import flexlib.controls.ScrollableArrowMenu;

import com.threerings.util.CommandEvent;

use namespace mx_internal;

/**
 * A pretty standard menu that can submit CommandEvents if menu items
 * have "command" and possibly "arg" properties. Commands are submitted to
 * controllers for processing. Alternatively, you may specify
 * "callback" properties that specify a function closure to call, with the
 * "arg" property containing either a single arg or an array of args.
 *
 * Example dataProvider array:
 * [ { label: "Go home", icon: homeIconClass,
 *     command: Controller.GO_HOME, arg: homeId },
 *   { type: "separator"},
     { label: "Crazytown", callback: setCrazy, arg: [ true, false ] },
 *   { label: "Other places", children: subMenuArray }
 * ];
 *
 * See "Defining menu structure and data" in the Flex manual for the
 * full list.
 */ 
public class CommandMenu extends ScrollableArrowMenu
{
    public function CommandMenu ()
    {
        super();
        verticalScrollPolicy = ScrollPolicy.OFF;

        addEventListener(MenuEvent.ITEM_CLICK, itemClicked);
    }

    /**
     * Factory method to create a command menu.
     *
     * @param items an array of menu items.
     */
    public static function createMenu (items :Array) :CommandMenu
    {
        var menu :CommandMenu = new CommandMenu();
        menu.owner = DisplayObjectContainer(Application.application);
        menu.tabEnabled = false;
        menu.showRoot = true;
        Menu.popUpMenu(menu, null, items);
        return menu;
    }

    /**
     * Actually pop up the menu. This can be used instead of show().
     */
    public function popUp (
        trigger :DisplayObject, popUpwards :Boolean = false) :void
    {
        var r :Rectangle = trigger.getBounds(trigger.stage);

        if (popUpwards) {
            show(r.x, 0);
            // then, reposition the y once we know our size
            y = r.y - getExplicitOrMeasuredHeight();

        } else {
            // position it below the trigger
            show(r.x, r.y + r.height);
        }
    }

    /**
     * Just like our superclass's show(), except that when invoked
     * with no args, causes the menu to show at the current mouse location
     * instead of the top-left corner of the application.
     */
    override public function show (xShow :Object = null, yShow :Object = null) :void
    {
        if (xShow == null) {
            xShow = DisplayObject(Application.application).mouseX;
        }
        if (yShow == null) {
            yShow = DisplayObject(Application.application).mouseY;
        }
        super.show(xShow, yShow);
    }

    /**
     * Callback for MenuEvent.ITEM_CLICK.
     */
    protected function itemClicked (event :MenuEvent) :void
    {
        var arg :Object = getItemProp(event.item, "arg");
        var cmdOrFn :Object = getItemProp(event.item, "command");
        if (cmdOrFn == null) {
            cmdOrFn = getItemProp(event.item, "callback");
        }
        if (cmdOrFn != null) {
            event.stopImmediatePropagation();
            CommandEvent.dispatch(mx_internal::parentDisplayObject, cmdOrFn, arg)
        }
        // else: no warning. There may be non-command menu items mixed in.
    }

    /**
     * Get the specified property for the specified item, if any.
     * Somewhat similar to bits in the DefaultDataDescriptor.
     */
    protected function getItemProp (item :Object, prop :String) :Object
    {
        try {
            if (item is XML) {
                return String((item as XML).attribute(prop));

            } else if (prop in item) {
                return item[prop];
            }

        } catch (e :Error) {
            // alas; fall through
        }
        return null;
    }
}
}
