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

package org.springframework.cloud.stream.binder.jms;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.springframework.cloud.stream.binder.AbstractMessageChannelBinder;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.ExtendedPropertiesBinder;
import org.springframework.cloud.stream.binder.jms.config.JmsConsumerProperties;
import org.springframework.cloud.stream.binder.jms.config.JmsExtendedBindingProperties;
import org.springframework.cloud.stream.binder.jms.config.JmsProducerProperties;
import org.springframework.cloud.stream.binder.jms.utils.JmsMessageDrivenChannelAdapterFactory;
import org.springframework.cloud.stream.binder.jms.utils.JmsSendingMessageHandlerFactory;
import org.springframework.cloud.stream.binder.jms.utils.TopicPartitionRegistrar;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * Binder definition for JMS.
 *
 * @author Jack Galilee
 * @author Jonathan Sharpe
 * @author Joseph Taylor
 * @author José Carlos Valero
 * @author Gary Russell
 * @author Soby Chacko
 * @author Ilayaperumal Gopinathan
 * @since 1.1
 */
public class JMSMessageChannelBinder
		extends AbstractMessageChannelBinder<ExtendedConsumerProperties<JmsConsumerProperties>, ExtendedProducerProperties<JmsProducerProperties>,
		ProvisioningProvider<ExtendedConsumerProperties<JmsConsumerProperties>, ExtendedProducerProperties<JmsProducerProperties>>>
		implements ExtendedPropertiesBinder<MessageChannel, JmsConsumerProperties, JmsProducerProperties> {

	private JmsExtendedBindingProperties extendedBindingProperties = new JmsExtendedBindingProperties();

	private final JmsSendingMessageHandlerFactory jmsSendingMessageHandlerFactory;
	private final JmsMessageDrivenChannelAdapterFactory jmsMessageDrivenChannelAdapterFactory;
	private final ConnectionFactory connectionFactory;

	private final DestinationResolver destinationResolver;

	public JMSMessageChannelBinder(ProvisioningProvider<ExtendedConsumerProperties<JmsConsumerProperties>, ExtendedProducerProperties<JmsProducerProperties>> provisioningProvider,
			JmsSendingMessageHandlerFactory jmsSendingMessageHandlerFactory,
			JmsMessageDrivenChannelAdapterFactory jmsMessageDrivenChannelAdapterFactory,
			JmsTemplate jmsTemplate,
			ConnectionFactory connectionFactory) {
		super(true, null, provisioningProvider);
		this.jmsSendingMessageHandlerFactory = jmsSendingMessageHandlerFactory;
		this.jmsMessageDrivenChannelAdapterFactory = jmsMessageDrivenChannelAdapterFactory;
		this.connectionFactory = connectionFactory;
		this.destinationResolver = jmsTemplate.getDestinationResolver();
	}

	public void setExtendedBindingProperties(JmsExtendedBindingProperties extendedBindingProperties) {
		this.extendedBindingProperties = extendedBindingProperties;
	}


	@Override
	protected MessageHandler createProducerMessageHandler(ProducerDestination producerDestination,
			ExtendedProducerProperties<JmsProducerProperties> producerProperties) throws Exception {
		TopicPartitionRegistrar topicPartitionRegistrar = new TopicPartitionRegistrar();
		Session session = connectionFactory.createConnection().createSession(true, 1);

		if (producerProperties.isPartitioned()) {
			int partitionCount = producerProperties.getPartitionCount();
			for (int i = 0; i < partitionCount; ++i) {
				String destination = producerDestination.getNameForPartition(i);
				Topic topic = (Topic) destinationResolver.resolveDestinationName(session, destination, true);
				topicPartitionRegistrar.addDestination(i, topic);
			}
		}
		else {
			String destination = producerDestination.getName();
			Topic topic = (Topic) destinationResolver.resolveDestinationName(session, destination, true);
			topicPartitionRegistrar.addDestination(-1, topic);
		}
		return this.jmsSendingMessageHandlerFactory.build(topicPartitionRegistrar);
	}

	@Override
	protected org.springframework.integration.core.MessageProducer createConsumerEndpoint(
			ConsumerDestination consumerDestination, String group, ExtendedConsumerProperties<JmsConsumerProperties> properties) throws Exception {
		Session session = connectionFactory.createConnection().createSession(true, 1);
		Queue queue = (Queue) destinationResolver.resolveDestinationName(session, consumerDestination.getName(), false);
		return jmsMessageDrivenChannelAdapterFactory.build(queue, properties);
	}

	@Override
	public JmsConsumerProperties getExtendedConsumerProperties(String channelName) {
		return this.extendedBindingProperties.getExtendedConsumerProperties(channelName);
	}

	@Override
	public JmsProducerProperties getExtendedProducerProperties(String channelName) {
		return this.extendedBindingProperties.getExtendedProducerProperties(channelName);
	}
}
