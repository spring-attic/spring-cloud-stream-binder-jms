package org.springframework.cloud.stream.binder.jms;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.stream.binder.*;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.jms.ChannelPublishingJmsMessageListener;
import org.springframework.integration.jms.JmsMessageDrivenEndpoint;
import org.springframework.integration.jms.JmsSendingMessageHandler;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.interceptor.MethodInvocationRecoverer;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import javax.jms.*;

public class JMSMessageChannelBinder extends AbstractBinder<MessageChannel, ConsumerProperties, ProducerProperties> {

    private BindingFactory bindingFactory;
    private JmsTemplate template;
    private ListenerContainerFactory listenerContainerFactory;
    private QueueProvisioner queueProvisioner;
    protected final Log logger = LogFactory.getLog(this.getClass());

    public JMSMessageChannelBinder(ConnectionFactory factory, JmsTemplate template, QueueProvisioner queueProvisioner) throws JMSException {
        this(new ListenerContainerFactory(factory), new BindingFactory(), template, queueProvisioner);
    }

    public JMSMessageChannelBinder(ListenerContainerFactory listenerContainerFactory, BindingFactory bindingFactory, JmsTemplate template, QueueProvisioner queueProvisioner) throws JMSException {
        this.bindingFactory = bindingFactory;
        this.template = template;
        this.listenerContainerFactory = listenerContainerFactory;
        this.queueProvisioner = queueProvisioner;
    }

    /**
     * JMS Consumer - consumes JMS messages and writes them to the inputTarget, so it's an input to our application (Sink.INPUT)
     */
    @Override
    protected Binding<MessageChannel> doBindConsumer(String name, String group, MessageChannel inputTarget, ConsumerProperties properties) {
        queueProvisioner.provisionTopicAndConsumerGroup(name, group);
        AbstractMessageListenerContainer listenerContainer = listenerContainerFactory.build(group);
        DefaultBinding<MessageChannel> binding = bindingFactory.build(name, group, inputTarget, listenerContainer, buildRetryTemplateIfRetryEnabled(properties));
        return binding;
    }

    /**
     * JMS Producer - consumes Spring from the outboundBindTarget messages and writes them to JMS, so it's an output from our application (Source.OUTPUT)
     */
    @Override
    protected Binding<MessageChannel> doBindProducer(String name, MessageChannel outboundBindTarget, ProducerProperties properties) {
        queueProvisioner.provisionTopicAndConsumerGroup(name, null);

        template.setPubSubDomain(true);

        JmsSendingMessageHandler handler = new JmsSendingMessageHandler(template);
        handler.setDestinationName(name);

        AbstractEndpoint consumer = new EventDrivenConsumer((SubscribableChannel) outboundBindTarget, handler);

        consumer.setBeanFactory(getBeanFactory());
        consumer.setBeanName("outbound." + name);
        consumer.afterPropertiesSet();
        consumer.start();

        return new DefaultBinding<>(name, null, outboundBindTarget, consumer);
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


    public static class BindingFactory {

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
}
