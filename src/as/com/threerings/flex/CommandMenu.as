package com.threerings.flex {

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;

import flash.geom.Rectangle;

import mx.controls.Menu;
import mx.core.mx_internal;
import mx.core.Application;
import mx.core.ScrollPolicy;
import mx.events.MenuEvent;

import com.dougmccune.controls.ScrollableArrowMenu;

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
        var arg :Object = getItemArgument(event.item);
        var cmd :String = getItemCommand(event.item);
        var fn :Function;
        if (cmd == null) {
            fn = getItemCallback(event.item);
        }
        if (cmd != null || fn != null) {
            event.stopImmediatePropagation();
            if (cmd != null) {
                CommandEvent.dispatch(mx_internal::parentDisplayObject, cmd, arg);

            } else {
                try {
                    var args :Array = (arg as Array);
                    if (args == null && arg != null) {
                        args = [ arg ];
                    }
                    fn.apply(null, args);

                } catch (err :Error) {
                    Log.getLog(this).warning("Unable to call menu callback: " +
                        event.item);
                }
            }
        }
    }

    /**
     * Get the command for the specified item, if any.
     * Somewhat similar to bits in the DefaultDataDescriptor.
     */
    protected function getItemCommand (item :Object) :String
    {
        try {
            if (item is XML) {
                return (item.@command as String);

            } else if (item is Object) {
                return (item.command as String);
            }
        } catch (e :Error) {
            // fall through
        }

        return null;
    }

    /**
     * Get the callback function for the specified item, if any.
     */
    protected function getItemCallback (item :Object) :Function
    {
        try {
            if (item is XML) {
                return (item.@callback as Function);

            } else if (item is Object) {
                return (item.callback as Function);
            }
        } catch (e :Error) {
            // fall through
        }

        return null;
    }

    /**
     * Get the command for the specified item, if any.
     * Somewhat similar to bits in the DefaultDataDescriptor.
     */
    protected function getItemArgument (item :Object) :Object
    {
        try {
            if (item is XML) {
                return item.@arg;

            } else if (item is Object) {
                return item.arg;
            }
        } catch (e :Error) {
            // fall through
        }

        return null;
    }
}
}
