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

package com.digitalpebble.stormcrawler.parse.filter;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.storm.shade.org.apache.commons.lang.StringUtils;
import org.w3c.dom.DocumentFragment;

import com.digitalpebble.stormcrawler.Metadata;
import com.digitalpebble.stormcrawler.parse.ParseData;
import com.digitalpebble.stormcrawler.parse.ParseFilter;
import com.digitalpebble.stormcrawler.parse.ParseResult;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Computes a signature for a page, based on the binary content or text. If the
 * content is empty, the URL is used.
 */
public class MD5SignatureParseFilter extends ParseFilter {

	private String key_name = "signature";

	private boolean useText = false;

	@Override
	public void filter(String URL, byte[] content, DocumentFragment doc, ParseResult parse) {
		ParseData parseData = parse.get(URL);
		Metadata metadata = parseData.getMetadata();
		byte[] data = null;
		if (useText) {
			String text = parseData.getText();
			if (StringUtils.isNotBlank(text)) {
				data = text.getBytes(StandardCharsets.UTF_8);
			}
		} else {
			data = content;
		}
		if (data == null) {
			data = URL.getBytes(StandardCharsets.UTF_8);
		}
		String hex = DigestUtils.md5Hex(data);
		metadata.setValue(key_name, hex);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void configure(Map stormConf, JsonNode filterParams) {
		JsonNode node = filterParams.get("useText");
		if (node != null && node.asBoolean()) {
			useText = true;
		}
		node = filterParams.get("keyName");
		if (node != null && node.isTextual()) {
			key_name = node.asText("signature");
		}
	}

}
