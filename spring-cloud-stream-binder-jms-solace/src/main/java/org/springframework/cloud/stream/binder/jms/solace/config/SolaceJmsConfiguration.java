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

package org.springframework.cloud.stream.binder.jms.solace.config;

import java.util.Hashtable;
import javax.jms.ConnectionFactory;

import com.solacesystems.jms.SolConnectionFactoryImpl;
import com.solacesystems.jms.property.JMSProperties;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JndiConnectionFactoryAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.binder.jms.spi.QueueProvisioner;
import org.springframework.cloud.stream.binder.jms.solace.SolaceQueueProvisioner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Solace specific configuration.
 *
 * Creates the connection factory and the infrastructure provisioner.
 *
 * @author Jack Galilee
 * @author Jonathan Sharpe
 * @author Joseph Taylor
 * @author José Carlos Valero
 * @since 1.1
 */
@Configuration
@AutoConfigureBefore(JmsAutoConfiguration.class)
@AutoConfigureAfter({JndiConnectionFactoryAutoConfiguration.class})
@ConditionalOnClass({ConnectionFactory.class, SolConnectionFactoryImpl.class})
@ConditionalOnMissingBean(ConnectionFactory.class)
@EnableConfigurationProperties(SolaceConfigurationProperties.class)
public class SolaceJmsConfiguration {

    @ConditionalOnMissingBean(ConnectionFactory.class)
    @Bean
    public ConnectionFactory connectionFactory(SolaceConfigurationProperties config) throws Exception {
        JMSProperties properties = new JMSProperties((Hashtable<?, ?>) null);
        SolConnectionFactoryImpl solConnectionFactory = new SolConnectionFactoryImpl(properties);
        solConnectionFactory.setProperty("Host", config.getHost());
        solConnectionFactory.setProperty("Username", config.getUsername());
        solConnectionFactory.setProperty("Password", config.getPassword());
        //Disabling direct transport allows JMS to use transacted sessions. Enabling at the same time
        //DLQ routing if maxRedeliveryAttempts is set
        solConnectionFactory.setDirectTransport(false);
        return solConnectionFactory;
    }

    @Bean
    public QueueProvisioner solaceQueueProvisioner(SolaceConfigurationProperties solaceConfigurationProperties) {
        return new SolaceQueueProvisioner(solaceConfigurationProperties);
    }

}
