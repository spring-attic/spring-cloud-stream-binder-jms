package org.springframework.cloud.stream.binder.jms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.stream.binder.*;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.jms.ChannelPublishingJmsMessageListener;
import org.springframework.integration.jms.JmsMessageDrivenEndpoint;
import org.springframework.integration.jms.JmsSendingMessageHandler;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.messaging.*;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import javax.jms.*;
import javax.jms.Message;
import java.util.Arrays;
import java.util.stream.IntStream;

public class JMSMessageChannelBinder extends AbstractBinder<MessageChannel, ConsumerProperties, ProducerProperties> {

    private ConsumerBindingFactory consumerBindingFactory;
    private ProducerBindingFactory producerBindingFactory;
    private JmsSendingMessageHandlerFactory jmsSendingMessageHandlerFactory;
    private ListenerContainerFactory listenerContainerFactory;
    private QueueProvisioner queueProvisioner;
    protected final Log logger = LogFactory.getLog(this.getClass());

    public JMSMessageChannelBinder(ConnectionFactory factory, JmsTemplate template, QueueProvisioner queueProvisioner) throws JMSException {
        this(queueProvisioner, new ConsumerBindingFactory(), new ProducerBindingFactory(), new ListenerContainerFactory(factory), null);
        this.jmsSendingMessageHandlerFactory = new JmsSendingMessageHandlerFactory(template);
    }

    public JMSMessageChannelBinder(QueueProvisioner queueProvisioner, ConsumerBindingFactory consumerBindingFactory, ProducerBindingFactory producerBindingFactory, ListenerContainerFactory listenerContainerFactory, JmsSendingMessageHandlerFactory jmsSendingMessageHandlerFactory) throws JMSException {
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
    protected Binding<MessageChannel> doBindConsumer(String name, String group, MessageChannel inputTarget, ConsumerProperties properties) {
        String groupName = buildRelativeQueueName(group, properties);
        String topicName = buildRelativeQueueName(name, properties);
        queueProvisioner.provisionTopicAndConsumerGroup(topicName, groupName);
        AbstractMessageListenerContainer listenerContainer = listenerContainerFactory.build(groupName);
        DefaultBinding<MessageChannel> binding = consumerBindingFactory.build(name, group, inputTarget, listenerContainer, buildRetryTemplateIfRetryEnabled(properties));
        return binding;
    }

    private String buildRelativeQueueName(String group, ConsumerProperties properties) {
        return properties.isPartitioned() ? String.format("%s-%s", group, properties.getInstanceIndex()) : group;
    }

    /**
     * JMS Producer - consumes Spring from the outboundBindTarget messages and writes them to JMS, so it's an output from our Sender application (Source.OUTPUT)
     */
    @Override
    protected Binding<MessageChannel> doBindProducer(String name, MessageChannel outboundBindTarget, ProducerProperties properties) {
        provisionQueuesAndTopics(name, properties);
        JmsSendingMessageHandler handler = jmsSendingMessageHandlerFactory.build(name, properties);
        DefaultBinding<MessageChannel> messageChannelDefaultBinding = producerBindingFactory.build(name, outboundBindTarget, handler, getBeanFactory());

        return messageChannelDefaultBinding;
    }

    private void provisionQueuesAndTopics(String name, ProducerProperties properties) {
        if (properties.isPartitioned()) {
            IntStream.range(0,properties.getPartitionCount()).forEach(index -> {
                String[] requiredPartitionGroupNames = Arrays.stream(properties.getRequiredGroups())
                        .map(group -> String.format("%s-%s", group, index))
                        .toArray(size -> new String[size]);
                queueProvisioner.provisionTopicAndConsumerGroup(String.format("%s-%s", name, index), requiredPartitionGroupNames);
            });
        } else {
            queueProvisioner.provisionTopicAndConsumerGroup(name, properties.getRequiredGroups());
        }
    }

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
            return listenerContainer;
        }
    }


    public static class ConsumerBindingFactory {

        public DefaultBinding<MessageChannel> build(String name, String group, MessageChannel inputTarget, AbstractMessageListenerContainer listenerContainer, RetryTemplate retryTemplate) {
            ChannelPublishingJmsMessageListener listener = new ChannelPublishingJmsMessageListener(){
                @Override
                public void onMessage(Message jmsMessage, Session session) throws JMSException {
                    if (retryTemplate == null) {
                        super.onMessage(jmsMessage, session);
                    }
                    else {
                        retryTemplate.execute(retryContext -> {
                            try {
                                super.onMessage(jmsMessage, session);
                            } catch (JMSException e) {
                                logger.error("Failed to send message", e);
                            }
                            return null;
                        });
                    }
                }
            };
            listener.setRequestChannel(inputTarget);

            AbstractEndpoint endpoint = new JmsMessageDrivenEndpoint(listenerContainer, listener);
            DefaultBinding<MessageChannel> binding = new DefaultBinding<>(name, group, inputTarget, endpoint);
            endpoint.setBeanName("inbound." + name);
            endpoint.start();
            return binding;
        }
    }

    static class ProducerBindingFactory {


        public DefaultBinding<MessageChannel> build(String name, MessageChannel outboundBindTarget, JmsSendingMessageHandler handler, BeanFactory beanFactory) {
            AbstractEndpoint consumer = new EventDrivenConsumer((SubscribableChannel) outboundBindTarget, handler);
            consumer.setBeanFactory(beanFactory);
            consumer.setBeanName("outbound." + name);
            consumer.afterPropertiesSet();
            consumer.start();
            return new DefaultBinding<>(name, null, outboundBindTarget, consumer);
        }
    }

    class JmsSendingMessageHandlerFactory {

        private final JmsTemplate template;

        public JmsSendingMessageHandlerFactory(JmsTemplate template) {
            this.template = template;
        }

        public JmsSendingMessageHandler build(String name, ProducerProperties producerProperties) {
            ExpressionParser parser = new SpelExpressionParser();
            final PartitionHandler partitionHandler = new PartitionHandler(getBeanFactory(), JMSMessageChannelBinder.this.evaluationContext, partitionSelector, producerProperties);
            template.setPubSubDomain(true);
            JmsSendingMessageHandler handler = new PartitionAwareJmsSendingMessageHandler(this.template, producerProperties, partitionHandler, PARTITION_HEADER);
            handler.setDestinationName(name);
            return handler;
        }
    }

    public class PartitionAwareJmsSendingMessageHandler extends JmsSendingMessageHandler{


        private final ProducerProperties producerProperties;
        private PartitionHandler partitionHandler;
        private final String partitionHeaderName;

        public PartitionAwareJmsSendingMessageHandler(JmsTemplate jmsTemplate, ProducerProperties producerProperties, PartitionHandler partitionHandler, String partitionHeaderName) {
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
            messageValues.setPayload(message.getPayload());
            super.handleMessageInternal(messageValues.toMessage(getMessageBuilderFactory()));
        }

        @Override
        public void setDestination(Destination destination) {
            throw new UnsupportedOperationException("Destination is not supported. Please use destination name instead");
        }

        @Override
        public void setDestinationName(String destinationName) {
            if(!producerProperties.isPartitioned()){
                super.setDestinationName(destinationName);
            }else{
                sanitizeSpelConstant(destinationName);
                Expression destinationExpression = new SpelExpressionParser()
                        .parseExpression(String.format("'%s-' + headers['%s']", destinationName, partitionHeaderName));
                super.setDestinationExpression(destinationExpression);
            }
        }

        private void sanitizeSpelConstant(String spelConstant){
            if(spelConstant.contains("'"))
                throw new IllegalArgumentException("The value %s contains an illegal character \"'\" ");
        }
    }
}
