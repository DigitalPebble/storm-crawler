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
package org.apache.stormcrawler.solr.persistence;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Tuple;
import org.apache.stormcrawler.Metadata;
import org.apache.stormcrawler.TestOutputCollector;
import org.apache.stormcrawler.TestUtil;
import org.apache.stormcrawler.persistence.Status;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class StatusBoltTest {
    @Rule public Timeout globalTimeout = Timeout.seconds(120);

    private StatusUpdaterBolt bolt;
    protected TestOutputCollector output;

    private static final Logger LOG = LoggerFactory.getLogger(StatusBoltTest.class);
    private static ExecutorService executorService;

    private final DockerImageName image = DockerImageName.parse("solr:9.1");

    @Rule
    public GenericContainer<?> container =
            new GenericContainer<>(image)
                    .withExposedPorts(8983)
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource("/cores/"),
                            "/opt/solr/server/solr/cores")
                    .waitingFor(Wait.forHttp("/solr/admin/cores?action=STATUS").forStatusCode(200));

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
    public void setupStatusBolt() throws IOException, InterruptedException {
        container.start();
        container.execInContainer(
                "/opt/solr/bin/solr",
                "create",
                "-c",
                "status",
                "-d",
                "/opt/solr/server/solr/cores/status");

        bolt = new StatusUpdaterBolt();
        output = new TestOutputCollector();

        Map<String, Object> conf = new HashMap<>();

        conf.put("scheduler.class", "org.apache.stormcrawler.persistence.DefaultScheduler");
        conf.put("status.updater.cache.spec", "maximumSize=10000,expireAfterAccess=1h");

        final String SOLRURL =
                "http://"
                        + container.getHost()
                        + ":"
                        + container.getMappedPort(8983)
                        + "/solr/status";

        conf.put("solr.status.url", SOLRURL);

        bolt.prepare(conf, TestUtil.getMockedTopologyContext(), new OutputCollector(output));
    }

    @After
    public void close() {
        LOG.info("Closing updater bolt and SOLR container");
        bolt.cleanup();
        container.close();
        output = null;
    }

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
    public void basicTest()
            throws IOException, ExecutionException, InterruptedException, TimeoutException {

        String url = "https://www.url.net/something";

        Metadata md = new Metadata();

        md.addValue("someKey", "someValue");

        store(url, Status.DISCOVERED, md).get(10, TimeUnit.SECONDS);

        assertEquals(1, output.getAckedTuples().size());
    }

    @Test
    public void errorTest() throws Exception {

        String url = "https://www.url.net/something";

        Metadata md = new Metadata();

        md.addValue("someKey", "someValue");

        store(url, Status.ERROR, md).get(10, TimeUnit.SECONDS);

        assertEquals(1, output.getAckedTuples().size());
    }
}
