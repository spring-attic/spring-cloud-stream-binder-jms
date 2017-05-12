/*
 *  Copyright 2016-2017 the original author or authors.
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
