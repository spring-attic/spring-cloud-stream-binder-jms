/*
 *  Copyright 2002-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.stream.binder.jms.activemq.config;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jms.JndiConnectionFactoryAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.binder.jms.activemq.ActiveMQQueueProvisioner;
import org.springframework.cloud.stream.binder.jms.config.JmsBinderAutoConfiguration;
import org.springframework.cloud.stream.binder.jms.utils.DestinationNameResolver;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * ActiveMQ specific configuration.
 *
 * Creates the connection factory and the infrastructure provisioner.
 *
 * @author José Carlos Valero
 * @since 1.1
 */
@Configuration
//It is important to include the root JMS configuration class.
@Import(JmsBinderAutoConfiguration.class)
@AutoConfigureAfter({JndiConnectionFactoryAutoConfiguration.class})
@ConditionalOnClass({ConnectionFactory.class, ActiveMQConnectionFactory.class})
@EnableConfigurationProperties(ActiveMQConfigurationProperties.class)
public class ActiveMQJmsConfiguration {

	@ConditionalOnMissingBean(ConnectionFactory.class)
	@Bean
	public ActiveMQConnectionFactory connectionFactory(ActiveMQConfigurationProperties config) throws Exception {
		return new ActiveMQConnectionFactory(config.getUsername(), config.getPassword(), config.getHost());
	}

	@Bean
	ProvisioningProvider solaceQueueProvisioner(ActiveMQConnectionFactory connectionFactory,
												DestinationNameResolver destinationNameResolver) {
		return new ActiveMQQueueProvisioner(connectionFactory, destinationNameResolver);
	}

}
