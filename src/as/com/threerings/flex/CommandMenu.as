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

import flash.events.Event;
import flash.events.IEventDispatcher;
import flash.events.MouseEvent;

import flash.geom.Point;
import flash.geom.Rectangle;

import mx.controls.Button;
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
 * Another 'type' now supported is "title", which gives the label for that item the style
 * '.menuTitle' and disables it. Your ".menuTitle" style should use the disabledColor.
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
 *
 * CommandMenu is scrollable.  This can be forced by setting verticalScrollPolicy to 
 * ScrollPolicy.AUTO or ScrollPolicy.ON and setting the maxHeight.  Also, if the scrolling isn't
 * forced on, but the menu does not fit within the vertical bounds given (either the stage size by
 * default, or the height of the rectangle given in setBounds()), scrolling will be turned on
 * so that none of the content is lost.
 *
 * Note: we don't extend flexlib's ScrollableMenu (or its sub-class ScrollableArrowMenu) because it
 * does some weird stuff in measure() that forces some of our menus down to a very small height...
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

    /**
     * Add a title to the TOP of the specified menu.
     *
     * @param icon may be a Class or IFlexDisplayObject.
     */
    public static function addTitle (
        menuItems :Array, title :String, icon :* = null, separatorAfter :Boolean = true) :void
    {
        var o :Object = { label: title, type: "title" };
        if (icon is Class) {
            o["icon"] = icon;
        } else if (icon is IFlexDisplayObject) {
            o["iconObject"] = icon;
        }
        if (separatorAfter) {
            menuItems.unshift({ type: "separator" });
        }
        menuItems.unshift(o);
    }

    /**
     * Add a separator to the specified menu, unless it would not make sense to do so, because
     * the menu is empty or already ends with a separator.
     */
    public static function addSeparator (menuItems :Array) :void
    {
        const len :int = menuItems.length;
        if (len > 0 && menuItems[len - 1].type != "separator") {
            menuItems.push({ type: "separator" });
        }
    }

    /**
     * The mx.controls.Menu class overrides setting and getting the verticalScrollPolicy
     * basically setting the verticalScrollPolicy did nothing, and getting it always returned
     * ScrollPolicy.OFF.  So that's not going to work if we want the menu to scroll. Here we 
     * reinstate the verticalScrollPolicy setter, and keep a local copy of the value in a 
     * protected variable _verticalScrollPolicy.
     * 
     * This setter is basically a copy of what ScrollControlBase and ListBase do.
     */
    override public function set verticalScrollPolicy (value :String) :void 
    {
        var newPolicy :String = value.toLowerCase();

        itemsSizeChanged = true;

        if (_verticalScrollPolicy != newPolicy)
        {
            _verticalScrollPolicy = newPolicy;
            dispatchEvent(new Event("verticalScrollPolicyChanged"));
        }

        invalidateDisplayList();
    }

    override public function get verticalScrollPolicy () :String
    {
        return _verticalScrollPolicy;
    }

    public function CommandMenu ()
    {
        super();

        itemRenderer = new ClassFactory(getItemRenderer());

        verticalScrollPolicy = ScrollPolicy.OFF;
        variableRowHeight = true;
        addEventListener(MenuEvent.ITEM_CLICK, itemClicked);
        addEventListener(MenuEvent.MENU_HIDE, menuHidden);
    }

    /** 
     * Called in the CommandMenu constructor, this should return the item rendering class for this
     * CommandMenu.
     */
    protected function getItemRenderer () :Class
    {
        return CommandMenuItemRenderer;
    }

    /**
     * Configure the button that popped-up this menu, which enables two features:
     * 1) If the button is clicked again while the menu is up, it won't re-trigger, the menu will
     *    just close.
     * 2) However the menu closes, if the button is a toggle, it will be de-selected.
     */
    public function setTriggerButton (trigger :Button) :void
    {
        _trigger = trigger;
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
     * Sets a Rectangle (in stage coords) that the menu will attempt to keep submenus positioned 
     * within.  If a submenu is too large to fit, it will position it in the lower right corner
     * of this Rectangle (or top if popping upwards, or left if popping leftwards).   This
     * value defaults to the stage bounds.
     */
    public function setBounds (fitting :Rectangle) :void
    {
        _fitting = fitting;
    }

    /**
     * Actually pop up the menu. This can be used instead of show().
     */
    public function popUp (
        trigger :DisplayObject, popUpwards :Boolean = false, popLeftwards :Boolean = false) :void
    {
        _upping = popUpwards;
        _lefting = popLeftwards;
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
        show(mx, my);
    }

    /**
     * Shows the menu at the current mouse location.
     */
    public function popUpAtMouse (popUpwards :Boolean = false, popLeftwards :Boolean = false) :void
    {
        _upping = popUpwards;
        _lefting = popLeftwards;
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

        var fitting :Rectangle = _fitting || screen;
        PopUpUtil.fitInRect(this, fitting);

        // if after fitting as best we can, the menu is outside of the declared bounds, we force
        // scrolling and set the max height
        if (y < fitting.y) {
            verticalScrollPolicy = ScrollPolicy.AUTO;
            maxHeight = fitting.height;
            y = fitting.y;
        }

        // set up some stuff to clean up and behave
        systemManager.topLevelSystemManager.getSandboxRoot().addEventListener(
            MouseEvent.MOUSE_DOWN, handleMouseDownOutside, false, 1, true);
    }

    override public function hide () :void
    {
        super.hide();
        systemManager.topLevelSystemManager.getSandboxRoot().removeEventListener(
            MouseEvent.MOUSE_DOWN, handleMouseDownOutside);
    }

    /** 
     * The Menu class overrode configureScrollBars() and made the function do nothing.  That means
     * the scrollbars don't know how to draw themselves, so here we reinstate configureScrollBars.
     * This is basically a copy of the same method from the mx.controls.List class.
     */
    override protected function configureScrollBars () :void
    {
        var rowCount :int = listItems.length;
        if (rowCount == 0) {
            return;
        }

        // if there is more than one row and it is a partial row we don't count it
        if (rowCount > 1 && 
            rowInfo[rowCount - 1].y + rowInfo[rowCount - 1].height > listContent.height) {
            rowCount--;
        }

        // offset, when added to rowCount, is hte index of the dataProvider item for that row.  
        // IOW, row 10 in listItems is showing dataProvider item 10 + verticalScrollPosition - 
        // lockedRowCount - 1
        var offset :int = verticalScrollPosition - lockedRowCount - 1;

        // don't count filler rows at the bottom either.
        var fillerRows :int = 0;
        while (rowCount > 0 && listItems[rowCount - 1].length == 0)
        {
            if (collection && rowCount + offset >= collection.length) {
                rowCount--;
                fillerRows++;
            } else {
                break;
            }
        }

        var colCount :int = listItems[0].length;
        var oldHorizontalScrollBar :Object = horizontalScrollBar;
        var oldVerticalScrollBar :Object = verticalScrollBar;
        var roundedWidth :int = Math.round(unscaledWidth);
        var length :int = collection ? collection.length - lockedRowCount : 0;
        var numRows :int = rowCount - lockedRowCount;

        setScrollBarProperties(Math.round(listContent.width), roundedWidth, length, numRows);
        maxVerticalScrollPosition = Math.max(length - numRows, 0);
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

    /**
     * Handles MenuEvent.MENU_HIDE.
     */
    protected function menuHidden (event :MenuEvent) :void 
    {
        // don't react to submenu hides
        if ((event.menu == this) && (_trigger != null) && _trigger.toggle) {
            _trigger.selected = false;
        }
    }

    /**
     * Handle a down-popping click outside the menu.
     */
    protected function handleMouseDownOutside (event :MouseEvent) :void
    {
        if (event.target == _trigger) {
            _trigger.addEventListener(
                MouseEvent.CLICK, handleTriggerClick, false, int.MAX_VALUE, true);
        }
    }

    /**
     * Suppress the click that wants to land on the trigger button.
     */
    protected function handleTriggerClick (event :MouseEvent) :void
    {
        event.stopImmediatePropagation();
        _trigger.removeEventListener(MouseEvent.CLICK, handleTriggerClick);
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
        if (_lefting) {
            submenu.x -= submenu.getExplicitOrMeasuredWidth();
        }
        if (_upping) {
            var rowLoc :Point = row.localToGlobal(new Point());
            submenu.y = rowLoc.y - submenu.getExplicitOrMeasuredHeight() + row.height;
        }

        var fitting :Rectangle = _fitting || screen;
        PopUpUtil.fitInRect(submenu, fitting);
        // if after fitting as best we can, the menu is outside of the declared bounds, we force
        // scrolling and set the max height
        if (submenu.y < fitting.y) {
            submenu.verticalScrollPolicy = ScrollPolicy.AUTO;
            submenu.maxHeight = fitting.height;
            submenu.y = fitting.y;
        }
    }

    override protected function measure () :void
    {
        super.measure();

        if (measuredHeight > this.maxHeight) {
            measuredHeight = this.maxHeight;
        }

        if (verticalScrollPolicy == ScrollPolicy.ON || verticalScrollPolicy == ScrollPolicy.AUTO) {
            if (verticalScrollBar) {
                measuredMinWidth = measuredWidth = measuredWidth + verticalScrollBar.minWidth;
            }
        }

        commitProperties();
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
    protected var _trigger :Button;
    protected var _lefting :Boolean = false;
    protected var _upping :Boolean = false;
    protected var _fitting :Rectangle = null;
    protected var _verticalScrollPolicy :String;
}
}
