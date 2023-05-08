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
package com.digitalpebble.stormcrawler.filtering;

import static org.junit.Assert.assertEquals;

import com.digitalpebble.stormcrawler.Metadata;
import com.digitalpebble.stormcrawler.filtering.basic.BasicURLNormalizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * Utility class which encapsulates the filtering of URLs based on the hostname or domain of the
 * source URL.
 */
public class BasicURLNormalizerTest {

    List<String> queryParamsToFilter = Arrays.asList("a", "foo");

    private URLFilter createFilter(boolean removeAnchor, boolean checkValidURI) {
        ObjectNode filterParams = new ObjectNode(JsonNodeFactory.instance);
        filterParams.put("removeAnchorPart", Boolean.valueOf(removeAnchor));
        filterParams.put("checkValidURI", Boolean.valueOf(checkValidURI));
        return createFilter(filterParams);
    }

    private URLFilter createFilter(List<String> queryElementsToRemove) {
        ObjectNode filterParams = new ObjectNode(JsonNodeFactory.instance);
        filterParams.set("queryElementsToRemove", getArrayNode(queryElementsToRemove));
        return createFilter(filterParams);
    }

    private URLFilter createFilter(boolean removeAnchor, List<String> queryElementsToRemove) {
        ObjectNode filterParams = new ObjectNode(JsonNodeFactory.instance);
        filterParams.set("queryElementsToRemove", getArrayNode(queryElementsToRemove));
        filterParams.put("removeAnchorPart", Boolean.valueOf(removeAnchor));
        return createFilter(filterParams);
    }

    private URLFilter createFilter(
            boolean removeAnchor, boolean unmangleQueryString, List<String> queryElementsToRemove) {
        ObjectNode filterParams = new ObjectNode(JsonNodeFactory.instance);
        filterParams.set("queryElementsToRemove", getArrayNode(queryElementsToRemove));
        filterParams.put("removeAnchorPart", Boolean.valueOf(removeAnchor));
        filterParams.put("unmangleQueryString", Boolean.valueOf(unmangleQueryString));
        return createFilter(filterParams);
    }

    private URLFilter createFilter(ObjectNode filterParams) {
        BasicURLNormalizer filter = new BasicURLNormalizer();
        Map<String, Object> conf = new HashMap<>();
        filter.configure(conf, filterParams);
        return filter;
    }

    @Test
    public void testAnchorFilter() throws MalformedURLException {
        URLFilter allAllowed = createFilter(true, false);
        URL url = new URL("http://www.sourcedomain.com/#0");
        Metadata metadata = new Metadata();
        String filterResult = allAllowed.filter(url, metadata, url.toExternalForm());
        String expected = "http://www.sourcedomain.com/";
        Assert.assertEquals(expected, filterResult);
    }

    @Test
    public void testAnchorFilterFalse() throws MalformedURLException {
        URLFilter allAllowed = createFilter(false, false);
        URL url = new URL("http://www.sourcedomain.com/#0");
        Metadata metadata = new Metadata();
        String filterResult = allAllowed.filter(url, metadata, url.toExternalForm());
        Assert.assertEquals(url.toExternalForm(), filterResult);
    }

    @Test
    public void testRemoveSomeOfManyQueryParams() throws MalformedURLException {
        URLFilter urlFilter = createFilter(queryParamsToFilter);
        URL testSourceUrl = new URL("http://google.com");
        String testUrl = "http://google.com?keep1=true&a=c&foo=baz&keep2=true";
        String expectedResult = "http://google.com?keep1=true&keep2=true";
        String normalizedUrl = urlFilter.filter(testSourceUrl, new Metadata(), testUrl);
        assertEquals("Failed to filter query string", expectedResult, normalizedUrl);
    }

    @Test
    public void testRemoveAllQueryParams() throws MalformedURLException {
        URLFilter urlFilter = createFilter(queryParamsToFilter);
        URL testSourceUrl = new URL("http://google.com");
        String testUrl = "http://google.com?a=c&foo=baz";
        String expectedResult = "http://google.com";
        String normalizedUrl = urlFilter.filter(testSourceUrl, new Metadata(), testUrl);
        assertEquals("Failed to filter query string", expectedResult, normalizedUrl);
    }

    @Test
    public void testRemoveDupeQueryParams() throws MalformedURLException {
        URLFilter urlFilter = createFilter(queryParamsToFilter);
        URL testSourceUrl = new URL("http://google.com");
        String testUrl = "http://google.com?a=c&foo=baz&foo=bar&test=true";
        String expectedResult = "http://google.com?test=true";
        String normalizedUrl = urlFilter.filter(testSourceUrl, new Metadata(), testUrl);
        assertEquals("Failed to filter query string", expectedResult, normalizedUrl);
    }

    @Test
    public void testPipeInUrlAndFilterStillWorks() throws MalformedURLException {
        URLFilter urlFilter = createFilter(queryParamsToFilter);
        URL testSourceUrl = new URL("http://google.com");
        String testUrl = "http://google.com?a=c|d&foo=baz&foo=bar&test=true";
        String expectedResult = "http://google.com?test=true";
        String normalizedUrl = urlFilter.filter(testSourceUrl, new Metadata(), testUrl);
        assertEquals("Failed to filter query string", expectedResult, normalizedUrl);
    }

    @Test
    public void testBothAnchorAndQueryFilter() throws MalformedURLException {
        URLFilter urlFilter = createFilter(true, queryParamsToFilter);
        URL testSourceUrl = new URL("http://google.com");
        String testUrl = "http://google.com?a=c|d&foo=baz&foo=bar&test=true#fragment=ohYeah";
        String expectedResult = "http://google.com?test=true";
        String normalizedUrl = urlFilter.filter(testSourceUrl, new Metadata(), testUrl);
        assertEquals("Failed to filter query string", expectedResult, normalizedUrl);
    }

    @Test
    public void testQuerySort() throws MalformedURLException {
        URLFilter urlFilter = createFilter(queryParamsToFilter);
        URL testSourceUrl = new URL("http://google.com");
        String testUrl = "http://google.com?a=c|d&foo=baz&foo=bar&test=true&z=2&d=4";
        String expectedResult = "http://google.com?d=4&test=true&z=2";
        String normalizedUrl = urlFilter.filter(testSourceUrl, new Metadata(), testUrl);
        assertEquals("Failed to filter query string", expectedResult, normalizedUrl);
    }

    @Test
    public void testMangledQueryString() throws MalformedURLException {
        URLFilter urlFilter = createFilter(queryParamsToFilter);
        URL testSourceUrl = new URL("http://google.com");
        String testUrl = "http://google.com&d=4&good=true";
        String expectedResult = "http://google.com?d=4&good=true";
        String normalizedUrl = urlFilter.filter(testSourceUrl, new Metadata(), testUrl);
        assertEquals("Failed to filter query string", expectedResult, normalizedUrl);
    }

    @Test
    public void testHashes() throws MalformedURLException {
        ObjectNode filterParams = new ObjectNode(JsonNodeFactory.instance);
        filterParams.put("removeHashes", true);
        URLFilter urlFilter = createFilter(filterParams);

        URL testSourceUrl = new URL("http://florida-chemical.com");
        String in =
                "http://www.florida-chemical.com/Diacetone-Alcohol-DAA-99.html?xid_0b629=12854b827878df26423d933a5baf86d5";
        String out = "http://www.florida-chemical.com/Diacetone-Alcohol-DAA-99.html";

        String normalizedUrl = urlFilter.filter(testSourceUrl, new Metadata(), in);
        assertEquals("Failed to filter query string", out, normalizedUrl);

        in =
                "http://www.maroongroupllc.com/maroon/login/auth;jsessionid=8DBFC2FEDBD740BBC8B4D1A504A6DE7F";
        out = "http://www.maroongroupllc.com/maroon/login/auth";
        normalizedUrl = urlFilter.filter(testSourceUrl, new Metadata(), in);
        assertEquals("Failed to filter query string", out, normalizedUrl);
    }

    @Test
    public void testDontFixMangledQueryString() throws MalformedURLException {
        URLFilter urlFilter = createFilter(true, false, queryParamsToFilter);
        URL testSourceUrl = new URL("http://google.com");
        String testUrl = "http://google.com&d=4&good=true";
        String expectedResult = "http://google.com&d=4&good=true";
        String normalizedUrl = urlFilter.filter(testSourceUrl, new Metadata(), testUrl);
        assertEquals("Failed to filter query string", expectedResult, normalizedUrl);
    }

    @Test
    public void testFixMangledQueryString() throws MalformedURLException {
        URLFilter urlFilter = createFilter(false, true, queryParamsToFilter);
        URL testSourceUrl = new URL("http://google.com");
        String testUrl = "http://google.com&d=4&good=true";
        String expectedResult = "http://google.com?d=4&good=true";
        String normalizedUrl = urlFilter.filter(testSourceUrl, new Metadata(), testUrl);
        assertEquals("Failed to filter query string", expectedResult, normalizedUrl);

        testSourceUrl = new URL("http://dev.com");
        testUrl = "http://dev.com/s&utax/NEWSRLSEfy18.pdf";
        normalizedUrl = urlFilter.filter(testSourceUrl, new Metadata(), testUrl);
        expectedResult = "http://dev.com/s&utax/NEWSRLSEfy18.pdf";
        assertEquals("Failed to filter query string", expectedResult, normalizedUrl);
    }

    @Test
    public void testProperURLEncodingWithoutQueryParameter() throws MalformedURLException {
        URLFilter urlFilter = createFilter(queryParamsToFilter);
        String urlWithEscapedCharacters =
                "http://www.dillards.com/product/ASICS-Womens-GT2000-3-LiteShow%E2%84%A2-Running-Shoes_301_-1_301_504736989";
        URL testSourceUrl = new URL(urlWithEscapedCharacters);
        String testUrl = urlWithEscapedCharacters;
        String expectedResult = urlWithEscapedCharacters;
        String normalizedUrl = urlFilter.filter(testSourceUrl, new Metadata(), testUrl);
        assertEquals("Failed to filter query string", expectedResult, normalizedUrl);
    }

    @Test
    public void testProperURLEncodingWithQueryParameters() throws MalformedURLException {
        URLFilter urlFilter = createFilter(queryParamsToFilter);
        String urlWithEscapedCharacters =
                "http://www.dillards.com/product/ASICS-Womens-GT2000-3-LiteShow%E2%84%A2-Running-Shoes_301_-1_301_504736989?how=are&you=doing";
        URL testSourceUrl = new URL(urlWithEscapedCharacters);
        String testUrl = urlWithEscapedCharacters;
        String expectedResult = urlWithEscapedCharacters;
        String normalizedUrl = urlFilter.filter(testSourceUrl, new Metadata(), testUrl);
        assertEquals("Failed to filter query string", expectedResult, normalizedUrl);
    }

    @Test
    public void testProperURLEncodingWithBackSlash() throws MalformedURLException {
        URLFilter urlFilter = createFilter(queryParamsToFilter);
        String urlWithEscapedCharacters =
                "http://www.voltaix.com/\\SDS\\Silicon\\Trisilane\\Trisilane_SI050_USENG.pdf";
        String expectedResult =
                "http://www.voltaix.com/%5CSDS%5CSilicon%5CTrisilane%5CTrisilane_SI050_USENG.pdf";
        URL testSourceUrl = new URL(urlWithEscapedCharacters);
        String testUrl = urlWithEscapedCharacters;
        String normalizedUrl = urlFilter.filter(testSourceUrl, new Metadata(), testUrl);
        assertEquals("Failed to filter query string", expectedResult, normalizedUrl);
    }

    @Test
    public void testInvalidURI() throws MalformedURLException {
        URLFilter urlFilter = createFilter(true, true);
        // this one is now handled by the normaliser
        String nonURI = "http://www.quanjing.com/search.aspx?q=top-651451||1|60|1|2||||&Fr=4";
        URL testSourceUrl = new URL(nonURI);
        String expectedResult =
                "http://www.quanjing.com/search.aspx?q=top-651451%7C%7C1%7C60%7C1%7C2%7C%7C%7C%7C&Fr=4";
        String normalizedUrl = urlFilter.filter(testSourceUrl, new Metadata(), nonURI);
        assertEquals("Failed to filter query string", expectedResult, normalizedUrl);

        // this one is
        nonURI =
                "http://vins.lemonde.fr?utm_source=LeMonde_partenaire_hp&utm_medium=EMPLACEMENT PARTENAIRE&utm_term=&utm_content=&utm_campaign=LeMonde_partenaire_hp";
        testSourceUrl = new URL(nonURI);
        expectedResult =
                "http://vins.lemonde.fr?utm_source=LeMonde_partenaire_hp&utm_medium=EMPLACEMENT%20PARTENAIRE&utm_term=&utm_content=&utm_campaign=LeMonde_partenaire_hp";
        normalizedUrl = urlFilter.filter(testSourceUrl, new Metadata(), nonURI);
        assertEquals("Failed to filter query string", expectedResult, normalizedUrl);

        // check normalisation of paths
        // http://docs.oracle.com/javase/7/docs/api/java/net/URI.html#normalize()
        String nonNormURL =
                "http://docs.oracle.com/javase/7/docs/api/java/net/../net/./URI.html#normalize()";
        testSourceUrl = new URL(nonNormURL);
        expectedResult = "http://docs.oracle.com/javase/7/docs/api/java/net/URI.html";
        normalizedUrl = urlFilter.filter(testSourceUrl, new Metadata(), nonNormURL);
        assertEquals("Failed to filter query string", expectedResult, normalizedUrl);
    }

    @Test
    public void testLowerCasing() throws MalformedURLException {
        URLFilter urlFilter = createFilter(false, false);
        URL testSourceUrl = new URL("http://blablabla.org/");

        String inputURL = "HTTP://www.quanjing.com/";
        String expectedResult = inputURL.toLowerCase();
        String normalizedUrl = urlFilter.filter(testSourceUrl, new Metadata(), inputURL);

        assertEquals("Failed to filter query string", expectedResult, normalizedUrl);

        inputURL = "http://www.QUANJING.COM/";
        expectedResult = inputURL.toLowerCase();
        normalizedUrl = urlFilter.filter(testSourceUrl, new Metadata(), inputURL);

        assertEquals("Failed to filter query string", expectedResult, normalizedUrl);
    }

    // https://github.com/DigitalPebble/storm-crawler/issues/401
    @Test
    public void testNonStandardPercentEncoding() throws MalformedURLException {
        URLFilter urlFilter = createFilter(false, false);
        URL testSourceUrl = new URL("http://www.hurriyet.com.tr/index/?d=20160328&p=13");

        String inputURL = "http://www.hurriyet.com.tr/index/?d=20160328&p=13&s=ni%u011fde";
        String expectedURL = "http://www.hurriyet.com.tr/index/?d=20160328&p=13&s=ni%C4%9Fde";
        String normalizedUrl = urlFilter.filter(testSourceUrl, new Metadata(), inputURL);

        assertEquals("Failed to filter query string", expectedURL, normalizedUrl);
    }

    @Test
    public void testHostIDNtoASCII() throws MalformedURLException {
        ObjectNode filterParams = new ObjectNode(JsonNodeFactory.instance);
        filterParams.put("hostIDNtoASCII", true);
        URLFilter urlFilter = createFilter(filterParams);
        URL testSourceUrl = new URL("http://www.example.com/");

        String inputURL = "http://señal6.com.ar/";
        String expectedURL = "http://xn--seal6-pta.com.ar/";
        String normalizedUrl = urlFilter.filter(testSourceUrl, new Metadata(), inputURL);

        assertEquals("Failed to filter query string", expectedURL, normalizedUrl);

        inputURL = "http://сфера.укр/";
        expectedURL = "http://xn--80aj7acp.xn--j1amh/";
        normalizedUrl = urlFilter.filter(testSourceUrl, new Metadata(), inputURL);

        assertEquals("Failed to filter query string", expectedURL, normalizedUrl);
    }

    private JsonNode getArrayNode(List<String> queryElementsToRemove) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.valueToTree(queryElementsToRemove);
    }
}
