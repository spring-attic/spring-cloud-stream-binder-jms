package org.springframework.cloud.stream.binder.jms.ibmmq;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.jms.DefaultJmsHeaderMapper;
import org.springframework.messaging.MessageHeaders;

/**
 * Replace all header names that contain '-' with '_' die to IBM MQ header name
 * constraints.
 *
 * See http://stackoverflow.com/a/30024766/2408961
 */
public class IBMMQJmsHeaderMapper extends DefaultJmsHeaderMapper {

	private static final Logger logger = LoggerFactory
			.getLogger(IBMMQJmsHeaderMapper.class);

	@Override
	public void fromHeaders(MessageHeaders headers, Message jmsMessage) {
		Map<String, Object> parsedHeaders = new HashMap<>(headers.size());
		for (Map.Entry<String, Object> entry : headers.entrySet()) {
			if (entry.getKey().contains("-")) {
				String key = entry.getKey().replaceAll("-", "_");
				logger.trace("Changing header '{}' to conform to JMS spec", key);
				parsedHeaders.put(key, entry.getValue());
			}
			else {
				parsedHeaders.put(entry.getKey(), entry.getValue());
			}
		}

		super.fromHeaders(new MessageHeaders(parsedHeaders), jmsMessage);
	}
}
