/*
 *  Copyright 2002-2017 the original author or authors.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.cloud.stream.binder.jms.utils.DestinationNameResolver;
import org.springframework.cloud.stream.binder.jms.utils.DestinationNames;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningException;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;
import org.springframework.jms.support.JmsUtils;

/**
 * {@link ProvisioningProvider} for ActiveMQ.
 *
 * @author José Carlos Valero
 * @since 1.1
 */
public class ActiveMQQueueProvisioner implements
		ProvisioningProvider<ConsumerProperties, ProducerProperties> {

	public static final String ACTIVE_MQ_DLQ = "ActiveMQ.DLQ";

	private final Log logger = LogFactory.getLog(getClass());
	private final ActiveMQConnectionFactory connectionFactory;

	private final DestinationNameResolver destinationNameResolver;

	public ActiveMQQueueProvisioner(ActiveMQConnectionFactory connectionFactory, DestinationNameResolver destinationNameResolver) {
		this.connectionFactory = connectionFactory;
		this.destinationNameResolver = destinationNameResolver;
	}

	@Override
	public ProducerDestination provisionProducerDestination(final String name, ProducerProperties properties) {

		Collection<DestinationNames> topicAndQueueNames =
				this.destinationNameResolver.resolveTopicAndQueueNameForRequiredGroups(name, properties);

		final Map<Integer, Topic> partitionTopics = new HashMap<>();

		for (DestinationNames destinationNames : topicAndQueueNames) {
			Topic topic = provisionTopic(destinationNames.getTopicName());
			provisionConsumerGroup(destinationNames.getTopicName(),
					destinationNames.getGroupNames());

			if (destinationNames.getPartitionIndex() != null) {
				partitionTopics.put(destinationNames.getPartitionIndex(), topic);
			}
			else {
				partitionTopics.put(-1, topic);
			}
		}
		return new JmsProducerDestination(partitionTopics);
	}

	@Override
	public ConsumerDestination provisionConsumerDestination(String name, String group, ConsumerProperties properties) {
		String groupName = this.destinationNameResolver.resolveQueueNameForInputGroup(group, properties);
		String topicName = this.destinationNameResolver.resolveQueueNameForInputGroup(name, properties);

		provisionTopic(topicName);
		final Queue queue = provisionConsumerGroup(topicName, groupName);

		//DLQ
		Session session;
		Connection connection;
		try {
			connection = connectionFactory.createConnection();
			session = connection.createSession(true, 1);
			session.createQueue(ACTIVE_MQ_DLQ);
		}
		catch (JMSException e) {
			throw new ProvisioningException("Provisioning failed", JmsUtils.convertJmsAccessException(e));
		}
		try {
			JmsUtils.commitIfNecessary(session);
		}
		catch (JMSException e) {
			throw new ProvisioningException("Provisioning failed", JmsUtils.convertJmsAccessException(e));
		}
		finally {
			JmsUtils.closeSession(session);
			JmsUtils.closeConnection(connection);
		}
		return new JmsConsumerDestination(queue);
	}

	private Topic provisionTopic(String topicName) {
		Connection activeMQConnection;
		Session session;
		Topic topic = null;
		try {
			activeMQConnection = connectionFactory.createConnection();
			session = activeMQConnection.createSession(true, Session.CLIENT_ACKNOWLEDGE);
			topic = session.createTopic(String.format("VirtualTopic.%s", topicName));

			JmsUtils.commitIfNecessary(session);
			JmsUtils.closeSession(session);
			JmsUtils.closeConnection(activeMQConnection);
		}
		catch (JMSException e) {
			throw new IllegalStateException(e);
		}
		return topic;
	}

	private Queue provisionConsumerGroup(String topicName, String... consumerGroupName) {
		Connection activeMQConnection;
		Session session;
		Queue[] groups = null;
		try {
			activeMQConnection = connectionFactory.createConnection();
			session = activeMQConnection.createSession(true, Session.CLIENT_ACKNOWLEDGE);
			if (ArrayUtils.isNotEmpty(consumerGroupName)) {
				groups = new Queue[consumerGroupName.length];
				for (int i = 0; i < consumerGroupName.length; i++) {
					/*
					 * By default, ActiveMQ consumer queues are named 'Consumer.*.VirtualTopic.',
					 * therefore we must remove '.' from the consumer group name if present.
					 * For example, anonymous consumer groups are named 'anonymous.*' by default.
					 */
					groups[i] = createQueue(topicName, session, consumerGroupName[i].replaceAll("\\.", "_"));
				}
			}

			JmsUtils.commitIfNecessary(session);
			JmsUtils.closeSession(session);
			JmsUtils.closeConnection(activeMQConnection);
			if (groups != null) {
				return groups[0];
			}
		}
		catch (JMSException e) {
			throw new IllegalStateException(e);
		}
		return null;
	}

	private Queue createQueue(String topicName, Session session, String consumerName) throws JMSException {
		Queue queue = session.createQueue(String.format("Consumer.%s.VirtualTopic.%s", consumerName, topicName));
		//TODO: Understand why a producer is required to actually create the queue, it's not mentioned in ActiveMQ docs
		session.createProducer(queue).close();
		return queue;
	}

	private final class JmsProducerDestination implements ProducerDestination {

		private final Map<Integer, Topic> partitionTopics;

		private JmsProducerDestination(Map<Integer, Topic> partitionTopics) {
			this.partitionTopics = partitionTopics;
		}

		@Override
		public String getName() {
			try {
				return partitionTopics.get(-1).getTopicName();
			}
			catch (JMSException e) {
				throw new ProvisioningException("Error getting topic name", JmsUtils.convertJmsAccessException(e));
			}
		}

		@Override
		public String getNameForPartition(int partition) {
			try {
				return partitionTopics.get(partition).getTopicName();
			}
			catch (JMSException e) {
				throw new ProvisioningException("Error getting topic name", JmsUtils.convertJmsAccessException(e));
			}
		}

		@Override
		public String toString() {
			return "JmsProducerDestination{" +
					"partitionTopics=" + partitionTopics +
					'}';
		}
	}

	private final class JmsConsumerDestination implements ConsumerDestination {

		private final Queue queue;

		private JmsConsumerDestination(final Queue queue) {
			this.queue = queue;
		}

		@Override
		public String getName() {
			try {
				return this.queue.getQueueName();
			}
			catch (JMSException e) {
				throw new ProvisioningException("Error getting queue name", JmsUtils.convertJmsAccessException(e));
			}
		}

		@Override
		public String toString() {
			return "JmsConsumerDestination{" +
					"queue=" + queue +
					'}';
		}
	}
}
