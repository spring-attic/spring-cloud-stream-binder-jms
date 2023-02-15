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

package org.springframework.cloud.stream.binder.jms.activemq;

import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.jms.config.JmsConsumerProperties;
import org.springframework.cloud.stream.binder.jms.config.JmsProducerProperties;
import org.springframework.cloud.stream.binder.jms.test.ActiveMQTestUtils;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.destination.DestinationResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author José Carlos Valero
 * @since 1.1
 */
public class ActiveMQQueueProvisionerIntegrationTest {

	private static JmsTemplate jmsTemplate;
	private static ActiveMQQueueProvisioner target;
	private static ActiveMQConnectionFactory activeMQConnectionFactory;

	@BeforeClass
	public static void initTests() throws Exception {
		activeMQConnectionFactory = ActiveMQTestUtils.startEmbeddedActiveMQServer();
		jmsTemplate = new JmsTemplate(activeMQConnectionFactory);
	}

	@Before
	public void setUp() throws Exception {
		target = new ActiveMQQueueProvisioner(activeMQConnectionFactory);
	}

	@Test
	public void provisionTopicAndConsumerGroup_whenSingleGroup_createsInfrastructure() throws Exception {
		ProducerDestination producerDestination = target.provisionProducerDestination("topic", new ExtendedProducerProperties<>(new JmsProducerProperties()));
		ConsumerDestination consumerDestination = target.provisionConsumerDestination("topic", "group1", new ExtendedConsumerProperties<>(new JmsConsumerProperties()));

		String dest = producerDestination.getName();
		DestinationResolver destinationResolver = jmsTemplate.getDestinationResolver();
		Session session = activeMQConnectionFactory.createConnection().createSession(true, 1);
		Topic topic = (Topic)destinationResolver.resolveDestinationName(session, dest, true);

		jmsTemplate.convertAndSend(topic, "hi jms scs");
		Queue queue = null;

		try {
			destinationResolver = jmsTemplate.getDestinationResolver();
			session = activeMQConnectionFactory.createConnection().createSession(true, 1);
			queue = (Queue)destinationResolver.resolveDestinationName(session, consumerDestination.getName(), false);
			}
		catch (Exception e) {
			//TODO
			e.printStackTrace();
		}
		Object payloadGroup1 = jmsTemplate.receiveAndConvert(queue);

		assertThat(payloadGroup1).isEqualTo("hi jms scs");
	}

	@Test
	public void provisionTopicAndConsumerGroup_whenMultipleGroups_createsInfrastructure() throws Exception {

		ProducerDestination producerDestination = target.provisionProducerDestination("topic", new ExtendedProducerProperties<>(new JmsProducerProperties()));
		ConsumerDestination consumerDestination1 = target.provisionConsumerDestination("topic", "group1", new ExtendedConsumerProperties<>(new JmsConsumerProperties()));
		ConsumerDestination consumerDestination2 = target.provisionConsumerDestination("topic", "group2", new ExtendedConsumerProperties<>(new JmsConsumerProperties()));

		String dest = producerDestination.getName();
		DestinationResolver destinationResolver = jmsTemplate.getDestinationResolver();
		Session session = activeMQConnectionFactory.createConnection().createSession(true, 1);
		Topic topic = (Topic)destinationResolver.resolveDestinationName(session, dest, true);

		jmsTemplate.convertAndSend(topic, "hi groups");

		Queue queue1 = null;
		Queue queue2 = null;

		try {
			destinationResolver = jmsTemplate.getDestinationResolver();
			session = activeMQConnectionFactory.createConnection().createSession(true, 1);
			queue1 = (Queue)destinationResolver.resolveDestinationName(session, consumerDestination1.getName(), false);
			queue2 = (Queue)destinationResolver.resolveDestinationName(session, consumerDestination2.getName(), false);
		}
		catch (Exception e) {
			//TODO
			e.printStackTrace();
		}

		Object payloadGroup1 = jmsTemplate.receiveAndConvert(queue1);
		Object payloadGroup2 = jmsTemplate.receiveAndConvert(queue2);

		assertThat(payloadGroup1).isEqualTo("hi groups");
		assertThat(payloadGroup2).isEqualTo("hi groups");
	}
}
