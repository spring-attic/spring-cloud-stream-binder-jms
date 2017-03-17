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
import org.springframework.cloud.stream.binder.jms.utils.JmsMessageDrivenChannelAdapterFactory;
import org.springframework.cloud.stream.binder.jms.utils.JmsSendingMessageHandlerFactory;
import org.springframework.cloud.stream.binder.jms.utils.ListenerContainerFactory;
import org.springframework.cloud.stream.binder.jms.utils.MessageRecoverer;
import org.springframework.cloud.stream.binder.jms.utils.RepublishMessageRecoverer;
import org.springframework.cloud.stream.binder.jms.utils.SpecCompliantJmsHeaderMapper;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.codec.Codec;
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
 * @since 1.1
 */
@Configuration
public class JmsBinderGlobalConfiguration {

	@Autowired
	private ConnectionFactory connectionFactory;

	@Bean
	public DestinationNameResolver queueNameResolver() throws Exception {
		return new DestinationNameResolver(new Base64UrlNamingStrategy("anonymous."));
	}

	@Bean
	@ConditionalOnMissingBean(MessageRecoverer.class)
	MessageRecoverer defaultMessageRecoverer() throws Exception {
		return new RepublishMessageRecoverer(jmsTemplate(), new SpecCompliantJmsHeaderMapper());
	}

	@Bean
	ListenerContainerFactory listenerContainerFactory() throws Exception {
		return new ListenerContainerFactory(connectionFactory);
	}

	@Bean
	public JmsMessageDrivenChannelAdapterFactory jmsMessageDrivenChannelAdapterFactory(
			MessageRecoverer messageRecoverer, ListenerContainerFactory listenerContainerFactory) throws Exception {
		return new JmsMessageDrivenChannelAdapterFactory(listenerContainerFactory, messageRecoverer);
	}

	@Bean
	@ConditionalOnMissingBean(JmsSendingMessageHandlerFactory.class)
	public JmsSendingMessageHandlerFactory jmsSendingMessageHandlerFactory() throws Exception {
		return new JmsSendingMessageHandlerFactory(jmsTemplate(), new SpecCompliantJmsHeaderMapper());
	}

	@Bean
	@ConditionalOnMissingBean(JmsTemplate.class)
	public JmsTemplate jmsTemplate() throws Exception {
		return new JmsTemplate(connectionFactory);
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
		JMSMessageChannelBinder jmsMessageChannelBinder(Codec codec,
				JmsMessageDrivenChannelAdapterFactory jmsMessageDrivenChannelAdapterFactory,
				JmsSendingMessageHandlerFactory jmsSendingMessageHandlerFactory, JmsTemplate jmsTemplate) throws Exception {

			JMSMessageChannelBinder jmsMessageChannelBinder = new JMSMessageChannelBinder(provisioningProvider,
					jmsSendingMessageHandlerFactory, jmsMessageDrivenChannelAdapterFactory, jmsTemplate, connectionFactory);
			jmsMessageChannelBinder.setCodec(codec);
			jmsMessageChannelBinder.setExtendedBindingProperties(jmsExtendedBindingProperties);
			return jmsMessageChannelBinder;
		}

	}

}
