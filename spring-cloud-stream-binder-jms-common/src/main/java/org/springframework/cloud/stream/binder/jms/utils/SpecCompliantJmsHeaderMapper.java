/*
 *  Copyright 2002-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.stream.binder.jms.utils;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.jms.DefaultJmsHeaderMapper;
import org.springframework.messaging.MessageHeaders;

/**
 * Replace all header names that contain '-' with '_' due to JMS spec header name
 * constraints.
 *
 * See https://stackoverflow.com/a/30024766/2408961 for context.
 *
 * @author Donovan Muller
 */
public class SpecCompliantJmsHeaderMapper extends DefaultJmsHeaderMapper {

	private static final Logger logger = LoggerFactory
			.getLogger(SpecCompliantJmsHeaderMapper.class);

	@Override
	public void fromHeaders(MessageHeaders headers, Message jmsMessage) {
		Map<String, Object> compliantHeaders = new HashMap<>(headers.size());
		for (Map.Entry<String, Object> entry : headers.entrySet()) {
			if (entry.getKey().contains("-")) {
				String key = entry.getKey().replaceAll("-", "_");
				logger.trace("Rewriting header name '{}' to conform to JMS spec", key);
				compliantHeaders.put(key, entry.getValue());
			}
			else {
				compliantHeaders.put(entry.getKey(), entry.getValue());
			}
		}

		super.fromHeaders(new MessageHeaders(compliantHeaders), jmsMessage);
	}
}
