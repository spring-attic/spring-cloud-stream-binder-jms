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

package org.springframework.cloud.stream.binder.jms.config;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.binder.jms.JMSMessageChannelBinder;
import org.springframework.cloud.stream.binder.jms.spi.QueueProvisioner;
import org.springframework.cloud.stream.binder.jms.utils.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.codec.Codec;
import org.springframework.integration.jms.DefaultJmsHeaderMapper;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;

/**
 * Configuration class containing required beans in order to set up the JMS
 * binder.
 *
 * @author Jack Galilee
 * @author Jonathan Sharpe
 * @author Joseph Taylor
 * @author José Carlos Valero
 * @author Donovan Muller
 * @since 1.1
 */
@Configuration
@EnableConfigurationProperties(JmsBinderConfigurationProperties.class)
public class JmsBinderGlobalConfiguration {

    @Autowired
    private ConnectionFactory connectionFactory;

    @Bean
    public DestinationNameResolver queueNameResolver() throws Exception {
        return new DestinationNameResolver(new Base64UrlNamingStrategy("anonymous."));
    }

    @Bean
    @ConditionalOnMissingBean(MessageRecoverer.class)
    MessageRecoverer defaultMessageRecoverer(QueueProvisioner queueProvisioner) throws Exception {
        return new RepublishMessageRecoverer(queueProvisioner, jmsTemplate(), new DefaultJmsHeaderMapper());
    }

    @Bean
    ListenerContainerFactory listenerContainerFactory(JmsBinderConfigurationProperties configurationProperties) throws Exception {
        return new ListenerContainerFactory(configurationProperties, connectionFactory);
    }

    @Bean
    public JmsMessageDrivenChannelAdapterFactory jmsMessageDrivenChannelAdapterFactory(MessageRecoverer messageRecoverer,
                                                                                       ListenerContainerFactory listenerContainerFactory) throws Exception {
        return new JmsMessageDrivenChannelAdapterFactory(listenerContainerFactory,
                messageRecoverer,
                queueNameResolver());
    }

    @Bean
    @ConditionalOnMissingBean(JmsSendingMessageHandlerFactory.class)
    public JmsSendingMessageHandlerFactory jmsSendingMessageHandlerFactory(BeanFactory beanFactory) throws Exception {
        return new JmsSendingMessageHandlerFactory(jmsTemplate(), beanFactory, new DefaultJmsHeaderMapper());
    }

    @Bean
    @ConditionalOnMissingBean(JmsTemplate.class)
    public JmsTemplate jmsTemplate() throws Exception {
        return new JmsTemplate(connectionFactory);
    }


    @Configuration
    public static class JmsBinderConfiguration {

        @Bean
        JMSMessageChannelBinder jmsMessageChannelBinder(QueueProvisioner queueProvisioner,
                                                        Codec codec,
                                                        JmsMessageDrivenChannelAdapterFactory jmsMessageDrivenChannelAdapterFactory,
                                                        DestinationNameResolver destinationNameResolver,
                                                        JmsSendingMessageHandlerFactory jmsSendingMessageHandlerFactory) throws Exception {

            JMSMessageChannelBinder jmsMessageChannelBinder = new JMSMessageChannelBinder(
                    queueProvisioner,
                    destinationNameResolver,
                    jmsSendingMessageHandlerFactory,
                    jmsMessageDrivenChannelAdapterFactory
            );
            jmsMessageChannelBinder.setCodec(codec);
            return jmsMessageChannelBinder;
        }

    }

}
