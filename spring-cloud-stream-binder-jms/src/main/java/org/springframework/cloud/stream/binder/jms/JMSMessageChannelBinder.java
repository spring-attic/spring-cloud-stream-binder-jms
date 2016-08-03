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

import java.util.Arrays;
import java.util.stream.IntStream;
import javax.jms.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.*;
import org.springframework.cloud.stream.binder.jms.utils.MessageRecoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.jms.ChannelPublishingJmsMessageListener;
import org.springframework.integration.jms.JmsMessageDrivenEndpoint;
import org.springframework.integration.jms.JmsSendingMessageHandler;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

/**
 * Binder definition for JMS.
 *
 * @author Jonathan Sharpe
 * @author Joseph Taylor
 * @author José Carlos Valero
 * @since 1.1
 */
public class JMSMessageChannelBinder extends AbstractBinder<MessageChannel, ConsumerProperties, ProducerProperties> {

    protected final Log logger = LogFactory.getLog(this.getClass());
    private ConsumerBindingFactory consumerBindingFactory;
    private JmsSendingMessageHandlerFactory jmsSendingMessageHandlerFactory;
    private ListenerContainerFactory listenerContainerFactory;
    private MessageRecoverer messageRecoverer;
    private ProducerBindingFactory producerBindingFactory;
    private QueueProvisioner queueProvisioner;

    public JMSMessageChannelBinder(ConnectionFactory factory,
                                   JmsTemplate template,
                                   QueueProvisioner queueProvisioner) throws JMSException {
        this(queueProvisioner,
                null,
                new ProducerBindingFactory(),
                new ListenerContainerFactory(factory),
                null);
        this.consumerBindingFactory = new ConsumerBindingFactory();
        this.jmsSendingMessageHandlerFactory = new JmsSendingMessageHandlerFactory(
                template);
    }

    public JMSMessageChannelBinder(QueueProvisioner queueProvisioner,
                                   ConsumerBindingFactory consumerBindingFactory,
                                   ProducerBindingFactory producerBindingFactory,
                                   ListenerContainerFactory listenerContainerFactory,
                                   JmsSendingMessageHandlerFactory jmsSendingMessageHandlerFactory
    ) throws JMSException {

        this.consumerBindingFactory = consumerBindingFactory;
        this.producerBindingFactory = producerBindingFactory;
        this.jmsSendingMessageHandlerFactory = jmsSendingMessageHandlerFactory;
        this.listenerContainerFactory = listenerContainerFactory;
        this.queueProvisioner = queueProvisioner;
    }

    /**
     * JMS Consumer - consumes JMS messages and writes them to the inputTarget, so it's an input to our Receiver application (Sink.INPUT)
     */
    @Override
    protected Binding<MessageChannel> doBindConsumer(String name,
                                                     String group,
                                                     MessageChannel inputTarget,
                                                     ConsumerProperties properties) {
        String groupName = buildRelativeQueueName(group, properties);
        String topicName = buildRelativeQueueName(name, properties);
        queueProvisioner.provisionTopicAndConsumerGroup(topicName, groupName);
        AbstractMessageListenerContainer listenerContainer = listenerContainerFactory.build(
                groupName);
        DefaultBinding<MessageChannel> binding = consumerBindingFactory.build(
                name,
                group,
                inputTarget,
                listenerContainer,
                getRetryTemplate(properties));
        return binding;
    }

    private RetryTemplate getRetryTemplate(ConsumerProperties properties) {
        RetryTemplate template = new RetryTemplate();
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(properties.getMaxAttempts());
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(properties.getBackOffInitialInterval());
        backOffPolicy.setMultiplier(properties.getBackOffMultiplier());
        backOffPolicy.setMaxInterval(properties.getBackOffMaxInterval());
        template.setRetryPolicy(retryPolicy);
        template.setBackOffPolicy(backOffPolicy);
        return template;
    }

    private String buildRelativeQueueName(String group,
                                          ConsumerProperties properties) {
        return properties.isPartitioned() ? String.format("%s-%s",
                group,
                properties.getInstanceIndex()) : group;
    }

    /**
     * JMS Producer - consumes Spring from the outboundBindTarget messages and writes them to JMS, so it's an output from our Sender application (Source.OUTPUT)
     */
    @Override
    protected Binding<MessageChannel> doBindProducer(String name,
                                                     MessageChannel outboundBindTarget,
                                                     ProducerProperties properties) {
        provisionQueuesAndTopics(name, properties);
        JmsSendingMessageHandler handler = jmsSendingMessageHandlerFactory.build(
                name,
                properties);
        DefaultBinding<MessageChannel> messageChannelDefaultBinding = producerBindingFactory.build(
                name,
                outboundBindTarget,
                handler,
                getBeanFactory());

        return messageChannelDefaultBinding;
    }

    private void provisionQueuesAndTopics(String name,
                                          ProducerProperties properties) {
        if (properties.isPartitioned()) {
            IntStream.range(0,
                    properties.getPartitionCount()).forEach(index -> {
                String[] requiredPartitionGroupNames = Arrays.stream(properties.getRequiredGroups())
                        .map(group -> String.format("%s-%s", group, index))
                        .toArray(size -> new String[size]);
                queueProvisioner.provisionTopicAndConsumerGroup(String.format(
                        "%s-%s",
                        name,
                        index), requiredPartitionGroupNames);
            });
        } else {
            queueProvisioner.provisionTopicAndConsumerGroup(name,
                    properties.getRequiredGroups());
        }
    }

    @Autowired
    public void setMessageRecoverer(MessageRecoverer messageRecoverer) {
        this.messageRecoverer = messageRecoverer;
    }

    /**
     * Factory to create Jms ListenerContainer
     */
    @Component
    public static class ListenerContainerFactory {

        private ConnectionFactory factory;

        public ListenerContainerFactory(ConnectionFactory factory) {
            this.factory = factory;
        }

        public AbstractMessageListenerContainer build(String name) {
            AbstractMessageListenerContainer listenerContainer = new SimpleMessageListenerContainer();
            listenerContainer.setDestinationName(name);
            listenerContainer.setPubSubDomain(false);
            listenerContainer.setConnectionFactory(factory);
            listenerContainer.setSessionTransacted(true); //Maybe configurable?
            return listenerContainer;
        }
    }

    /**
     * Factory to create bindings between Spring integration and JMS.
     */
    static class ProducerBindingFactory {

        public DefaultBinding<MessageChannel> build(String name,
                                                    MessageChannel outboundBindTarget,
                                                    JmsSendingMessageHandler handler,
                                                    BeanFactory beanFactory) {
            AbstractEndpoint consumer = new EventDrivenConsumer((SubscribableChannel) outboundBindTarget,
                    handler);
            consumer.setBeanFactory(beanFactory);
            consumer.setBeanName("outbound." + name);
            consumer.afterPropertiesSet();
            consumer.start();
            return new DefaultBinding<>(name,
                    null,
                    outboundBindTarget,
                    consumer);
        }
    }

    /**
     * Factory to create bindings between JMS and Spring integration.
     */
    public class ConsumerBindingFactory {

        public static final String RETRY_CONTEXT_MESSAGE_ATTRIBUTE = "message";

        public DefaultBinding<MessageChannel> build(String name,
                                                    String group,
                                                    MessageChannel moduleInputChannel,
                                                    AbstractMessageListenerContainer listenerContainer,
                                                    RetryTemplate retryTemplate) {

            // the listener is the channel adapter. it connects the JMS endpoint to the input
            // channel by converting the messages that the listener container passes to it
            ChannelPublishingJmsMessageListener listener = new ChannelPublishingJmsMessageListener() {
                @Override
                public void onMessage(Message jmsMessage,
                                      Session session) throws JMSException {
                    retryTemplate.execute(
                            continueRetryContext -> {
                                try {
                                    continueRetryContext.setAttribute(
                                            RETRY_CONTEXT_MESSAGE_ATTRIBUTE,
                                            jmsMessage);
                                    super.onMessage(jmsMessage, session);
                                } catch (JMSException e) {
                                    logger.error("Failed to send message",
                                            e);
                                    throw new RuntimeException(e);
                                }
                                return null;
                            },
                            recoverRetryContext -> {
                                if (messageRecoverer != null) {
                                    Message message = (Message) recoverRetryContext.getAttribute(
                                            RETRY_CONTEXT_MESSAGE_ATTRIBUTE);
                                    messageRecoverer.recover(message,
                                            recoverRetryContext.getLastThrowable());
                                } else {
                                    logger.warn(
                                            "No message recoverer was configured. Messages will be discarded.");
                                }
                                return null;
                            }
                    );
                }
            };

            listener.setMessageConverter(new SimpleMessageConverter() {
                @Override
                protected byte[] extractByteArrayFromMessage(BytesMessage message) throws JMSException {
                    message.reset();
                    return super.extractByteArrayFromMessage(message);
                }
            });

            //The endpoint allows Spring Integration to control the lifecycle of the listener
            JmsMessageDrivenEndpoint endpoint = new JmsMessageDrivenEndpoint(
                    listenerContainer,
                    listener);
            endpoint.setBeanName("inbound." + name);

            SubscribableChannel deserializingChannel = getDeserializingChannel(name,
                    moduleInputChannel);

            listener.setRequestChannel(deserializingChannel);

            DefaultBinding<MessageChannel> binding = new DefaultBinding<>(name,
                    group,
                    deserializingChannel,
                    endpoint);

            endpoint.start();

            return binding;
        }

        private SubscribableChannel getDeserializingChannel(String name,
                                                            MessageChannel moduleInputChannel) {
            DeserializingReceivingHandler deserializingReceivingHandler = new DeserializingReceivingHandler();
            deserializingReceivingHandler.setOutputChannel(moduleInputChannel);
            deserializingReceivingHandler.setBeanName(name + ".convert.bridge");
            deserializingReceivingHandler.afterPropertiesSet();

            DirectChannel deserializingChannel = new DirectChannel();
            deserializingChannel.setBeanFactory(getBeanFactory());
            deserializingChannel.setBeanName(name + ".bridge");
            deserializingChannel.subscribe(deserializingReceivingHandler);

            return deserializingChannel;
        }
    }

    /**
     * Factory to create JMS message handlers
     */
    class JmsSendingMessageHandlerFactory {

        private final JmsTemplate template;

        public JmsSendingMessageHandlerFactory(JmsTemplate template) {
            this.template = template;
        }

        public JmsSendingMessageHandler build(String name,
                                              ProducerProperties producerProperties) {
            final PartitionHandler partitionHandler = new PartitionHandler(
                    getBeanFactory(),
                    JMSMessageChannelBinder.this.evaluationContext,
                    partitionSelector,
                    producerProperties
            );
            template.setPubSubDomain(true);
            JmsSendingMessageHandler handler = new PartitionAwareJmsSendingMessageHandler(
                    this.template,
                    producerProperties,
                    partitionHandler,
                    PARTITION_HEADER);
            handler.setDestinationName(name);
            handler.setBeanFactory(getBeanFactory());
            handler.afterPropertiesSet();

            return handler;
        }
    }

    /**
     * Extension of {@link JmsSendingMessageHandler}, with partition awareness.
     *
     * <p>Whenever a destination name is set, it builds up a SpEL expression
     * using the partition index of the message to route it to the appropriate
     * partition.
     */
    public class PartitionAwareJmsSendingMessageHandler extends JmsSendingMessageHandler {


        private final ProducerProperties producerProperties;
        private final String partitionHeaderName;
        private PartitionHandler partitionHandler;

        public PartitionAwareJmsSendingMessageHandler(JmsTemplate jmsTemplate,
                                                      ProducerProperties producerProperties,
                                                      PartitionHandler partitionHandler,
                                                      String partitionHeaderName) {
            super(jmsTemplate);
            this.producerProperties = producerProperties;
            sanitizeSpelConstant(partitionHeaderName);
            this.partitionHandler = partitionHandler;
            this.partitionHeaderName = partitionHeaderName;
        }

        @Override
        protected void handleMessageInternal(org.springframework.messaging.Message<?> message) throws Exception {
            MessageValues messageValues = serializePayloadIfNecessary(message);

            if (producerProperties.isPartitioned()) {
                messageValues.put(partitionHeaderName,
                        this.partitionHandler.determinePartition(message));
            }

            super.handleMessageInternal(messageValues.toMessage(
                    getMessageBuilderFactory()));
        }

        @Override
        public void setDestination(Destination destination) {
            throw new UnsupportedOperationException(
                    "Destination is not supported. Please use destination name instead");
        }

        @Override
        public void setDestinationName(String destinationName) {
            if (!producerProperties.isPartitioned()) {
                super.setDestinationName(destinationName);
            } else {
                sanitizeSpelConstant(destinationName);
                Expression destinationExpression = new SpelExpressionParser()
                        .parseExpression(String.format("'%s-' + headers['%s']",
                                destinationName,
                                partitionHeaderName));
                super.setDestinationExpression(destinationExpression);
            }
        }

        private void sanitizeSpelConstant(String spelConstant) {
            if (spelConstant.contains("'"))
                throw new IllegalArgumentException(
                        "The value %s contains an illegal character \"'\" ");
        }

    }

    /**
     * Receiving handler to deserialize message payload as needed.
     */
    private final class DeserializingReceivingHandler extends AbstractReplyProducingMessageHandler {

        private DeserializingReceivingHandler() {
            super();
            this.setBeanFactory(JMSMessageChannelBinder.this.getBeanFactory());
        }

        @Override
        protected Object handleRequestMessage(org.springframework.messaging.Message<?> requestMessage) {
            return deserializePayloadIfNecessary(requestMessage).toMessage(
                    getMessageBuilderFactory());
        }

        @Override
        protected boolean shouldCopyRequestHeaders() {
            // The headers have already been copied by an earlier process.
            return false;
        }

    }
}
