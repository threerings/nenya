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
import flash.events.IEventDispatcher;

import flash.geom.Point;
import flash.geom.Rectangle;

import mx.controls.Menu;
import mx.controls.listClasses.BaseListData;
import mx.controls.listClasses.IListItemRenderer;
import mx.controls.menuClasses.IMenuItemRenderer;
import mx.controls.menuClasses.MenuListData;

import mx.core.mx_internal;
import mx.core.Application;
import mx.core.ClassFactory;
import mx.core.IFlexDisplayObject;
import mx.core.ScrollPolicy;

import mx.events.MenuEvent;

import mx.managers.PopUpManager;

import mx.utils.ObjectUtil;

import flexlib.controls.ScrollableArrowMenu;

import com.threerings.util.CommandEvent;

import com.threerings.flex.PopUpUtil;

import com.threerings.flex.menuClasses.CommandMenuItemRenderer;
import com.threerings.flex.menuClasses.CommandListData;

use namespace mx_internal;

/**
 * A pretty standard menu that can submit CommandEvents if menu items have "command" and possibly
 * "arg" properties. Commands are submitted to controllers for processing. Alternatively, you may
 * specify "callback" properties that specify a function closure to call, with the "arg" property
 * containing either a single arg or an array of args.
 *
 * Another property now supported is "iconObject", which is an already-instantiated
 * IFlexDisplayObject to use as the icon. This will only be applied if the "icon" property
 * (which specifies a Class) is not used.
 *
 * Example dataProvider array:
 * [ { label: "Go home", icon: homeIconClass,
 *     command: Controller.GO_HOME, arg: homeId },
 *   { type: "separator"},
     { label: "Crazytown", callback: setCrazy, arg: [ true, false ] },
 *   { label: "Other places", children: subMenuArray }
 * ];
 *
 * See "Defining menu structure and data" in the Flex manual for the full list.
 */
public class CommandMenu extends Menu
{
    /**
     * Factory method to create a command menu.
     *
     * @param items an array of menu items.
     * @param dispatcher an override event dispatcher to use for command events, rather than
     * our parent.
     */
    public static function createMenu (
        items :Array, dispatcher :IEventDispatcher = null) :CommandMenu
    {
        var menu :CommandMenu = new CommandMenu();
        menu.owner = DisplayObjectContainer(Application.application);
        menu.tabEnabled = false;
        menu.showRoot = true;
        menu.setDispatcher(dispatcher);
        Menu.popUpMenu(menu, null, items); // does not actually pop up, but needed.
        return menu;
    }

    public function CommandMenu ()
    {
        super();

        itemRenderer = new ClassFactory(CommandMenuItemRenderer);

        verticalScrollPolicy = ScrollPolicy.OFF;
        addEventListener(MenuEvent.ITEM_CLICK, itemClicked);
    }

    /**
     * Configures the event dispatcher to be used when dispatching this menu's command events.
     * Normally they will be dispatched on our parent (usually the SystemManager or something).
     */
    public function setDispatcher (dispatcher :IEventDispatcher) :void
    {
        _dispatcher = dispatcher;
    }

    /**
     * Actually pop up the menu. This can be used instead of show().
     */
    public function popUp (
        trigger :DisplayObject, popUpwards :Boolean = false, popLeftwards :Boolean = false) :void
    {
        _upping = popUpwards;
        _lefting = popLeftwards;
        _fitting = null;
        var r :Rectangle = trigger.getBounds(trigger.stage);
        show(_lefting ? r.left : r.right, _upping ? r.top : r.bottom);
    }

    /**
     * Shows the menu at the specified mouse coordinates.
     */
    public function popUpAt (
        mx :int, my :int, popUpwards :Boolean = false, popLeftwards :Boolean = false) :void
    {
        _upping = popUpwards;
        _lefting = popLeftwards;
        _fitting = null;
        show(mx, my);
    }

    /**
     * Shows the menu at the current mouse location.
     */
    public function popUpAtMouse (popUpwards :Boolean = false, popLeftwards :Boolean = false) :void
    {
        _upping = popUpwards;
        _lefting = popLeftwards;
        _fitting = null;
        show(); // our show, with no args, pops at the mouse
    }

    /**
     * Just like our superclass's show(), except that when invoked with no args, causes the menu to
     * show at the current mouse location instead of the top-left corner of the application.
     * Also, we ensure that the resulting menu is in-bounds.
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

        // reposition now that we know our size
        if (_lefting) {
            y = x - getExplicitOrMeasuredWidth();
        }
        if (_upping) {
            y = y - getExplicitOrMeasuredHeight();
        }

        PopUpUtil.fit(this);
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
            CommandEvent.dispatch(_dispatcher == null ? mx_internal::parentDisplayObject :
                                  _dispatcher, cmdOrFn, arg);
        }
        // else: no warning. There may be non-command menu items mixed in.
    }

    // from ScrollableArrowMenu..
    override mx_internal function openSubMenu (row :IListItemRenderer) :void
    {
        supposedToLoseFocus = true;

        var r :Menu = getRootMenu();
        var menu :CommandMenu;

        // check to see if the menu exists, if not create it
        if (!IMenuItemRenderer(row).menu) {
            // the only differences between this method and the original method in mx.controls.Menu
            // are these few lines.
            menu = new CommandMenu();
            menu.maxHeight = this.maxHeight;
            menu.verticalScrollPolicy = this.verticalScrollPolicy;
            menu.variableRowHeight = this.variableRowHeight;

            menu.parentMenu = this;
            menu.owner = this;
            menu.showRoot = showRoot;
            menu.dataDescriptor = r.dataDescriptor;
            menu.styleName = r;
            menu.labelField = r.labelField;
            menu.labelFunction = r.labelFunction;
            menu.iconField = r.iconField;
            menu.iconFunction = r.iconFunction;
            menu.itemRenderer = r.itemRenderer;
            menu.rowHeight = r.rowHeight;
            menu.scaleY = r.scaleY;
            menu.scaleX = r.scaleX;

            // if there's data and it has children then add the items
            if (row.data && _dataDescriptor.isBranch(row.data) &&
                    _dataDescriptor.hasChildren(row.data)) {
                menu.dataProvider = _dataDescriptor.getChildren(row.data);
            }
            menu.sourceMenuBar = sourceMenuBar;
            menu.sourceMenuBarItem = sourceMenuBarItem;

            IMenuItemRenderer(row).menu = menu;
            PopUpManager.addPopUp(menu, r, false);
        }

        super.openSubMenu(row);

        // if we're lefting, upping or fitting make sure our submenu does so as well
        var submenu :Menu = IMenuItemRenderer(row).menu;
        var lefting :Boolean = _lefting;
        var upping :Boolean = _upping;
        if (_fitting != null) {
            if (submenu.x + submenu.getExplicitOrMeasuredWidth() > _fitting.right) {
                lefting = true;
            }
            if (submenu.y + submenu.getExplicitOrMeasuredHeight() > _fitting.bottom) {
                upping = true;
            }
        }
        if (lefting) {
            submenu.x -= submenu.getExplicitOrMeasuredWidth();
        }
        if (upping) {
            submenu.y -= (submenu.getExplicitOrMeasuredHeight() - DisplayObject(row).height);
        }
        // if we've poped out of the boundaries, just snap it to the lower right corner
        if (_fitting != null) {
            if (submenu.x < _fitting.left) {
                submenu.x = Math.max(
                        _fitting.right - submenu.getExplicitOrMeasuredWidth(), _fitting.left);
            }
            if (submenu.y < _fitting.top) {
                submenu.y = Math.max(
                        _fitting.bottom - submenu.getExplicitOrMeasuredHeight(), _fitting.top);
            }
        }
    }

    // from List
    override protected function makeListData (data :Object, uid :String, rowNum :int) :BaseListData
    {
        // Oh, FFS.
        // We need to set up these "maxMeasuredIconWidth" fields on the MenuListData, but our
        // superclass has made those variables private.
        // We can get the values out of another MenuListData, so we just always call super()
        // to create one of those, and if we need to make a CommandListData, we construct one
        // from the fields in the MenuListData.

        var menuListData :MenuListData = super.makeListData(data, uid, rowNum) as MenuListData;

        var iconObject :IFlexDisplayObject = getItemProp(data, "iconObject") as IFlexDisplayObject;
        if (iconObject != null) {
            var cmdListData :CommandListData = new CommandListData(menuListData.label,
                menuListData.icon, iconObject, labelField, uid, this, rowNum);

            cmdListData.maxMeasuredIconWidth = menuListData.maxMeasuredIconWidth;
            cmdListData.maxMeasuredTypeIconWidth = menuListData.maxMeasuredTypeIconWidth;
            cmdListData.maxMeasuredBranchIconWidth = menuListData.maxMeasuredBranchIconWidth;
            cmdListData.useTwoColumns = menuListData.useTwoColumns;
            return cmdListData;
        }

        return menuListData;
    }

    /**
     * Get the specified property for the specified item, if any.  Somewhat similar to bits in the
     * DefaultDataDescriptor.
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

    protected var _dispatcher :IEventDispatcher;
    protected var _fitting :Rectangle;
    protected var _lefting :Boolean = false;
    protected var _upping :Boolean = false;
}
}
