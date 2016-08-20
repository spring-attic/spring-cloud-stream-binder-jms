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

package org.springframework.cloud.stream.binder.jms.utils;

import org.springframework.context.Lifecycle;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.jms.DefaultJmsHeaderMapper;
import org.springframework.integration.jms.JmsHeaderMapper;
import org.springframework.integration.jms.JmsSendingMessageHandler;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.messaging.Message;

import javax.jms.Destination;
import javax.jms.JMSException;

import static org.springframework.cloud.stream.binder.BinderHeaders.PARTITION_HEADER;

/**
 * Extension of {@link JmsSendingMessageHandler}, with partition awareness.
 *
 * <p>Whenever a destination name is set, it builds up a SpEL expression
 * using the partition index of the message to route it to the appropriate
 * partition.
 *
 * @author José Carlos Valero
 * @since 1.1
 */
public class PartitionAwareJmsSendingMessageHandler extends AbstractMessageHandler implements Lifecycle {


    private JmsTemplate jmsTemplate;
    private final TopicPartitionRegistrar destinations;
    private volatile JmsHeaderMapper headerMapper = new DefaultJmsHeaderMapper();

    public PartitionAwareJmsSendingMessageHandler(JmsTemplate jmsTemplate,
                                                  TopicPartitionRegistrar destinations) {
        this.jmsTemplate = jmsTemplate;
        this.destinations = destinations;
    }

    protected void handleMessageInternal(Message<?> message) throws Exception {
        if(message == null) {
            throw new IllegalArgumentException("message must not be null");
        }
        Object destination = this.determineDestination(message);
        Object objectToSend = message.getPayload();
        HeaderMappingMessagePostProcessor messagePostProcessor = new HeaderMappingMessagePostProcessor(message, this.headerMapper);

        this.jmsTemplate.convertAndSend((Destination)destination, objectToSend, messagePostProcessor);
    }

    private Destination determineDestination(Message<?> message) {
        return destinations.getDestination(message.getHeaders().get(PARTITION_HEADER));
    }

    private static final class HeaderMappingMessagePostProcessor implements MessagePostProcessor {
        private final Message<?> integrationMessage;
        private final JmsHeaderMapper headerMapper;

        private HeaderMappingMessagePostProcessor(Message<?> integrationMessage, JmsHeaderMapper headerMapper) {
            this.integrationMessage = integrationMessage;
            this.headerMapper = headerMapper;
        }

        public javax.jms.Message postProcessMessage(javax.jms.Message jmsMessage) throws JMSException {
            this.headerMapper.fromHeaders(this.integrationMessage.getHeaders(), jmsMessage);
            return jmsMessage;
        }
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
