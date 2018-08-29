/*
 *  Copyright 2002-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.stream.binder.jms.utils;

import javax.jms.Destination;

import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.context.Lifecycle;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.jms.JmsHeaderMapper;
import org.springframework.integration.jms.JmsSendingMessageHandler;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;

/**
 * Extension of {@link JmsSendingMessageHandler}, with partition awareness.
 *
 * <p>Whenever a destination name is set, it builds up a SpEL expression
 * using the partition index of the message to route it to the appropriate
 * partition.
 *
 * @author José Carlos Valero
 * @author Donovan Muller
 * @author Tim Ysewyn
 * @since 1.1
 */
public class PartitionAwareJmsSendingMessageHandler extends AbstractMessageHandler implements Lifecycle {

	private final JmsTemplate jmsTemplate;

	private final TopicPartitionRegistrar destinations;

	private final JmsHeaderMapper headerMapper;

	public PartitionAwareJmsSendingMessageHandler(JmsTemplate jmsTemplate,
												  TopicPartitionRegistrar destinations,
												  JmsHeaderMapper headerMapper) {
		this.jmsTemplate = jmsTemplate;
		this.destinations = destinations;
		this.headerMapper = headerMapper;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		if (message == null) {
			throw new IllegalArgumentException("message must not be null");
		}
		Destination destination = this.determineDestination(message);
		this.jmsTemplate.convertAndSend(destination, message.getPayload(), (jmsMessage) -> {
			this.headerMapper.fromHeaders(message.getHeaders(), jmsMessage);
			return jmsMessage;
		});
	}

	private Destination determineDestination(Message<?> message) {
		return destinations.getDestination(message.getHeaders().get(BinderHeaders.PARTITION_HEADER));
	}

	/*
	TODO: This has to be refactored, there is an open issue https://github.com/spring-cloud/spring-cloud-stream/issues/607
	that requires some love first
	 */
	private boolean running;
	@Override
	public synchronized void start() {
		running = true;
	}

	@Override
	public synchronized void stop() {
		running = false;
	}

	@Override
	public synchronized boolean isRunning() {
		return running;
	}
}
