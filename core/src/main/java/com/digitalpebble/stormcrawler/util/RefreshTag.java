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

package com.digitalpebble.stormcrawler.util;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.DocumentFragment;

// Utility class used to extract refresh tags from HTML pages
public abstract class RefreshTag {

    private static XPathExpression expression;
    static {
        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            expression = xpath
                    .compile("//META[http-equiv=\"refresh\"]/@content");
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    public static String extractRefreshURL(DocumentFragment doc) {
        String value;
        try {
            value = (String) expression.evaluate(doc, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            return null;
        }
        if (StringUtils.isBlank(value))
            return null;
        // 0;URL=http://www.apollocolors.com/site
        int idx = value.toLowerCase().indexOf(";url=");
        if (idx != -1) {
            return value.substring(idx + 5);
        }
        return null;
    }
}
