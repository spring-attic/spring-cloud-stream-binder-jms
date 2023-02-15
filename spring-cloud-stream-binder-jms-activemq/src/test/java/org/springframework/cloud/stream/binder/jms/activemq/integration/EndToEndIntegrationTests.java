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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.cloud.stream.binder.jms.activemq.ActiveMQQueueProvisioner;
import org.springframework.cloud.stream.binder.jms.test.ActiveMQTestUtils;
import org.springframework.cloud.stream.binder.jms.utils.Base64UrlNamingStrategy;
import org.springframework.cloud.stream.binder.jms.utils.DestinationNameResolver;

import javax.jms.ConnectionFactory;

/**
 * @author José Carlos Valero
 * @author Tim Ysewyn
 * @since 1.1
 */
public class EndToEndIntegrationTests extends org.springframework.cloud.stream.binder.test.integration.EndToEndIntegrationTests {

	private static ActiveMQTestUtils activeMQTestUtils;
	private static ConnectionFactory connectionFactory;

	public EndToEndIntegrationTests() {
		super(
				new ActiveMQQueueProvisioner(connectionFactory,
						new DestinationNameResolver(new Base64UrlNamingStrategy("anonymous."))),
				connectionFactory
		);
	}

	@BeforeClass
	public static void setup() throws Exception {
		activeMQTestUtils = new ActiveMQTestUtils();
		connectionFactory = activeMQTestUtils.getConnectionFactory();
	}

	@AfterClass
	public static void teardown() throws Exception {
		activeMQTestUtils.stopEmbeddedActiveMQServer();
	}

}
