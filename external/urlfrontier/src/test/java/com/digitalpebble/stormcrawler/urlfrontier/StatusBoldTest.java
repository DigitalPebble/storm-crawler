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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitalpebble.stormcrawler.Metadata;
import com.digitalpebble.stormcrawler.TestOutputCollector;
import com.digitalpebble.stormcrawler.TestUtil;
import com.digitalpebble.stormcrawler.persistence.Status;
import io.grpc.ManagedChannel;
import java.util.HashMap;
import java.util.concurrent.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Tuple;
import org.junit.*;
import org.junit.rules.Timeout;

public class StatusBoldTest {
    private StatusUpdaterBolt bolt;
    private TestOutputCollector output;
    private URLFrontierContainer urlFrontierContainer;

    private ManagedChannel managedChannel;

    private static final String persistedKey = "somePersistedKey";

    private static ExecutorService executorService;

    @Rule public Timeout globalTimeout = Timeout.seconds(60);

    @BeforeClass
    public static void beforeClass() {
        executorService = Executors.newFixedThreadPool(2);
    }

    @AfterClass
    public static void afterClass() {
        executorService.shutdown();
        executorService = null;
    }

    @Before
    public void before() {

        urlFrontierContainer = new URLFrontierContainer("crawlercommons/url-frontier:2.1");
        urlFrontierContainer.start();

        bolt = new StatusUpdaterBolt();

        var connection = urlFrontierContainer.getFrontierConnection();
        managedChannel = ChannelManager.getChannel(connection.getAddress());

        final var config = new HashMap<String, Object>();
        config.put(
                "urlbuffer.class",
                "com.digitalpebble.stormcrawler.persistence.urlbuffer.SimpleURLBuffer");
        config.put(Constants.URLFRONTIER_HOST_KEY, connection.getHost());
        config.put(Constants.URLFRONTIER_PORT_KEY, connection.getPort());
        config.put(
                "scheduler.class", "com.digitalpebble.stormcrawler.persistence.DefaultScheduler");
        config.put("status.updater.cache.spec", "maximumSize=10000,expireAfterAccess=1h");
        config.put("metadata.persist", persistedKey);

        output = new TestOutputCollector();
        bolt.prepare(config, TestUtil.getMockedTopologyContext(), new OutputCollector(output));
    }

    @After
    public void after() {
        bolt.cleanup();
        urlFrontierContainer.close();
        ChannelManager.returnChannel(managedChannel);
        output = null;
    }

    @SuppressWarnings("SameParameterValue")
    private Future<Integer> store(String url, Status status, Metadata metadata) {
        Tuple tuple = mock(Tuple.class);
        when(tuple.getValueByField("status")).thenReturn(status);
        when(tuple.getStringByField("url")).thenReturn(url);
        when(tuple.getValueByField("metadata")).thenReturn(metadata);
        bolt.execute(tuple);

        return executorService.submit(
                () -> {
                    var outputSize = output.getAckedTuples().size();
                    while (outputSize == 0) {
                        Thread.sleep(100);
                        outputSize = output.getAckedTuples().size();
                    }
                    return outputSize;
                });
    }

    @Test
    public void canAckASimpleTuple()
            throws ExecutionException, InterruptedException, TimeoutException {

        Configurator.setLevel(StatusUpdaterBolt.class, Level.ALL);

        final var url = "https://www.url.net/something";
        final var meta = new Metadata();
        meta.setValue(persistedKey, "somePersistedMetaInfo");
        meta.setValue("someNotPersistedKey", "someNotPersistedMetaInfo");

        var future = store(url, Status.DISCOVERED, meta);

        int numberOfAckedTuples = future.get(5, TimeUnit.SECONDS);

        Assert.assertEquals(1, numberOfAckedTuples);
    }
}
