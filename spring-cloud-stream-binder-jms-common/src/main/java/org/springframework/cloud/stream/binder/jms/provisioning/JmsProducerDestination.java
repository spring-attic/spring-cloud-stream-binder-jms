/*
 *  Copyright 2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.stream.binder.jms.provisioning;

import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Topic;

import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningException;
import org.springframework.jms.support.JmsUtils;

/**
 * An implementation of {@link ProducerDestination} for JMS.
 *
 * @author Donovan Muller
 */
public class JmsProducerDestination implements ProducerDestination {

	private final Map<Integer, Topic> partitionTopics;

    public JmsProducerDestination(Map<Integer, Topic> partitionTopics) {
		this.partitionTopics = partitionTopics;
	}

	@Override
	public String getName() {
		try {
			return partitionTopics.get(-1).getTopicName();
		}
		catch (JMSException e) {
			throw new ProvisioningException("Error getting topic name",
					JmsUtils.convertJmsAccessException(e));
		}
	}

	@Override
	public String getNameForPartition(int partition) {
		try {
			return partitionTopics.get(partition).getTopicName();
		}
		catch (JMSException e) {
			throw new ProvisioningException("Error getting topic name",
					JmsUtils.convertJmsAccessException(e));
		}
	}

	@Override
	public String toString() {
		return "JmsProducerDestination{" + "partitionTopics=" + partitionTopics + '}';
	}
}
