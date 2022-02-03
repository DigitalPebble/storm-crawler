/**
 * Licensed to DigitalPebble Ltd under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership.
 * DigitalPebble licenses this file to You under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.digitalpebble.stormcrawler.persistence.urlbuffer;

import com.digitalpebble.stormcrawler.Metadata;
import com.digitalpebble.stormcrawler.persistence.EmptyQueueListener;
import java.util.Map;
import org.apache.storm.tuple.Values;

/**
 * Buffers URLs to be processed into separate queues; used by spouts. Guarantees that no URL can be
 * put in the buffer more than once.
 *
 * <p>Configured by setting
 *
 * <p>urlbuffer.class: "com.digitalpebble.stormcrawler.persistence.SimpleURLBuffer"
 *
 * <p>in the configuration
 *
 * @since 1.15
 */
public interface URLBuffer {

    /** Replace with URLBufferUtil.bufferClassParamName */
    @Deprecated
    public static final String bufferClassParamName = URLBufferUtil.bufferClassParamName;

    /** Replace with URLBufferUtil.createInstance */
    @Deprecated
    public static URLBuffer getInstance(Map<String, Object> stormConf) {
        return URLBufferUtil.createInstance(stormConf);
    }
    /**
     * Stores the URL and its Metadata under a given key.
     *
     * <p>Implementations of this method should be synchronised
     *
     * @return false if the URL was already in the buffer, true if it wasn't and was added
     */
    boolean add(String URL, Metadata m, String key);

    /**
     * Stores the URL and its Metadata using the hostname as key.
     *
     * <p>Implementations of this method should be synchronised
     *
     * @return false if the URL was already in the buffer, true if it wasn't and was added
     */
    default boolean add(String URL, Metadata m) {
        return add(URL, m, null);
    }

    /** Total number of URLs in the buffer * */
    int size();

    /** Total number of queues in the buffer * */
    int numQueues();

    /**
     * Retrieves the next available URL, guarantees that the URLs are always perfectly shuffled
     *
     * <p>Implementations of this method should be synchronised
     */
    Values next();

    /** Implementations of this method should be synchronised */
    boolean hasNext();

    void setEmptyQueueListener(EmptyQueueListener l);

    /**
     * Notify the buffer that a URL has been successfully processed used e.g to compute an ideal
     * delay for a host queue
     */
    default void acked(String url) {
        // do nothing with the information about URLs being acked
    }

    default void configure(Map<String, Object> stormConf) {}
}
