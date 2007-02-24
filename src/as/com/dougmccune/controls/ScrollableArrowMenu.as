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

package com.dougmccune.controls
{
    import flash.display.DisplayObjectContainer;
    import flash.events.Event;
    import flash.events.MouseEvent;
    import flash.events.TimerEvent;
    import flash.utils.Timer;
    
    import mx.controls.Button;
    import mx.controls.List;
    import mx.controls.Menu;
    import mx.controls.listClasses.IListItemRenderer;
    import mx.controls.listClasses.ListBase;
    import mx.controls.menuClasses.IMenuItemRenderer;
    import mx.controls.scrollClasses.ScrollBar;
    import mx.core.Application;
    import mx.core.ScrollControlBase;
    import mx.core.ScrollPolicy;
    import mx.core.mx_internal;
    import mx.events.ScrollEvent;
    import mx.managers.PopUpManager;
    
    use namespace mx_internal;
    
    public class ScrollableArrowMenu extends ScrollableMenu
    {
        //The buttons that are used for the scrolling up and down
        private var upButton:Button;
        private var downButton:Button;
        
        //We use a timer to control the scrolling while the mouse is over the buttons
        private var timer:Timer;
        
        //scrollSpeed is the delay between scrolling the list, so a smaller number
        //here will increase the speed of the scrolling. This is in ms.
        public var scrollSpeed:Number = 80;
        
        //scrollJump specifies how many rows to scroll each time. Leaving it at 1 makes
        //for the smoothest scrolling
           public var scrollJump:Number = 1;
           
           
           /**
            * We are adding a new property called arrowScrollPolicy. This is just like 
            * verticalScrollPolicy, except it controls how we display the up and down arrows
            * for scrolling. If this is set to ScrollPolicy.OFF we never show the arrows.
            * If it's ScrollPolicy.ON we always show the arrows. And if it's ScrollPolicy.AUTO
            * then we show the arrows if they are needed. OFF and AUTO are the only ones
            * that should probably be used, since ON gets in the way of the first menu item
            * in the list.
            */
           private var _arrowScrollPolicy:String = ScrollPolicy.AUTO;
           
           public function get arrowScrollPolicy():String {
               return _arrowScrollPolicy;
           }
           public function set arrowScrollPolicy(value:String):void {
               this._arrowScrollPolicy = value;
               
               invalidateDisplayList();
           }
        
        /* A few icons that we'll use for the up and down buttons */
        [Embed("../skins/menu_assets.swf#up_arrow")]
        public var up_icon:Class;
        
        [Embed("../skins/menu_assets.swf#down_arrow")]
        public var down_icon:Class;
        
        [Embed("../skins/menu_assets.swf#up_arrow_disabled")]
        public var up_icon_disabled:Class;
        
        [Embed("../skins/menu_assets.swf#down_arrow_disabled")]
        public var down_icon_disabled:Class;
        
        /**
         * We have to override the static function createMenu so that we create a 
         * ScrollableMenu instead of a normal Menu.
         */ 
        public static function createMenu(parent:DisplayObjectContainer, mdp:Object, showRoot:Boolean=true):ScrollableArrowMenu
        {    
            var menu:ScrollableArrowMenu = new ScrollableArrowMenu();
            menu.tabEnabled = false;
            
            menu.owner = DisplayObjectContainer(Application.application);
            menu.showRoot = showRoot;
            popUpMenu(menu, parent, mdp);
            return menu;
        }
        
        public function ScrollableArrowMenu()
        {
            super();
        }
        
        /**
         * We override createChildren so we can instantiate our up and down buttons
         * and add them as children.
         */
        override protected function createChildren():void {
            super.createChildren();
            
            upButton = new Button();
            upButton.setStyle("cornerRadius", 0);
            upButton.setStyle("fillAlphas", [1,1,1,1]);
            
            downButton = new Button();
            downButton.setStyle("cornerRadius", 0);
            downButton.setStyle("fillAlphas", [1,1,1,1]);
            
            upButton.setStyle("icon", up_icon);
            downButton.setStyle("icon", down_icon);
            upButton.setStyle("disabledIcon", up_icon_disabled);
            downButton.setStyle("disabledIcon", down_icon_disabled);
            
            addChild(upButton);
            addChild(downButton);
            
            upButton.addEventListener(MouseEvent.ROLL_OVER, startScrollingUp);
            upButton.addEventListener(MouseEvent.ROLL_OUT, stopScrolling);
            
            downButton.addEventListener(MouseEvent.ROLL_OVER, startScrollingDown);
            downButton.addEventListener(MouseEvent.ROLL_OUT, stopScrolling);
            
            //we're using an event listener to check if we should still be showing the
            //up and down buttons. This checks every time the list is scrolled at all.
            this.addEventListener(ScrollEvent.SCROLL, checkButtons);
        }
        
        /**
        * We need to override openSubMenu as well, so that any subMenus opened by this Menu controls
        * will also be ScrollableMenus and will have the same maxHeight set
        */
        override mx_internal function openSubMenu(row:IListItemRenderer):void
        {
            supposedToLoseFocus = true;
    
            var r:Menu = getRootMenu();
            var menu:ScrollableArrowMenu;
    
            // check to see if the menu exists, if not create it
            if (!IMenuItemRenderer(row).menu)
            {
                /* The only differences between this method and the original method in mx.controls.Menu
                 * are these four lines.
                 */
                menu = new ScrollableArrowMenu();
                menu.maxHeight = this.maxHeight;
                menu.verticalScrollPolicy = this.verticalScrollPolicy;
                menu.arrowScrollPolicy = this.arrowScrollPolicy;
                
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
                if (row.data && 
                    _dataDescriptor.isBranch(row.data) &&
                    _dataDescriptor.hasChildren(row.data))
                {
                    menu.dataProvider = _dataDescriptor.getChildren(row.data);
                }
                menu.sourceMenuBar = sourceMenuBar;
                menu.sourceMenuBarItem = sourceMenuBarItem;

                IMenuItemRenderer(row).menu = menu;
                PopUpManager.addPopUp(menu, r, false);
            }
            
            super.openSubMenu(row);
        }
        
        /**
         * We've got to layout the up and down buttons now. They are overlaid on the list
         * at the very top and bottom.
         */
        override protected  function updateDisplayList(unscaledWidth:Number, unscaledHeight:Number):void {
            measure();
            
            super.updateDisplayList(unscaledWidth, unscaledHeight);
            
            
            var w:Number = unscaledWidth;
            
            if(verticalScrollBar) {
                w = unscaledWidth - ScrollBar.THICKNESS;
            }
            
            upButton.setActualSize(w, 15);
            downButton.setActualSize(w, 15);
            
            upButton.move(0, 0);
            downButton.move(0, measuredHeight - downButton.height);
            
            checkButtons(null);
        }
        
        /**
         * This method is used to hide or show the up and down buttons, depending on where we
         * are scrolled in the list and what the setting of arrowScrollPolicy is
         */
        private function checkButtons(event:Event):void {
            if(this.arrowScrollPolicy == ScrollPolicy.AUTO) {
                upButton.visible = upButton.enabled = (this.verticalScrollPosition != 0);
                downButton.visible = downButton.enabled = (this.verticalScrollPosition != this.maxVerticalScrollPosition);
            }
            else if(this.arrowScrollPolicy == ScrollPolicy.ON) {
                upButton.visible = downButton.visible = true;
                upButton.enabled = (this.verticalScrollPosition != 0);
                downButton.enabled = (this.verticalScrollPosition != this.maxVerticalScrollPosition);
            }
            else {
                upButton.visible = upButton.enabled = downButton.visible = downButton.enabled = false;
            }
        }
        
        /**
         * We start a timer that updates the verticalScrollPosition at a regular interval
         * until the mouse rolls off the button.
         */
        private function startScrollingUp(event:Event):void {
            if(timer && timer.running) {
                timer.stop();
            }
            
            timer = new Timer(this.scrollSpeed);
            timer.addEventListener(TimerEvent.TIMER, scrollUp);
            
            timer.start();
        }
        
        private function startScrollingDown(event:Event):void {
            if(timer && timer.running) {
                timer.stop();
            }
            
            timer = new Timer(this.scrollSpeed);
            timer.addEventListener(TimerEvent.TIMER, scrollDown);
            
            timer.start();
        }
        
        private function stopScrolling(event:Event):void {
            
            stage.removeEventListener(MouseEvent.MOUSE_UP, stopScrolling);
            
            if(timer && timer.running) {
                timer.stop();
            }
        }
        
        private function scrollUp(event:TimerEvent):void {
            if(this.verticalScrollPosition - scrollJump > 0) {
                this.verticalScrollPosition -= scrollJump;
            }
            else {
                this.verticalScrollPosition = 0;
            }
            
            
            checkButtons(null);
        }
        
        private function scrollDown(event:TimerEvent):void {
            if(this.verticalScrollPosition + scrollJump < this.maxVerticalScrollPosition) {
                this.verticalScrollPosition += scrollJump;
            }
            else {
                this.verticalScrollPosition = this.maxVerticalScrollPosition;
            }
            
            
            checkButtons(null);
        }
        
    }
}

