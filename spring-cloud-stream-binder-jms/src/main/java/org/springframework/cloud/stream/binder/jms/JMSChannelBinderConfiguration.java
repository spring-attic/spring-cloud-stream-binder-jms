package org.springframework.cloud.stream.binder.jms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.codec.Codec;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

public class JMSChannelBinderConfiguration {

    @Autowired
    private Codec codec;

    @Bean
    JMSMessageChannelBinder jmsMessageChannelBinder(JmsTemplate template, ConnectionFactory connectionFactory, QueueProvisioner queueProvisioner) throws JMSException {
        JMSMessageChannelBinder jmsMessageChannelBinder = new JMSMessageChannelBinder(connectionFactory, template, queueProvisioner);
        jmsMessageChannelBinder.setCodec(codec);
        return jmsMessageChannelBinder;
    }

}
