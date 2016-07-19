package org.springframework.cloud.stream.binder.jms;

import org.junit.Test;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.DefaultBinding;
import org.springframework.cloud.stream.binder.jms.JMSMessageChannelBinder;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.jms.JmsMessageDrivenEndpoint;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.messaging.MessageChannel;
import org.springframework.retry.support.RetryTemplate;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.getField;

public class JMSMessageChannelBinderTest {

    JmsTemplate jmsTemplate = mock(JmsTemplate.class);
    ConnectionFactory connectionFactory = mock(ConnectionFactory.class);


    @Test
    public void doBindConsumer_createsListenerAndBinding() throws Exception {
        JMSMessageChannelBinder.ListenerContainerFactory listenerContainerFactory = mock(JMSMessageChannelBinder.ListenerContainerFactory.class);
        JMSMessageChannelBinder.BindingFactory bindingFactory = mock(JMSMessageChannelBinder.BindingFactory.class);

        JMSMessageChannelBinder target = new JMSMessageChannelBinder(listenerContainerFactory, bindingFactory, null);
        ConsumerProperties properties = new ConsumerProperties();
        MessageChannel inputTarget = new DirectChannel();

        AbstractMessageListenerContainer listenerContainer = mock(AbstractMessageListenerContainer.class);
        when(listenerContainerFactory.build(anyString())).thenReturn(listenerContainer);

        target.doBindConsumer("whatever", "group", inputTarget, properties);

        verify(listenerContainerFactory, times(1)).build("whatever");
        verify(bindingFactory, times(1)).build(eq("whatever"), eq("group"), eq(inputTarget), eq(listenerContainer), any(RetryTemplate.class));
    }

    @Test
    public void listenerContainerFactory_createsAndConfiguresMessageListenerContainer() throws Exception {
        ConnectionFactory factory = mock(ConnectionFactory.class);
        JMSMessageChannelBinder.ListenerContainerFactory listenerContainerFactory = new JMSMessageChannelBinder.ListenerContainerFactory(factory);

        AbstractMessageListenerContainer messageListenerContainer = listenerContainerFactory.build("my channel");

        assertThat(messageListenerContainer.getDestinationName(), is("my channel"));
        assertThat(messageListenerContainer.getConnectionFactory(), is(factory));
        assertThat(getField(messageListenerContainer, "pubSubDomain"), is(true));
    }


    @Test
    public void doBindConsumer_createsAndConfiguresMessageListenerContainer() throws Exception {
        MessageChannel inputTarget = mock(MessageChannel.class);
        Connection connection = mock(Connection.class);
        Session session = mock(Session.class);
        MessageConsumer consumer = mock(MessageConsumer.class);

        when(session.createConsumer(any(), any())).thenReturn(consumer);
        when(connection.createSession(anyBoolean(), anyInt())).thenReturn(session);
        when(connectionFactory.createConnection()).thenReturn(connection);

        JMSMessageChannelBinder jmsMessageChannelBinder = new JMSMessageChannelBinder(connectionFactory, jmsTemplate);
        DefaultBinding<MessageChannel> messageChannelBinding = (DefaultBinding<MessageChannel>) jmsMessageChannelBinder.doBindConsumer("my channel", "", inputTarget, new ConsumerProperties());
        JmsMessageDrivenEndpoint endpoint = (JmsMessageDrivenEndpoint) getField(messageChannelBinding, "endpoint");
        SimpleMessageListenerContainer listenerContainer = (SimpleMessageListenerContainer) getField(endpoint, "listenerContainer");

        assertThat(listenerContainer.getDestinationName(), is("my channel"));
    }
}