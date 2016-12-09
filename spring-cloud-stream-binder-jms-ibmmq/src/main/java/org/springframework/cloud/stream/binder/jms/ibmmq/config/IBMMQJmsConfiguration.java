package org.springframework.cloud.stream.binder.jms.ibmmq.config;

import javax.jms.ConnectionFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jms.JndiConnectionFactoryAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.binder.jms.config.JmsBinderAutoConfiguration;
import org.springframework.cloud.stream.binder.jms.config.JmsBinderConfigurationProperties;
import org.springframework.cloud.stream.binder.jms.ibmmq.IBMMQJmsHeaderMapper;
import org.springframework.cloud.stream.binder.jms.ibmmq.IBMMQQueueProvisioner;
import org.springframework.cloud.stream.binder.jms.spi.QueueProvisioner;
import org.springframework.cloud.stream.binder.jms.utils.JmsSendingMessageHandlerFactory;
import org.springframework.cloud.stream.binder.jms.utils.MessageRecoverer;
import org.springframework.cloud.stream.binder.jms.utils.RepublishMessageRecoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.StringUtils;

import com.ibm.mq.MQEnvironment;
import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;

/**
 * IBM MQ specific configuration.
 *
 * Creates the connection factory and the infrastructure provisioner.
 *
 * @author Donovan Muller
 */
@Configuration
@Import(JmsBinderAutoConfiguration.class)
@AutoConfigureAfter({ JndiConnectionFactoryAutoConfiguration.class })
@EnableConfigurationProperties(IBMMQConfigurationProperties.class)
public class IBMMQJmsConfiguration {

	private IBMMQConfigurationProperties configurationProperties;

	public IBMMQJmsConfiguration(
			final IBMMQConfigurationProperties configurationProperties) {
		this.configurationProperties = configurationProperties;
	}

	@Bean
	public MessageRecoverer defaultMessageRecoverer(QueueProvisioner queueProvisioner,
			JmsTemplate jmsTemplate) throws Exception {
		return new RepublishMessageRecoverer(queueProvisioner, jmsTemplate,
				new IBMMQJmsHeaderMapper());

	}

	@Bean
	public JmsSendingMessageHandlerFactory jmsSendingMessageHandlerFactory(
			JmsTemplate jmsTemplate, BeanFactory beanFactory) throws Exception {
		return new JmsSendingMessageHandlerFactory(jmsTemplate, beanFactory,
				new IBMMQJmsHeaderMapper());
	}

	@ConditionalOnMissingBean(ConnectionFactory.class)
	@Bean
	public MQConnectionFactory connectionFactory(
			IBMMQConfigurationProperties configurationProperties) throws Exception {
		// see http://stackoverflow.com/a/33135633/2408961
		MQEnvironment.hostname = configurationProperties.getHost();
		MQEnvironment.port = configurationProperties.getPort();
		MQEnvironment.channel = configurationProperties.getChannel();

		if (!StringUtils.isEmpty(configurationProperties.getUsername())) {
			MQEnvironment.userID = configurationProperties.getUsername();
			MQEnvironment.password = configurationProperties.getPassword();
		}

		MQConnectionFactory connectionFactory = new MQConnectionFactory();
		connectionFactory.setHostName(configurationProperties.getHost());
		connectionFactory.setPort(configurationProperties.getPort());
		connectionFactory.setQueueManager(configurationProperties.getQueueManager());
		connectionFactory.setChannel(configurationProperties.getChannel());
		connectionFactory.setTransportType(configurationProperties.getTransportType());

		if (!StringUtils.isEmpty(configurationProperties.getUsername())) {
			connectionFactory.setStringProperty(WMQConstants.USERID,
					configurationProperties.getUsername());
			connectionFactory.setStringProperty(WMQConstants.PASSWORD,
					configurationProperties.getPassword());
		}

		return connectionFactory;
	}

	@Bean
	public IBMMQQueueProvisioner ibmMQQueueProvisioner(
			MQConnectionFactory connectionFactory,
			JmsBinderConfigurationProperties binderConfigurationProperties)
			throws Exception {
		return new IBMMQQueueProvisioner(connectionFactory, configurationProperties,
				binderConfigurationProperties);
	}

}
