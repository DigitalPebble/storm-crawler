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

import backtype.storm.metric.LoggingMetricsConsumer;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;
import com.digitalpebble.storm.crawler.bolt.FetcherBolt;
import com.digitalpebble.storm.crawler.bolt.JSoupParserBolt;
import com.digitalpebble.storm.crawler.bolt.SiteMapParserBolt;
import com.digitalpebble.storm.crawler.bolt.URLPartitionerBolt;
import com.digitalpebble.storm.crawler.indexing.StdOutIndexer;
import com.digitalpebble.storm.crawler.persistence.RecursiveCrawlingUpdater;
import com.digitalpebble.storm.crawler.persistence.StdOutStatusUpdater;
import com.digitalpebble.storm.crawler.spout.RandomURLSpout;

/**
 * Dummy recursive topology to play with the spouts and bolts
 */
public class RecursiveCrawlTopology extends ConfigurableTopology {

    public static void main(String[] args) throws Exception {
        ConfigurableTopology.start(new RecursiveCrawlTopology(), args);
    }

    @Override
    protected int run(String[] args) {
        TopologyBuilder builder = new TopologyBuilder();

        builder.setSpout("spout", new RandomURLSpout());

        builder.setBolt("partitioner", new URLPartitionerBolt())
                .shuffleGrouping("spout");

        builder.setBolt("fetch", new FetcherBolt()).fieldsGrouping(
                "partitioner", new Fields("key"));

        builder.setBolt("sitemap", new SiteMapParserBolt())
                .localOrShuffleGrouping("fetch");

        builder.setBolt("parse", new JSoupParserBolt()).localOrShuffleGrouping(
                "sitemap");

        builder.setBolt("index", new StdOutIndexer()).localOrShuffleGrouping(
                "parse");

        builder.setBolt("status", new RecursiveCrawlingUpdater())
                .localOrShuffleGrouping("fetch", Constants.StatusStreamName)
                .localOrShuffleGrouping("sitemap", Constants.StatusStreamName)
                .localOrShuffleGrouping("parse", Constants.StatusStreamName)
                .localOrShuffleGrouping("index", Constants.StatusStreamName);

        conf.registerMetricsConsumer(LoggingMetricsConsumer.class);

        return submit("crawl", conf, builder);
    }
}
