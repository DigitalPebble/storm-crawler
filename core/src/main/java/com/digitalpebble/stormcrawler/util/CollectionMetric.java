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
package com.digitalpebble.stormcrawler.util;

import java.util.LinkedList;
import java.util.List;
import org.apache.storm.metric.api.IMetric;

public class CollectionMetric implements IMetric {

    private final List<Long> measurements = new LinkedList<>();

    public void addMeasurement(long l) {
        synchronized (measurements) {
            measurements.add(l);
        }
    }

    @Override
    public Object getValueAndReset() {
        synchronized (measurements) {
            LinkedList<Long> copy = new LinkedList<>(measurements);
            measurements.clear();
            return copy;
        }
    }
}
