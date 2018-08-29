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

package org.springframework.cloud.stream.binder.jms.config;

import javax.jms.ConnectionFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.jms.JMSMessageChannelBinder;
import org.springframework.cloud.stream.binder.jms.utils.Base64UrlNamingStrategy;
import org.springframework.cloud.stream.binder.jms.utils.DestinationNameResolver;
import org.springframework.cloud.stream.binder.jms.utils.JmsMessageDrivenEndpointFactory;
import org.springframework.cloud.stream.binder.jms.utils.JmsSendingMessageHandlerFactory;
import org.springframework.cloud.stream.binder.jms.utils.ListenerContainerFactory;
import org.springframework.cloud.stream.binder.jms.utils.MessageRecoverer;
import org.springframework.cloud.stream.binder.jms.utils.RepublishMessageRecoverer;
import org.springframework.cloud.stream.binder.jms.utils.SpecCompliantJmsHeaderMapper;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.jms.JmsHeaderMapper;
import org.springframework.jms.core.JmsTemplate;

/**
 * Configuration class containing required beans in order to set up the JMS
 * binder.
 *
 * @author Jack Galilee
 * @author Jonathan Sharpe
 * @author Joseph Taylor
 * @author José Carlos Valero
 * @author Donovan Muller
 * @author Gary Russell
 * @author Ilayaperumal Gopinathan
 * @author Tim Ysewyn
 * @since 1.1
 */
@Configuration
public class JmsBinderGlobalConfiguration {

	@Autowired
	private ConnectionFactory connectionFactory;

	@Bean
	public DestinationNameResolver queueNameResolver() {
		return new DestinationNameResolver(new Base64UrlNamingStrategy("anonymous."));
	}

	@Bean
	@ConditionalOnMissingBean
	MessageRecoverer defaultMessageRecoverer(JmsTemplate jmsTemplate, JmsHeaderMapper jmsHeaderMapper) {
		return new RepublishMessageRecoverer(jmsTemplate, jmsHeaderMapper);
	}

	@Bean
	ListenerContainerFactory listenerContainerFactory() {
		return new ListenerContainerFactory(this.connectionFactory);
	}

	@Bean
	public JmsMessageDrivenEndpointFactory jmsMessageDrivenChannelAdapterFactory(
			MessageRecoverer messageRecoverer, ListenerContainerFactory listenerContainerFactory, JmsHeaderMapper jmsHeaderMapper) {
		return new JmsMessageDrivenEndpointFactory(listenerContainerFactory, messageRecoverer, jmsHeaderMapper);
	}

	@Bean
	@ConditionalOnMissingBean
	public JmsSendingMessageHandlerFactory jmsSendingMessageHandlerFactory(JmsTemplate jmsTemplate, JmsHeaderMapper jmsHeaderMapper) {
		return new JmsSendingMessageHandlerFactory(jmsTemplate, jmsHeaderMapper);
	}

	@Bean
	@ConditionalOnMissingBean
	public JmsHeaderMapper jmsHeaderMapper() {
		return new SpecCompliantJmsHeaderMapper();
	}

	@Configuration
	@EnableConfigurationProperties(JmsExtendedBindingProperties.class)
	public static class JmsBinderConfiguration {

		@Autowired
		private ProvisioningProvider<ExtendedConsumerProperties<JmsConsumerProperties>, ExtendedProducerProperties<JmsProducerProperties>> provisioningProvider;

		@Autowired
		private ConnectionFactory connectionFactory;

		@Autowired
		private JmsExtendedBindingProperties jmsExtendedBindingProperties;

		@Bean
		JMSMessageChannelBinder jmsMessageChannelBinder(
				JmsMessageDrivenEndpointFactory jmsMessageDrivenEndpointFactory,
				JmsSendingMessageHandlerFactory jmsSendingMessageHandlerFactory, JmsTemplate jmsTemplate) {

			JMSMessageChannelBinder jmsMessageChannelBinder = new JMSMessageChannelBinder(this.provisioningProvider,
					jmsSendingMessageHandlerFactory, jmsMessageDrivenEndpointFactory, jmsTemplate, this.connectionFactory);
			jmsMessageChannelBinder.setExtendedBindingProperties(this.jmsExtendedBindingProperties);
			return jmsMessageChannelBinder;
		}

	}

}
