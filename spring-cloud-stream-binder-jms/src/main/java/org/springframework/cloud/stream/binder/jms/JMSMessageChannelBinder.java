/*
 *  Copyright 2002-2016 the original author or authors.
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

package org.springframework.cloud.stream.binder.jms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.stream.binder.AbstractMessageChannelBinder;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.cloud.stream.binder.jms.spi.QueueProvisioner;
import org.springframework.cloud.stream.binder.jms.utils.*;
import org.springframework.integration.dsl.jms.JmsMessageDrivenChannelAdapter;
import org.springframework.messaging.MessageHandler;

import javax.jms.JMSException;
import javax.jms.Queue;
import java.util.Collection;

/**
 * Binder definition for JMS.
 *
 * @author Jack Galilee
 * @author Jonathan Sharpe
 * @author Joseph Taylor
 * @author José Carlos Valero
 * @since 1.1
 */
public class JMSMessageChannelBinder extends AbstractMessageChannelBinder<ConsumerProperties, ProducerProperties, Queue, TopicPartitionRegistrar> {
    protected final Log logger = LogFactory.getLog(this.getClass());
    private final QueueProvisioner queueProvisioner;
    private final DestinationNameResolver destinationNameResolver;

    private JmsSendingMessageHandlerFactory jmsSendingMessageHandlerFactory;
    private JmsMessageDrivenChannelAdapterFactory jmsMessageDrivenChannelAdapterFactory;

    public JMSMessageChannelBinder(QueueProvisioner queueProvisioner,
                                   DestinationNameResolver destinationNameResolver,
                                   JmsSendingMessageHandlerFactory jmsSendingMessageHandlerFactory,
                                   JmsMessageDrivenChannelAdapterFactory jmsMessageDrivenChannelAdapterFactory
    ) throws JMSException {
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
                destinationNameResolver.resolveTopicAndQueueNameForRequiredGroups(name, properties);

        QueueProvisioner.Destinations destinations;
        for (DestinationNames destinationNames : topicAndQueueNames) {
            destinations = queueProvisioner.provisionTopicAndConsumerGroup(destinationNames.getTopicName(), destinationNames.getGroupNames());
            topicPartitionRegistrar.addDestination(destinationNames.getPartitionIndex(),destinations.getTopic());
        }
        return topicPartitionRegistrar;
    }

    @Override
    protected Queue createConsumerDestinationIfNecessary(String name,
                                                         String group,
                                                         ConsumerProperties properties) {
        String groupName = destinationNameResolver.resolveQueueNameForInputGroup(group, properties);
        String topicName = destinationNameResolver.resolveQueueNameForInputGroup(name, properties);
        QueueProvisioner.Destinations destinations = queueProvisioner.provisionTopicAndConsumerGroup(topicName, groupName);
        return destinations.getGroups()[0];

    }

    @Override
    protected MessageHandler createProducerMessageHandler(TopicPartitionRegistrar destination,
                                                          ProducerProperties producerProperties) throws Exception {
        return jmsSendingMessageHandlerFactory.build(destination);
    }

    @Override
    protected org.springframework.integration.core.MessageProducer createConsumerEndpoint(
            String name,
            String group,
            Queue destination,
            ConsumerProperties properties) {


        JmsMessageDrivenChannelAdapter jmsMessageDrivenChannelAdapter =
                jmsMessageDrivenChannelAdapterFactory.build(destination, properties);
        return jmsMessageDrivenChannelAdapter;
    }

}
