/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stormcrawler.parse.filter;

import java.io.IOException;
import java.util.List;
import org.apache.stormcrawler.Metadata;
import org.apache.stormcrawler.bolt.JSoupParserBolt;
import org.apache.stormcrawler.parse.ParsingTester;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class XPathFilterTest extends ParsingTester {

    @Before
    public void setupParserBolt() {
        bolt = new JSoupParserBolt();
        setupParserBolt(bolt);
    }

    @Test
    public void testBasicExtraction() throws IOException {

        prepareParserBolt("test.parsefilters.json");

        parse("http://www.digitalpebble.com", "digitalpebble.com.html");

        Assert.assertEquals(1, output.getEmitted().size());
        List<Object> parsedTuple = output.getEmitted().get(0);
        Metadata metadata = (Metadata) parsedTuple.get(2);
        Assert.assertNotNull(metadata);
        String concept = metadata.getFirstValue("concept");
        Assert.assertNotNull(concept);

        concept = metadata.getFirstValue("concept2");
        Assert.assertNotNull(concept);
    }

    @Test
    // https://github.com/DigitalPebble/storm-crawler/issues/219
    public void testScriptExtraction() throws IOException {

        prepareParserBolt("test.parsefilters.json");

        parse("http://www.digitalpebble.com", "digitalpebble.com.html");

        Assert.assertEquals(1, output.getEmitted().size());
        List<Object> parsedTuple = output.getEmitted().get(0);
        Metadata metadata = (Metadata) parsedTuple.get(2);
        Assert.assertNotNull(metadata);
        String[] scripts = metadata.getValues("js");
        Assert.assertNotNull(scripts);
        // should be 2 of them
        Assert.assertEquals(2, scripts.length);
        Assert.assertEquals("", scripts[0].trim());
        Assert.assertTrue(scripts[1].contains("urchinTracker();"));
    }

    @Test
    public void testLDJsonExtraction() throws IOException {

        prepareParserBolt("test.parsefilters.json");

        parse("http://www.digitalpebble.com", "digitalpebble.com.html");

        Assert.assertEquals(1, output.getEmitted().size());
        List<Object> parsedTuple = output.getEmitted().get(0);
        Metadata metadata = (Metadata) parsedTuple.get(2);
        Assert.assertNotNull(metadata);
        String[] scripts = metadata.getValues("streetAddress");
        Assert.assertNotNull(scripts);
    }
}
