/*
 *  Copyright 2016-2017 the original author or authors.
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

package org.springframework.cloud.stream.binder.jms.activemq;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.cloud.stream.binder.AbstractBinderTests;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.Spy;
import org.springframework.cloud.stream.binder.jms.JMSMessageChannelBinder;
import org.springframework.cloud.stream.binder.jms.config.JmsConsumerProperties;
import org.springframework.cloud.stream.binder.jms.config.JmsProducerProperties;
import org.springframework.cloud.stream.binder.jms.test.ActiveMQTestUtils;
import org.springframework.cloud.stream.binder.jms.utils.Base64UrlNamingStrategy;
import org.springframework.cloud.stream.binder.jms.utils.DestinationNameResolver;
import org.springframework.cloud.stream.binder.jms.utils.JmsMessageDrivenEndpointFactory;
import org.springframework.cloud.stream.binder.jms.utils.JmsSendingMessageHandlerFactory;
import org.springframework.cloud.stream.binder.jms.utils.ListenerContainerFactory;
import org.springframework.cloud.stream.binder.jms.utils.MessageRecoverer;
import org.springframework.cloud.stream.binder.jms.utils.RepublishMessageRecoverer;
import org.springframework.cloud.stream.binder.jms.utils.SpecCompliantJmsHeaderMapper;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;

/**
 * @author Ilayaperumal Gopinathan
 * @author Gary Russell
 * @author Tim Ysewyn
 */
public class ActiveMQBinderTests extends AbstractBinderTests<ActiveMQTestBinder, ExtendedConsumerProperties<JmsConsumerProperties>,
		ExtendedProducerProperties<JmsProducerProperties>> {

	private static ActiveMQTestUtils activeMQTestUtils;

	@BeforeClass
	public static void setup() {
		activeMQTestUtils = new ActiveMQTestUtils();
	}

	@AfterClass
	public static void teardown() throws Exception {
		activeMQTestUtils.stopEmbeddedActiveMQServer();
	}

	@Override
	protected ActiveMQTestBinder getBinder() throws Exception {
		ConnectionFactory connectionFactory = activeMQTestUtils.getConnectionFactory();
		ActiveMQQueueProvisioner queueProvisioner = new ActiveMQQueueProvisioner(connectionFactory,
				new DestinationNameResolver(new Base64UrlNamingStrategy("anonymous.")));
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.refresh();
		JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
		JmsSendingMessageHandlerFactory jmsSendingMessageHandlerFactory = new JmsSendingMessageHandlerFactory(
				jmsTemplate, new SpecCompliantJmsHeaderMapper());
		jmsSendingMessageHandlerFactory.setApplicationContext(applicationContext);
		jmsSendingMessageHandlerFactory.setBeanFactory(applicationContext.getBeanFactory());
		ListenerContainerFactory listenerContainerFactory = new ListenerContainerFactory(connectionFactory);
		MessageRecoverer messageRecoverer = new RepublishMessageRecoverer(jmsTemplate,
				new SpecCompliantJmsHeaderMapper());
		JmsMessageDrivenEndpointFactory jmsMessageDrivenEndpointFactory = new JmsMessageDrivenEndpointFactory(
				listenerContainerFactory, messageRecoverer, new SpecCompliantJmsHeaderMapper());
		jmsMessageDrivenEndpointFactory.setApplicationContext(applicationContext);
		jmsMessageDrivenEndpointFactory.setBeanFactory(applicationContext.getBeanFactory());
		JMSMessageChannelBinder binder = new JMSMessageChannelBinder(queueProvisioner,
				jmsSendingMessageHandlerFactory,
				jmsMessageDrivenEndpointFactory, jmsTemplate, connectionFactory);
		binder.setApplicationContext(applicationContext);
		ActiveMQTestBinder testBinder = new ActiveMQTestBinder();
		testBinder.setBinder(binder);
		return testBinder;
	}

	@Override
	protected ExtendedConsumerProperties createConsumerProperties() {
		return new ExtendedConsumerProperties(new JmsConsumerProperties());
	}

	@Override
	protected ExtendedProducerProperties createProducerProperties() {
		return new ExtendedProducerProperties(new JmsProducerProperties());
	}

	@Override
	public Spy spyOn(String name) {
		throw new UnsupportedOperationException("'spyOn' is not used by JMS tests");
	}
}
