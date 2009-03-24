//
// $Id$

package com.threerings.flex {

import mx.core.IFactory;
import mx.core.ScrollPolicy;

import mx.collections.ArrayCollection;
import mx.collections.Sort;

import mx.controls.List;

import com.threerings.util.Util;

import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.AttributeChangeAdapter;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.DSet_Entry;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.SetAdapter;

/**
 * A list that renders the contents of a DSet.
 */
public class DSetList extends List
{
    public function DSetList (
        renderer :IFactory, sortFn :Function = null, filterFn :Function = null)
    {
        horizontalScrollPolicy = ScrollPolicy.OFF;
        verticalScrollPolicy = ScrollPolicy.ON;
        selectable = false;

        itemRenderer = renderer;

        _data = new ArrayCollection();
        var sort :Sort = new Sort();
        sort.compareFunction = sortFn;
        _data.sort = sort;
        _data.filterFunction = filterFn;
        _data.refresh();
        dataProvider = _data;
    }

    /**
     * Set the aata to be displayed by this List, if not being attached to a DObject.
     */
    public function setData (array :Array /* of DSet_Entry */) :void
    {
        _data.source = (array == null) ? [] : array;
        refresh();
    }

    /**
     * Refresh the data, which can be called if something external has changed
     * the sort order or the rendering.
     */
    public function refresh () :void
    {
        _data.refresh();
    }

    /**
     * Initialize listening on the specified object.
     *
     * @param object the DObject on which to listen.
     * @param field the name of the DSet field that we're displaying.
     * @param refreshFields additional fields that cause refresh() to be called should
     *        AttributeChangedEvents arrive on them.
     */
    public function init (object :DObject, field :String, ... refreshFields) :void
    {
        shutdown();

        _object = object;
        _field = field;
        _refreshFields = refreshFields;
        _attrListener = new AttributeChangeAdapter(attrChanged);
        _setListener = new SetAdapter(entryAdded, entryUpdated, entryRemoved);
        _object.addListener(_attrListener);
        _object.addListener(_setListener);

        setEntries(_object[_field] as DSet);
    }

    /**
     * Shut down listening.
     */
    public function shutdown () :void
    {
        if (_attrListener != null) {
            _object.removeListener(_attrListener);
            _object.removeListener(_setListener);
            _attrListener = null;
            _setListener = null;
            _object = null;
            _field = null;
            _refreshFields = null;
            setData(null);
        }
    }

    /**
     * Safely set the entries, even if null.
     */
    protected function setEntries (entries :DSet) :void
    {
        setData((entries == null) ? null : entries.toArray());
    }

    /**
     * Handle a change to the entire set.
     */
    protected function attrChanged (event :AttributeChangedEvent) :void
    {
        var name :String = event.getName();
        if (name == _field) {
            setEntries(event.getValue() as DSet);

        } else if (_refreshFields.indexOf(name) != -1) {
            refresh();
        }
    }

    /**
     * Handle an addition to the set.
     */
    protected function entryAdded (event :EntryAddedEvent) :void
    {
        if (event.getName() == _field) {
            _data.list.addItem(event.getEntry());
            refresh();
        }
    }

    /**
     * Handle an update to the set.
     */
    protected function entryUpdated (event :EntryUpdatedEvent) :void
    {
        if (event.getName() == _field) {
            var entry :DSet_Entry = event.getEntry();
            var idx :int = findKeyIndex(entry.getKey());
            if (idx == -1) {
                throw new Error();
            }
            _data.list.setItemAt(entry, idx);
            refresh();
        }
    }

    /**
     * Handle a removal from the set.
     */
    protected function entryRemoved (event :EntryRemovedEvent) :void
    {
        if (event.getName() == _field) {
            var idx :int = findKeyIndex(event.getKey());
            if (idx != -1) {
                _data.list.removeItemAt(idx);
                refresh();
            }
        }
    }

    /**
     * Find the index of the specified entry key from the raw list.
     */
    protected function findKeyIndex (key :Object) :int
    {
        for (var ii :int = 0; ii < _data.list.length; ii++) {
            if (Util.equals(key, DSet_Entry(_data.list.getItemAt(ii)).getKey())) {
                return ii;
            }
        }
        return -1;
    }

    /** The collection for the List, This is the filtered/sorted view. For the
     * the raw list, we access _data.list. */
    protected var _data :ArrayCollection;

    /** The object on which we're listening. */
    protected var _object :DObject;

    /** The fieldname of the DSet in the _object. */
    protected var _field :String;

    /** Fields on which to watch for updates and call refresh(). */
    protected var _refreshFields :Array;

    /** Our listeners, so that we don't junk-up our public interface. */
    protected var _attrListener :AttributeChangeAdapter;
    protected var _setListener :SetAdapter;
}
}
