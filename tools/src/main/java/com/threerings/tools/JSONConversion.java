// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
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

package com.threerings.tools;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.util.ClassUtil;
import com.samskivert.util.StringUtil;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


/**
 * Utilities for converting instances of java objects to json. This is basically automatic for
 * simple classes, but field enumeration and overall conversion can be customized. <br/><br/>
 *
 * Example: <pre>
 *    Config cfg = new Config();
 *    System.out.println(cfg.convert(new Pojo()));
 * </pre>
 * Converting a type universally: <pre>{@code
    Converter&lt;Color&gt; colorConv = new Converter&lt;Color&gt;() {
        public Object convert (Color c, Config cfg) {
            return new Integer((c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue());
        }
    };

    cfg.addClassConverter(Color.class, colorConv);
 * }</pre>
 * Customizing a specific field of a class:<pre>{@code
    FieldAccessor&lt;SomeImpl, Collection&lt;?&gt;&gt; entryConv =
            new FieldAccessor&lt;SomeImpl, Collection&lt;?&gt;&gt;() {
        public Collection&lt;SomeValue&gt; get (SomeImpl impl) {
            return impl.getValuesAsCollection();
        }
    };
    cfg.addFieldConfig(SomeImpl.class, new FieldConfig&lt;SomeImpl&gt;().
        addExclusions("privateStuff").
        addFieldConverter("valueMap", new ConvertCollectionFieldToArray&lt;SomeImpl&gt;(entryConv)));
 * }</pre>
 */
public class JSONConversion
{
    /**
     * Controls verbose logging for the class.
     */
    public static boolean vlog = false;

    /**
     * Logs the given arguments separated by spaces, if {@link #vlog} is set.
     */
    public static void vlog (Object...args)
    {
        if (vlog) {
            StringBuilder sb = new StringBuilder();
            sb.append("JSONConversion: ");
            StringUtil.toString(sb, args, "", "", " ");
            System.out.println(sb.toString());
        }
    }

    /**
     * Converts T to a suitable parameter for {@link JSONArray#add(Object)},
     * {@link JSONObject#element(String, Object)}, etc.
     */
    public static interface Converter<T>
    {
        /**
         * Returns an instance of {@code T} in json form. The cfg may be used to convert aggregated
         * objects.
         */
        Object convert (T obj, Config cfg);
    }

    /**
     * Plugs into some common cases for automating the conversion of special fields. For example
     * a map that is reconstructed from the values on deserialization.
     */
    public static interface FieldAccessor<T, F>
    {
        F get (T obj);
    }

    /**
     * Defines the field layout for serializing instances of {@code T}.
     */
    public static class FieldConfig<T>
    {
        /**
         * Fields that should not be serialized.
         */
        public List<String> excludeFields = Lists.newArrayList();

        /**
         * Fields with custom conversions.
         */
        public Map<String, Converter<?>> customFields = Maps.newHashMap();

        /**
         * Adds some excluded fields and returns this for chaining.
         */
        public FieldConfig<T> addExclusions (String ...names)
        {
            excludeFields.addAll(Arrays.asList(names));
            return this;
        }

        /**
         * Adds a custom converter for a field and returns this for chaining.
         */
        public FieldConfig<T> addFieldConverter (String name, Converter<T> field)
        {
            customFields.put(name, (Converter<?>)field);
            return this;
        }

        /**
         * Tests if a field ought to be excluded, based on current configuration.
         */
        public boolean excludeField (Field field)
        {
            int mask = Modifier.FINAL | Modifier.TRANSIENT | Modifier.STATIC;
            return (field.getModifiers() & mask) != 0 ||
                    excludeFields.contains(field.getName());
        }
    }

    /**
     * Defines the conversion of java objects to json. Works mostly by reflection, with
     * customizability.
     */
    public static class Config
    {
        /**
         * Overrides the field treatment for encountered instances.
         */
        public Map<Class<?>, FieldConfig<?>> classes = Maps.newHashMap();

        /**
         * Overrides the conversion process for encountered instances.
         */
        public Map<Class<?>, Converter<?>> converters = Maps.newHashMap();

        /**
         * Overrides the way fields are serialized for instances of {@code T} and returns this
         * for chaining.
         */
        public <T> Config addFieldConfig (Class<T> clazz, FieldConfig<T> config)
        {
            classes.put(clazz, config);
            return this;
        }

        /**
         * Overrides the way instances of a given type are serialized and returns this for chaining.
         */
        public <T> Config addClassConverter (Class<T> clazz, Converter<T> converter)
        {
            converters.put(clazz, converter);
            return this;
        }

        /**
         * Looks up the field access for a class. This also makes a fairly lame attempt to
         * ensure the most derived entry is used.
         * TODO: spruce up class sort
         */
        public FieldConfig<?> findConfig (Class<?> clazz)
        {
            Class<?> found = null;
            for (Class<?> entry : classes.keySet()) {
                if (entry.isAssignableFrom(clazz)) {
                    if (found == null || found.isAssignableFrom(entry)) {
                        found = entry;
                    }
                }
            }
            return classes.get(found);
        }

        /**
         * Tests if a class is primitive for our purposes.
         */
        public boolean isPrimitive (Class<?> cl)
        {
            return cl.isPrimitive() ||
                    Number.class.isAssignableFrom(cl) ||
                    String.class.isAssignableFrom(cl) ||
                    Boolean.class.isAssignableFrom(cl) ||
                    (cl.isArray() && isPrimitive(cl.getComponentType()));
        }

        /**
         * Converts the given object to json.
         * @return either a {@link JSON} instance of an object suitable for insertion to
         * {@link JSONObject#element(String, Object)}, etc.
         */
        public Object convert (final Object obj)
        {
            if (obj == null) {
                return null;
            }

            vlog("Converting object", obj, "of", obj.getClass());

            @SuppressWarnings("unchecked")
            Converter<Object> converter = (Converter<Object>)converters.get(obj.getClass());
            if (converter != null) {
                vlog("..using converter:", converter);
                return converter.convert(obj, this);
            }

            if (isPrimitive(obj.getClass())) {
                vlog("..returning primitive");
                return obj;
            }

            if (obj.getClass().isArray()) {
                vlog("..aggregating array");
                JSONArray array = new JSONArray();
                Object[] arr = (Object[])obj;
                for (int ii = 0; ii < arr.length; ++ii) {
                    array.add(convert(arr[ii]));
                }
                return array;
            }

            if (List.class.isAssignableFrom(obj.getClass())) {
                vlog("..aggregating list");
                JSONArray array = new JSONArray();
                List<?> arr = (List<?>)obj;
                for (int ii = 0; ii < arr.size(); ++ii) {
                    array.add(convert(arr.get(ii)));
                }
                return array;
            }

            // TODO: add more collection types as needed

            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<JSONObject>() {
                    public JSONObject run ()
                        throws Exception
                    {
                        vlog("..aggregating fields");
                        return convertFields(obj);
                    }
                });
            } catch (PrivilegedActionException ex) {
                throw new RuntimeException("Failed to make object", ex);
            }
        }

        /**
         * Converts an object to json using it's declared fields. This can be called on an
         * object known to be of an appropriate composition, otherwise use {@link #convert(Object)}.
         */
        public JSONObject convertFields (Object obj)
        {
            try {
                FieldConfig<?> config = findConfig(obj.getClass());
                List<Field> fields = cachedFields.get(obj.getClass());
                if (fields == null) {
                    cachedFields.put(obj.getClass(), fields = Lists.newArrayList());
                    ClassUtil.getFields(obj.getClass(), fields);
                    vlog("Got fields for class", obj.getClass(), "fields", fields);
                }

                JSONObject jobj = new JSONObject();
                for (Field field : fields) {
                    if (config != null && config.excludeField(field)) {
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    Converter<Object> fieldConverter = config != null ?
                        (Converter<Object>)config.customFields.get(field.getName()) : null;
                    String fieldName = getFieldName(field);
                    if (fieldConverter != null) {
                        vlog("....converting field", field, "with name", fieldName,
                            "using custom converter", fieldConverter);
                        jobj.element(fieldName, fieldConverter.convert(obj, this));
                    } else {
                        vlog("....converting field", field, "with name", fieldName);
                        jobj.element(fieldName, convert(field.get(obj)));
                    }
                }
                return jobj;

            } catch (Exception ex) {
                throw new RuntimeException("Failed to make object", ex);
            }
        }

        /**
         * Gets the field from an object of a given name, using a doPrivileged call so that
         * protected members can be accessed.
         */
        public Object getFieldValue (final Object obj, final String fieldName)
        {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    public Object run ()
                        throws Exception
                    {
                        List<Field> fields = cachedFields.get(obj.getClass());
                        if (fields == null) {
                            cachedFields.put(obj.getClass(), fields = Lists.newArrayList());
                            ClassUtil.getFields(obj.getClass(), fields);
                        }
                        for (Field field : fields) {
                            if (field.getName().equals(fieldName)) {
                                return field.get(obj);
                            }
                        }
                        return null;
                    }
                });
            } catch (PrivilegedActionException ex) {
                throw new RuntimeException("Failed to make object", ex);
            }
        }

        /**
         * Utility method to call {@link #convert(Object)} each item in an iterable and return
         * an array of the results.
         */
        public <T> JSONArray toArray (Iterable<T> objects)
        {
            JSONArray arr = new JSONArray();
            for (Object item : objects) {
                arr.add(convert(item));
            }
            return arr;
        }

        /**
         * Gets the name of a field, stripping off the leading underscore if present.
         */
        public String getFieldName (Field field)
        {
            String name = field.getName();
            if (name.startsWith("_")) {
                name = name.substring(1);
            }
            return name;
        }

        private Map<Class<?>, List<Field>> cachedFields = Maps.newHashMap();
    }

    /**
     * Converts an object to an array using an collection member. This covers the fairly common
     * case where for example a {@code FooSet} needs to be stored as a json array of {@code Foo}
     * instances, but for practical reasons the iterable is a member.
     */
    public static class ConvertCollectionFieldToArray<T> implements Converter<T>
    {
        public final FieldAccessor<T, Collection<?>> accessor;

        public ConvertCollectionFieldToArray (FieldAccessor<T, Collection<?>> accessor)
        {
            this.accessor = accessor;
        }

        public JSON convert (T obj, Config cfg)
        {
            return cfg.toArray(accessor.get(obj));
        }
    }
}
