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
}
}
