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
package com.digitalpebble.stormcrawler.urlfrontier;

import static com.digitalpebble.stormcrawler.urlfrontier.Constants.URLFRONTIER_DEFAULT_PORT;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ManagedChannelToolkit {
    private ManagedChannelToolkit() {}

    private static final Logger LOG = LoggerFactory.getLogger(ManagedChannelToolkit.class);

    /** Gets a channel for the given host and post. */
    @NotNull
    static ManagedChannel createChannel(
            @NotNull String host, @Range(from = 0, to = 65535) int port) {
        return createChannel(host + ":" + port);
    }

    @NotNull
    static ManagedChannel createChannel(@NotNull String address) {
        // add the default port if missing
        if (!address.contains(":")) {
            address += ":" + URLFRONTIER_DEFAULT_PORT;
        }
        LOG.info("Initialisation of connection to URLFrontier service on {}", address);
        return ManagedChannelBuilder.forTarget(address).usePlaintext().build();
    }
}
