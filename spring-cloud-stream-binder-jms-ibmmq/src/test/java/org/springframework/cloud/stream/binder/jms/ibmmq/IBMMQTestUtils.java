package org.springframework.cloud.stream.binder.jms.ibmmq;

import java.util.Map;

import javax.jms.ConnectionFactory;

import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.cloud.stream.binder.jms.ibmmq.config.IBMMQConfigurationProperties;
import org.springframework.cloud.stream.binder.jms.ibmmq.config.IBMMQJmsConfiguration;
import org.springframework.core.io.ClassPathResource;

import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFMessageAgent;

public class IBMMQTestUtils {

	public static final String APPLICATION_YML = "application.yml";

	public static final String DLQ_NAME = "DLQ";

	@SuppressWarnings("unchecked")
	public static IBMMQConfigurationProperties getIBMMQProperties() throws Exception {
		YamlMapFactoryBean factoryBean = new YamlMapFactoryBean();
		factoryBean.setResources(new ClassPathResource(APPLICATION_YML));

		Map<String, Object> mapObject = factoryBean.getObject();
		Map<String, Object> ibmMQPropertyMap = (Map<String, Object>) mapObject
				.get("ibmmq");

		IBMMQConfigurationProperties solaceConfigurationProperties = new IBMMQConfigurationProperties();
		solaceConfigurationProperties.setHost((String) ibmMQPropertyMap.get("host"));
		solaceConfigurationProperties.setPort((Integer) ibmMQPropertyMap.get("port"));
		solaceConfigurationProperties
				.setUsername((String) ibmMQPropertyMap.get("username"));
		solaceConfigurationProperties
				.setPassword((String) ibmMQPropertyMap.get("password"));
		solaceConfigurationProperties
				.setQueueManager((String) ibmMQPropertyMap.get("queueManager"));
		solaceConfigurationProperties
				.setChannel((String) ibmMQPropertyMap.get("channel"));

		return solaceConfigurationProperties;
	}

	public static ConnectionFactory createConnectionFactory() throws Exception {
		return new IBMMQJmsConfiguration(getIBMMQProperties())
				.connectionFactory(getIBMMQProperties());
	}

	public static void deprovisionDLQ() throws Exception {
		MQQueueManager queueManager = new MQQueueManager(
				getIBMMQProperties().getQueueManager());
		PCFMessageAgent pcfMessageAgent = new PCFMessageAgent(queueManager);

		try {
			PCFMessage request = new PCFMessage(MQConstants.MQCMD_CLEAR_Q);
			request.addParameter(MQConstants.MQCA_Q_NAME, DLQ_NAME);
			pcfMessageAgent.send(request);

			request = new PCFMessage(MQConstants.MQCMD_DELETE_Q);
			request.addParameter(MQConstants.MQCA_Q_NAME, DLQ_NAME);
			pcfMessageAgent.send(request);
		}
		catch (MQException e) {
			if (e.getReason() != 2085) {
				throw new RuntimeException("Cannot deprovision DLQ", e);
			}
		}

		pcfMessageAgent.disconnect();
		queueManager.disconnect();
	}
}
