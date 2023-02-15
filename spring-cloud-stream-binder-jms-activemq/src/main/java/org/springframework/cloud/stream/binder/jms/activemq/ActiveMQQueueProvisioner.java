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

import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.jms.config.JmsConsumerProperties;
import org.springframework.cloud.stream.binder.jms.config.JmsProducerProperties;
import org.springframework.cloud.stream.binder.jms.provisioning.JmsConsumerDestination;
import org.springframework.cloud.stream.binder.jms.provisioning.JmsProducerDestination;
import org.springframework.cloud.stream.binder.jms.utils.AnonymousNamingStrategy;
import org.springframework.cloud.stream.binder.jms.utils.Base64UrlNamingStrategy;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.SessionCallback;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * <p>
 * Implementation of the {@link ProvisioningProvider} for Apache ActiveMQ.
 * This provider will provision and set up necessary infrastructure
 * (topics, queues, subscriptions etc) to support Spring Cloud Stream's "Grouping"
 * and "Partitioning" within the capabilities available in AciveMQ.
 * </p>
 * <p>
 * Grouping and Partitioning will be combining two approaches
 * <ul>
 * <li>
 * 1. Partitioning is supported by topic-per-partition approach
 * </li>
 * <li>
 * 2. Grouping is supported using Virtual Topic feature provided by ActiveMQ
 *    (see <a href="http://activemq.apache.org/virtual-destinations.html">Virtual Topic</a>)
 * </li>
 * </ul>
 * For example; Let's say you have 2 partitions and 2 groups ("grp1" and "grp2") for destination
 * "myDest". In this case this provisioner will create
 * <pre>
 * Two topics:
 * 		- VirtualTopic.myDest-1
 * 		- VirtualTopic.myDest-2
 * And 4 consumers
 * 		- Consumer.grp1.VirtualTopic.myDest-1
 * 		- Consumer.grp2.VirtualTopic.myDest-1
 * 		- Consumer.grp1.VirtualTopic.myDest-2
 * 		- Consumer.grp2.VirtualTopic.myDest-2
 * </pre>
 * It will also create all necessary subscriptions required to enable internal delegation from virtual
 * topics to queues.
 * </p>
 *
 * @author José Carlos Valero
 * @author Ilayaperumal Gopinathan
 * @author Donovan Muller
 * @author Oleg Zhurakousky
 * @since 1.1
 */
public class ActiveMQQueueProvisioner implements
		ProvisioningProvider<ExtendedConsumerProperties<JmsConsumerProperties>, ExtendedProducerProperties<JmsProducerProperties>> {

	private static final AnonymousNamingStrategy ANONYMOUS_GROUP_NAME_GENERATOR = new Base64UrlNamingStrategy("anonymous");

	private final JmsTemplate jmsTemplate;

	public ActiveMQQueueProvisioner(ActiveMQConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "'connectionFactory' must not be null");
		this.jmsTemplate = new JmsTemplate(connectionFactory);
		this.jmsTemplate.setSessionAcknowledgeMode(Session.AUTO_ACKNOWLEDGE);
	}

	/**
	 * Will provision producer's topic(s) as well as consumer queues per each
	 * required group (if any).
	 * This operation will also establish active delegate subscriptions between
	 * producer's topic(s) and queues for each required
	 * group (see {@link #createDelegationSubscription(String, String)}
	 * An active delegate subscription essentially means creation of a JMS subscriber
	 * between virtual topic and physical queue from which consumers will be consuming
	 * messages and is an ActiveMQ mechanism to establish PubSub consumer groups
	 * (see http://activemq.apache.org/virtual-destinations.html).
	 */
	@Override
	public ProducerDestination provisionProducerDestination(String topicName, ExtendedProducerProperties<JmsProducerProperties> producerProperties) {
		String baseTopicName = this.generateVirtualTopicName(topicName);
		String[] virtualTopicNames = new String[producerProperties.getPartitionCount()];
		if (producerProperties.isPartitioned()) {
			for (int i = 0; i < producerProperties.getPartitionCount(); i++) {
				virtualTopicNames[i] = baseTopicName + "-" + i;
			}
		}
		else {
			virtualTopicNames[0] = baseTopicName;
		}

		String[] requiredGroups = producerProperties.getRequiredGroups() == null ? new String[]{} : producerProperties.getRequiredGroups();
		List<Topic> partitionTopics = new ArrayList<>();
		for (String virtualTopicName : virtualTopicNames) {
			for (String requiredGroupName : requiredGroups) {
				// create subscription for each required group
				this.createDelegationSubscription(requiredGroupName, virtualTopicName);
			}
			partitionTopics.add(this.provisionTopic(virtualTopicName));
		}

		return new JmsProducerDestination(partitionTopics);
	}

	@Override
	public ConsumerDestination provisionConsumerDestination(String topicName, String groupName, ExtendedConsumerProperties<JmsConsumerProperties> properties) {
		if (!StringUtils.hasText(groupName)){
			groupName = ANONYMOUS_GROUP_NAME_GENERATOR.generateName();
		}
		if (properties.isPartitioned()){
			topicName += "-" + properties.getInstanceIndex();
  		}

		Queue queue = this.createDelegationSubscription(groupName, this.generateVirtualTopicName(topicName));

		return new JmsConsumerDestination(queue);
	}

	/**
	 * Creates a delegating subscription between 'virtual topic' and 'queue'
	 * from which messages are going to be consumed.
	 * This is required by AcyiveMQ broker to setup the actual active subscription
	 * which will forward messages from virtual topic to queue.
	 * For more details please see http://activemq.apache.org/virtual-destinations.html and
	 * https://github.com/apache/activemq/blob/master/activemq-unit-tests/src/test/java/org/apache/activemq/broker/virtual/VirtualTopicSelectorTest.java
	 */
	private Queue createDelegationSubscription(final String groupName, final String topicName){
		return this.jmsTemplate.execute(new SessionCallback<Queue>() {
			@Override
			public Queue doInJms(Session session) throws JMSException {
				String queueName = "Consumer." + groupName + "." + topicName;
				Queue queue = new ActiveMQQueue(queueName);
				session.createConsumer(queue);
				return queue;
			}
		});
	}

	private Topic provisionTopic(final String topicName) {
		return this.jmsTemplate.execute(new SessionCallback<Topic>() {
			@Override
			public Topic doInJms(Session session) throws JMSException {
				return session.createTopic(topicName);
			}
		});
	}

	private String generateVirtualTopicName(String topicName){
		return "VirtualTopic." + topicName;
	}
}
