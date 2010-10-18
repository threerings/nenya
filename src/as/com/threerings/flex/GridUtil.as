//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
// http://code.google.com/p/nenya/
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

import mx.containers.Grid;
import mx.containers.GridItem;
import mx.containers.GridRow;

import mx.controls.Label;

import mx.core.UIComponent;

/**
 * Convenience methods for adding children to Grids.
 */
public class GridUtil
{
    /**
     * Add a new row to the grid, containing the specified
     * components.
     * 
     * @param specs a list of components or Strings, or you can follow
     * any component with a two-dimensional array that specifies
     * grid width/height.
     *
     * Example: addRow(grid, "labeltxt", _entryField, _bigThing, [2, 2], _smallThing);
     *
     * All will be put in the same row, but bigThing will have
     *  colspan=2 rowspan=2
     */
    public static function addRow (grid :Grid, ... specs) :GridRow
    {
        var row :GridRow = new GridRow();
        var lastItem :GridItem;
        for each (var o :Object in specs) {
            if (o is String) {
                var lbl :Label = new Label();
                lbl.text = String(o);
                o = lbl;
            }
            if (o is UIComponent) {
                lastItem = addToRow(row, UIComponent(o));

            } else if (o is Array) {
                var arr :Array = (o as Array);
                lastItem.colSpan = int(arr[0]);
                lastItem.rowSpan = int(arr[1]);

            } else {
                throw new ArgumentError();
            }
        }
        grid.addChild(row);
        return row;
    }

    /**
     * Add a child to the specified grid row, returning the
     * GridItem created for containment of the child.
     */
    public static function addToRow (row :GridRow, comp :UIComponent) :GridItem
    {
        var item :GridItem = new GridItem();
        item.addChild(comp);
        row.addChild(item);
        return item;
    }

    /**
     * Get the number of cells in a grid.
     */
    public static function getCellCount (grid :Grid) :int
    {
        var count :int = 0;
        for (var ii :int = 0; ii < grid.numChildren; ii++) {
            count += (grid.getChildAt(ii) as GridRow).numChildren;
        }
        return count;
    }

    /**
     * Return the contents of the specified cell. Not super fast, as it
     * checks each row in order.
     */
    public static function getCellAt (grid :Grid, index :int) :UIComponent
    {
        for (var ii :int = 0; ii < grid.numChildren; ii++) {
            var row :GridRow = grid.getChildAt(ii) as GridRow;
            if (index < row.numChildren) {
                return (row.getChildAt(index) as GridItem).getChildAt(0) as UIComponent;
            } else {
                index -= row.numChildren; // and then on to the next row
            }
        }
        return null; // never found it. throw an out-of-range error?
    }
}
}
