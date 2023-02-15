/*
 *  Copyright 2002-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *		https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.stream.binder.jms.utils;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.jms.JmsHeaderMapper;
import org.springframework.jms.core.JmsTemplate;

/**
 * Factory to create JMS message handlers
 *
 * @author José Carlos Valero
 * @author Donovan Muller
 * @author Gary Russell
 * @since 1.1
 */
public class JmsSendingMessageHandlerFactory implements ApplicationContextAware, BeanFactoryAware {

	private final JmsTemplate template;

	private ApplicationContext applicationContext;

	private BeanFactory beanFactory;

	private final JmsHeaderMapper headerMapper;

	public JmsSendingMessageHandlerFactory(JmsTemplate template,
										   JmsHeaderMapper headerMapper) {
		this.template = template;
		this.headerMapper = headerMapper;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public PartitionAwareJmsSendingMessageHandler build(TopicPartitionRegistrar destinations) {
		template.setPubSubDomain(true);
		PartitionAwareJmsSendingMessageHandler handler = new PartitionAwareJmsSendingMessageHandler(
				this.template,
				destinations,
				headerMapper);
		handler.setApplicationContext(this.applicationContext);
		handler.setBeanFactory(this.beanFactory);
		handler.afterPropertiesSet();
		return handler;
	}

}
