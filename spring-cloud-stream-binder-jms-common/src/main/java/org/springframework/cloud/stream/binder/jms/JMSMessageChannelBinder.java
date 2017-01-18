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

import java.util.Collection;

import javax.jms.Queue;

import org.springframework.cloud.stream.binder.AbstractMessageChannelBinder;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.cloud.stream.binder.jms.spi.QueueProvisioner;
import org.springframework.cloud.stream.binder.jms.utils.DestinationNameResolver;
import org.springframework.cloud.stream.binder.jms.utils.DestinationNames;
import org.springframework.cloud.stream.binder.jms.utils.JmsMessageDrivenChannelAdapterFactory;
import org.springframework.cloud.stream.binder.jms.utils.JmsSendingMessageHandlerFactory;
import org.springframework.cloud.stream.binder.jms.utils.TopicPartitionRegistrar;
import org.springframework.messaging.MessageHandler;

/**
 * Binder definition for JMS.
 *
 * @author Jack Galilee
 * @author Jonathan Sharpe
 * @author Joseph Taylor
 * @author José Carlos Valero
 * @author Gary Russell
 * @since 1.1
 */
public class JMSMessageChannelBinder
		extends AbstractMessageChannelBinder<ConsumerProperties, ProducerProperties, Queue, TopicPartitionRegistrar> {

	private final QueueProvisioner queueProvisioner;

	private final DestinationNameResolver destinationNameResolver;

	private final JmsSendingMessageHandlerFactory jmsSendingMessageHandlerFactory;
	private final JmsMessageDrivenChannelAdapterFactory jmsMessageDrivenChannelAdapterFactory;

	public JMSMessageChannelBinder(QueueProvisioner queueProvisioner, DestinationNameResolver destinationNameResolver,
			JmsSendingMessageHandlerFactory jmsSendingMessageHandlerFactory,
			JmsMessageDrivenChannelAdapterFactory jmsMessageDrivenChannelAdapterFactory) {
		super(true, null);
		this.destinationNameResolver = destinationNameResolver;
		this.jmsSendingMessageHandlerFactory = jmsSendingMessageHandlerFactory;
		this.jmsMessageDrivenChannelAdapterFactory = jmsMessageDrivenChannelAdapterFactory;
		this.queueProvisioner = queueProvisioner;
	}

	@Override
	protected TopicPartitionRegistrar createProducerDestinationIfNecessary(String name,
														ProducerProperties properties) {
		TopicPartitionRegistrar topicPartitionRegistrar = new TopicPartitionRegistrar();
		Collection<DestinationNames> topicAndQueueNames =
				this.destinationNameResolver.resolveTopicAndQueueNameForRequiredGroups(name, properties);

		QueueProvisioner.Destinations destinations;
		for (DestinationNames destinationNames : topicAndQueueNames) {
			destinations = this.queueProvisioner.provisionTopicAndConsumerGroup(destinationNames.getTopicName(),
					destinationNames.getGroupNames());
			topicPartitionRegistrar.addDestination(destinationNames.getPartitionIndex(),destinations.getTopic());
		}
		return topicPartitionRegistrar;
	}

	@Override
	protected Queue createConsumerDestinationIfNecessary(String name,
														 String group,
														 ConsumerProperties properties) {
		String groupName = this.destinationNameResolver.resolveQueueNameForInputGroup(group, properties);
		String topicName = this.destinationNameResolver.resolveQueueNameForInputGroup(name, properties);
		QueueProvisioner.Destinations destinations =
				this.queueProvisioner.provisionTopicAndConsumerGroup(topicName, groupName);
		return destinations.getGroups()[0];

	}

	@Override
	protected MessageHandler createProducerMessageHandler(TopicPartitionRegistrar destination,
														  ProducerProperties producerProperties) throws Exception {
		return this.jmsSendingMessageHandlerFactory.build(destination);
	}

	@Override
	protected org.springframework.integration.core.MessageProducer createConsumerEndpoint(
			String name, String group, Queue destination, ConsumerProperties properties) {
		return jmsMessageDrivenChannelAdapterFactory.build(destination, properties);
	}

}
