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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.cloud.stream.binder.jms.utils.Base64UrlNamingStrategy;
import org.springframework.cloud.stream.binder.jms.utils.DestinationNameResolver;
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
		target = new ActiveMQQueueProvisioner(activeMQConnectionFactory,
				new DestinationNameResolver(new Base64UrlNamingStrategy("anonymous.")));
	}

	@Test
	public void provisionTopicAndConsumerGroup_whenSingleGroup_createsInfrastructure() throws Exception {
		ProducerDestination producerDestination = target.provisionProducerDestination("topic", new ProducerProperties());
		ConsumerDestination consumerDestination = target.provisionConsumerDestination("topic", "group1", new ConsumerProperties());

		String dest = producerDestination.getProducerDestinationName();
		DestinationResolver destinationResolver = jmsTemplate.getDestinationResolver();
		Session session = activeMQConnectionFactory.createConnection().createSession(true, 1);
		Topic topic = (Topic)destinationResolver.resolveDestinationName(session, dest, true);

		jmsTemplate.convertAndSend(topic, "hi jms scs");
		Queue queue = null;

		try {
			destinationResolver = jmsTemplate.getDestinationResolver();
			session = activeMQConnectionFactory.createConnection().createSession(true, 1);
			queue = (Queue)destinationResolver.resolveDestinationName(session, consumerDestination.getConsumerDestinationName(), false);
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

		ProducerDestination producerDestination = target.provisionProducerDestination("topic", new ProducerProperties());
		ConsumerDestination consumerDestination1 = target.provisionConsumerDestination("topic", "group1", new ConsumerProperties());
		ConsumerDestination consumerDestination2 = target.provisionConsumerDestination("topic", "group2", new ConsumerProperties());

		String dest = producerDestination.getProducerDestinationName();
		DestinationResolver destinationResolver = jmsTemplate.getDestinationResolver();
		Session session = activeMQConnectionFactory.createConnection().createSession(true, 1);
		Topic topic = (Topic)destinationResolver.resolveDestinationName(session, dest, true);

		jmsTemplate.convertAndSend(topic, "hi groups");

		Queue queue1 = null;
		Queue queue2 = null;

		try {
			destinationResolver = jmsTemplate.getDestinationResolver();
			session = activeMQConnectionFactory.createConnection().createSession(true, 1);
			queue1 = (Queue)destinationResolver.resolveDestinationName(session, consumerDestination1.getConsumerDestinationName(), false);
			queue2 = (Queue)destinationResolver.resolveDestinationName(session, consumerDestination2.getConsumerDestinationName(), false);
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

	private class CountingListener implements MessageListener {
		private final CountDownLatch latch;

		private final List<Exception> errors = new ArrayList<>();

		private final List<String> payloads = new ArrayList<>();
		private final List<Message> messages = new ArrayList<>();

		public CountingListener(CountDownLatch latch) {
			this.latch = latch;
		}

		public CountingListener(int expectedMessages) {
			this.latch = new CountDownLatch(expectedMessages);
		}

		@Override
		public void onMessage(Message message) {
			if (message instanceof StreamMessage) {
				try {
					payloads.add(((StreamMessage)message).readString());
				} catch (JMSException e) {
					errors.add(e);
				}
			}
			else {
				payloads.add(message.toString());
			}

			messages.add(message);
			latch.countDown();
		}

		boolean awaitExpectedMessages() throws InterruptedException {
			return awaitExpectedMessages(2000);
		}

		boolean awaitExpectedMessages(int timeout) throws InterruptedException {
			return latch.await(timeout, TimeUnit.MILLISECONDS);
		}
		public List<Exception> getErrors() {
			return errors;
		}

		public List<String> getPayloads() {
			return payloads;
		}

		public List<Message> getMessages() {
			return messages;
		}


	}
}
