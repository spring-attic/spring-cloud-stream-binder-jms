package org.springframework.cloud.stream.binder.jms.activemq.integration;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.cloud.stream.binder.jms.activemq.ActiveMQQueueProvisioner;
import org.springframework.cloud.stream.binder.jms.test.ActiveMQTestUtils;

/**
 * @author José Carlos Valero
 * @since 1.1
 */
public class EndToEndIntegrationTests extends org.springframework.cloud.stream.binder.test.integration.EndToEndIntegrationTests {
	private static ActiveMQConnectionFactory connectionFactory;

	static {
		try {
			connectionFactory = ActiveMQTestUtils.startEmbeddedActiveMQServer();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public EndToEndIntegrationTests() throws Exception {
		super(
				new ActiveMQQueueProvisioner(connectionFactory),
				connectionFactory
		);
	}

}
