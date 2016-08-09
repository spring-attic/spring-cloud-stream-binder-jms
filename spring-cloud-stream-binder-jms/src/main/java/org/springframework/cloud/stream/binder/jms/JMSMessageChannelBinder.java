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

import java.util.Map;
import javax.jms.JMSException;
import javax.jms.Queue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.stream.binder.AbstractMessageChannelBinder;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.cloud.stream.binder.jms.spi.QueueProvisioner;
import org.springframework.cloud.stream.binder.jms.utils.JmsMessageDrivenChannelAdapterFactory;
import org.springframework.cloud.stream.binder.jms.utils.JmsSendingMessageHandlerFactory;
import org.springframework.cloud.stream.binder.jms.utils.QueueNameResolver;
import org.springframework.integration.dsl.jms.JmsMessageDrivenChannelAdapter;
import org.springframework.messaging.MessageHandler;

/**
 * Binder definition for JMS.
 *
 * @author Jack Galilee
 * @author Jonathan Sharpe
 * @author Joseph Taylor
 * @author José Carlos Valero
 * @since 1.1
 */
public class JMSMessageChannelBinder extends AbstractMessageChannelBinder<ConsumerProperties, ProducerProperties, Queue> {
    protected final Log logger = LogFactory.getLog(this.getClass());
    private final QueueProvisioner queueProvisioner;
    private final QueueNameResolver queueNameResolver;
    private JmsSendingMessageHandlerFactory jmsSendingMessageHandlerFactory;
    private JmsMessageDrivenChannelAdapterFactory jmsMessageDrivenChannelAdapterFactory;

    public JMSMessageChannelBinder(QueueProvisioner queueProvisioner,
                                   QueueNameResolver queueNameResolver,
                                   JmsSendingMessageHandlerFactory jmsSendingMessageHandlerFactory,
                                   JmsMessageDrivenChannelAdapterFactory jmsMessageDrivenChannelAdapterFactory
    ) throws JMSException {
        super(true, null);
        this.queueNameResolver = queueNameResolver;
        this.jmsSendingMessageHandlerFactory = jmsSendingMessageHandlerFactory;
        this.jmsMessageDrivenChannelAdapterFactory = jmsMessageDrivenChannelAdapterFactory;
        this.queueProvisioner = queueProvisioner;
    }

    @Override
    protected void createProducerDestinationIfNecessary(String name,
                                                        ProducerProperties properties) {
        provisionQueuesAndTopics(name, properties);
    }

    @Override
    protected Queue createConsumerDestinationIfNecessary(String name,
                                                         String group,
                                                         ConsumerProperties properties) {
        String groupName = queueNameResolver.resolveQueueNameForInputGroup(group, properties);
        String topicName = queueNameResolver.resolveQueueNameForInputGroup(name, properties);
        queueProvisioner.provisionTopicAndConsumerGroup(topicName, groupName);
        //TODO: This reference will only be used in the create consumerEndpoint, so we can skip it for now as long as destination is not used in createConsumerEndpoint.
        return null;

    }

    @Override
    protected MessageHandler createProducerMessageHandler(String destination,
                                                          ProducerProperties producerProperties) throws Exception {
        return jmsSendingMessageHandlerFactory.build(
                destination,
                producerProperties);
    }

    @Override
    protected org.springframework.integration.core.MessageProducer createConsumerEndpoint(
            String name,
            String group,
            Queue destination,
            ConsumerProperties properties) {


        JmsMessageDrivenChannelAdapter jmsMessageDrivenChannelAdapter =
                jmsMessageDrivenChannelAdapterFactory.build(group, properties);
        return jmsMessageDrivenChannelAdapter;
    }

    private void provisionQueuesAndTopics(String name,
                                          ProducerProperties properties) {
        Map<String, String[]> topicAndQueueNames =
                queueNameResolver.resolveQueueNameForRequiredGroups(name, properties);

        for (Map.Entry<String, String[]> entry : topicAndQueueNames.entrySet()) {
            queueProvisioner.provisionTopicAndConsumerGroup(entry.getKey(),
                    entry.getValue());
        }
    }

}
