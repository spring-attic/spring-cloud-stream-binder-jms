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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.DefaultBinding;
import org.springframework.cloud.stream.binder.PartitionKeyExtractorStrategy;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.cloud.stream.binder.jms.spi.QueueProvisioner;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.jms.JmsMessageDrivenEndpoint;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.retry.support.RetryTemplate;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.getField;

public class JMSMessageChannelBinderTests {

    JmsTemplate jmsTemplate = mock(JmsTemplate.class);
    JMSMessageChannelBinder.JmsSendingMessageHandlerFactory jmsSendingMessageHandlerFactory = mock(JMSMessageChannelBinder.JmsSendingMessageHandlerFactory.class);
    ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
    private AbstractApplicationContext mockedApplicationContext;

    @Before
    public void setUp() throws Exception {
        mockedApplicationContext = mock(AbstractApplicationContext.class);
        ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
        when(mockedApplicationContext.getBeanFactory()).thenReturn(beanFactory);
    }

    @Test
    public void doBindConsumer_createsListenerAndBinding() throws Exception {
        JMSMessageChannelBinder.ListenerContainerFactory listenerContainerFactory = mock(JMSMessageChannelBinder.ListenerContainerFactory.class);
        JMSMessageChannelBinder.ConsumerBindingFactory consumerBindingFactory = mock(JMSMessageChannelBinder.ConsumerBindingFactory.class);
        JMSMessageChannelBinder.ProducerBindingFactory producerBindingFactory = mock(JMSMessageChannelBinder.ProducerBindingFactory.class);
        QueueProvisioner queueProvisioner = mock(QueueProvisioner.class);

        JMSMessageChannelBinder target = new JMSMessageChannelBinder(queueProvisioner, consumerBindingFactory, producerBindingFactory, listenerContainerFactory, jmsSendingMessageHandlerFactory);
        ConsumerProperties properties = new ConsumerProperties();
        MessageChannel inputTarget = new DirectChannel();

        AbstractMessageListenerContainer listenerContainer = mock(AbstractMessageListenerContainer.class);
        when(listenerContainerFactory.build(anyString())).thenReturn(listenerContainer);

        target.doBindConsumer("whatever", "group", inputTarget, properties);

        verify(listenerContainerFactory, times(1)).build("group");
        verify(consumerBindingFactory, times(1)).build(eq("whatever"), eq("group"), eq(inputTarget), eq(listenerContainer), any(RetryTemplate.class));
    }

    @Test
    public void doBindConsumer_whenPartitioned_provisionsWithPartitionIndex() throws Exception {
        JMSMessageChannelBinder.ListenerContainerFactory listenerContainerFactory = mock(JMSMessageChannelBinder.ListenerContainerFactory.class);
        JMSMessageChannelBinder.ConsumerBindingFactory consumerBindingFactory = mock(JMSMessageChannelBinder.ConsumerBindingFactory.class);
        JMSMessageChannelBinder.ProducerBindingFactory producerBindingFactory = mock(JMSMessageChannelBinder.ProducerBindingFactory.class);

        QueueProvisioner queueProvisioner = mock(QueueProvisioner.class);

        JMSMessageChannelBinder target = new JMSMessageChannelBinder(queueProvisioner, consumerBindingFactory, producerBindingFactory, listenerContainerFactory, jmsSendingMessageHandlerFactory);
        ConsumerProperties properties = new ConsumerProperties();
        MessageChannel inputTarget = new DirectChannel();

        properties.setPartitioned(true);
        properties.setInstanceCount(2);
        properties.setInstanceIndex(0);

        AbstractMessageListenerContainer listenerContainer = mock(AbstractMessageListenerContainer.class);
        when(listenerContainerFactory.build(anyString())).thenReturn(listenerContainer);

        target.doBindConsumer("topic", "group", inputTarget, properties);

        verify(queueProvisioner, times(1)).provisionTopicAndConsumerGroup("topic-0", "group-0");
        verify(listenerContainerFactory, times(1)).build("group-0");
        verify(consumerBindingFactory, times(1)).build(eq("topic"), eq("group"), eq(inputTarget), eq(listenerContainer), any(RetryTemplate.class));
    }

    @Test
    public void listenerContainerFactory_createsAndConfiguresMessageListenerContainer() throws Exception {
        ConnectionFactory factory = mock(ConnectionFactory.class);
        JMSMessageChannelBinder.ListenerContainerFactory listenerContainerFactory = new JMSMessageChannelBinder.ListenerContainerFactory(factory);

        AbstractMessageListenerContainer messageListenerContainer = listenerContainerFactory.build("my channel");

        assertThat(messageListenerContainer.getDestinationName(), Is.is("my channel"));
        assertThat(messageListenerContainer.getConnectionFactory(), Is.is(factory));
        assertThat(getField(messageListenerContainer, "pubSubDomain"), Is.is(false));
        assertThat("Transacted is not true. Transacted is required for guaranteed deliveries. " +
                        "In particular, some implementation will require it so they can eventually route the message to the DLQ",
                messageListenerContainer.isSessionTransacted(), is(true));

    }


    @Test
    public void doBindConsumer_createsAndConfiguresMessageListenerContainer() throws Exception {
        MessageChannel inputTarget = mock(MessageChannel.class);
        Connection connection = mock(Connection.class);
        Session session = mock(Session.class);
        MessageConsumer consumer = mock(MessageConsumer.class);
        QueueProvisioner queueProvisioner = mock(QueueProvisioner.class);

        when(session.createConsumer(any(), any())).thenReturn(consumer);
        when(connection.createSession(anyBoolean(), anyInt())).thenReturn(session);
        when(connectionFactory.createConnection()).thenReturn(connection);

        JMSMessageChannelBinder jmsMessageChannelBinder = new JMSMessageChannelBinder(connectionFactory, jmsTemplate, queueProvisioner);
        jmsMessageChannelBinder.setApplicationContext(mockedApplicationContext);
        DefaultBinding<MessageChannel> messageChannelBinding = (DefaultBinding<MessageChannel>) jmsMessageChannelBinder.doBindConsumer("my channel", "my group", inputTarget, new ConsumerProperties());
        JmsMessageDrivenEndpoint endpoint = (JmsMessageDrivenEndpoint) getField(messageChannelBinding, "endpoint");
        SimpleMessageListenerContainer listenerContainer = (SimpleMessageListenerContainer) getField(endpoint, "listenerContainer");

        assertThat(listenerContainer.getDestinationName(), Is.is("my group"));
    }

    @Test
    public void doBindProducer_whenRequiredGroupsProvided_provisionsRequiredGroups() throws Exception {
        JMSMessageChannelBinder.ListenerContainerFactory listenerContainerFactory = mock(JMSMessageChannelBinder.ListenerContainerFactory.class);
        JMSMessageChannelBinder.ConsumerBindingFactory consumerBindingFactory = mock(JMSMessageChannelBinder.ConsumerBindingFactory.class);
        JMSMessageChannelBinder.ProducerBindingFactory producerBindingFactory = mock(JMSMessageChannelBinder.ProducerBindingFactory.class);
        QueueProvisioner queueProvisioner = mock(QueueProvisioner.class);
        MessageChannel outboundBindTarget = mock(SubscribableChannel.class);
        ProducerProperties properties = new ProducerProperties();
        properties.setRequiredGroups("required-group", "required-group2");

        JMSMessageChannelBinder target = new JMSMessageChannelBinder(queueProvisioner, consumerBindingFactory, producerBindingFactory, listenerContainerFactory, jmsSendingMessageHandlerFactory);
        target.setApplicationContext(mockedApplicationContext);
        target.doBindProducer("mytopic", outboundBindTarget, properties);

        verify(queueProvisioner, times(1)).provisionTopicAndConsumerGroup("mytopic", "required-group", "required-group2");
    }

    @Test
    public void doBindProducer_whenPartitioningAndRequiredGroupsProvided_provisionsCartesianProductsOfGroupsAndPartitions() throws Exception {
        JMSMessageChannelBinder.ListenerContainerFactory listenerContainerFactory = mock(JMSMessageChannelBinder.ListenerContainerFactory.class);
        JMSMessageChannelBinder.ConsumerBindingFactory consumerBindingFactory = mock(JMSMessageChannelBinder.ConsumerBindingFactory.class);
        JMSMessageChannelBinder.ProducerBindingFactory producerBindingFactory = mock(JMSMessageChannelBinder.ProducerBindingFactory.class);
        QueueProvisioner queueProvisioner = mock(QueueProvisioner.class);
        MessageChannel outboundBindTarget = mock(SubscribableChannel.class);
        ProducerProperties properties = new ProducerProperties();
        properties.setPartitionCount(2);
        properties.setPartitionKeyExtractorClass(PartitionKeyExtractorStrategy.class);
        properties.setRequiredGroups("required-group", "required-group2");

        JMSMessageChannelBinder target = new JMSMessageChannelBinder(queueProvisioner, consumerBindingFactory, producerBindingFactory, listenerContainerFactory, jmsSendingMessageHandlerFactory);
        target.setApplicationContext(mockedApplicationContext);
        target.doBindProducer("mytopic", outboundBindTarget, properties);

        verify(queueProvisioner, times(1)).provisionTopicAndConsumerGroup("mytopic-0", "required-group-0", "required-group2-0");
        verify(queueProvisioner, times(1)).provisionTopicAndConsumerGroup("mytopic-1", "required-group-1", "required-group2-1");
    }

}