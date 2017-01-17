/*
 *  Copyright 2002-2016 the original author or authors.
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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.jms.JmsHeaderMapper;
import org.springframework.jms.core.JmsTemplate;

/**
 * Factory to create JMS message handlers
 *
 * @author José Carlos Valero
 * @author Donovan Muller
 * @since 1.1
 */
public class JmsSendingMessageHandlerFactory {

	private final JmsTemplate template;
	private final BeanFactory beanFactory;
	private JmsHeaderMapper headerMapper;

	public JmsSendingMessageHandlerFactory(JmsTemplate template,
										   BeanFactory beanFactory,
										   JmsHeaderMapper headerMapper) {
		this.template = template;
		this.beanFactory = beanFactory;
		this.headerMapper = headerMapper;
	}

	public PartitionAwareJmsSendingMessageHandler build(TopicPartitionRegistrar destinations) {
		template.setPubSubDomain(true);
		PartitionAwareJmsSendingMessageHandler handler = new PartitionAwareJmsSendingMessageHandler(
				this.template,
				destinations,
				headerMapper);
		handler.setBeanFactory(beanFactory);
		handler.afterPropertiesSet();

		return handler;
	}
}
