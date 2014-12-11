/**
 * Licensed to DigitalPebble Ltd under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * DigitalPebble licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.digitalpebble.storm.crawler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

/** Wrapper around Map <String,String[]> **/

public class Metadata {

    // TODO customize the behaviour of Kryo via annotations
    // @BindMap(valueSerializer = IntArraySerializer.class, keySerializer =
    // StringSerializer.class, valueClass = String[].class, keyClass =
    // String.class, keysCanBeNull = false)
    private Map<String, String[]> md;

    public Metadata() {
        md = new HashMap<String, String[]>();
    }

    /**
     * Wraps an existing HashMap into a Metadata object - does not clone the
     * content
     **/
    public Metadata(Map<String, String[]> metadata) {
        md = metadata;
    }

    public Map<String, String[]> getMap() {
        return md;
    }

    public String getFirstValue(String key) {
        String[] values = md.get(key);
        if (values == null)
            return null;
        if (values.length == 0)
            return null;
        return values[0];
    }

    public String[] getValues(String key) {
        String[] values = md.get(key);
        if (values == null)
            return null;
        if (values.length == 0)
            return null;
        return values;
    }

    public void setValue(String key, String value) {
        md.put(key, new String[] { value });
    }

    public void addValue(String key, String value) {
        if (StringUtils.isBlank(value))
            return;

        String[] existingvals = md.get(key);
        if (existingvals == null || existingvals.length == 0) {
            setValue(key, value);
            return;
        }

        ArrayList<String> existing = new ArrayList<String>(existingvals.length);
        for (String v : existingvals)
            existing.add(v);

        existing.add(value);
        md.put(key, existing.toArray(new String[existing.size()]));
    }

    public void addValues(String key, Collection<String> values) {
        if (values == null || values.size() == 0)
            return;
        String[] existingvals = md.get(key);
        if (existingvals == null) {
            md.put(key, values.toArray(new String[values.size()]));
            return;
        }

        ArrayList<String> existing = new ArrayList<String>(existingvals.length);
        for (String v : existingvals)
            existing.add(v);

        existing.addAll(values);
        md.put(key, existing.toArray(new String[existing.size()]));
    }

    public String toString() {
        return toString("");
    }

    /** Returns a String representation of the metadata with one K/V per line **/
    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer();
        if (prefix == null)
            prefix = "";
        Iterator<Entry<String, String[]>> iter = md.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, String[]> entry = iter.next();
            for (String val : entry.getValue()) {
                sb.append(prefix).append(entry.getKey()).append(": ")
                        .append(val).append("\n");
            }
        }
        return sb.toString();
    }
}