package org.springframework.cloud.stream.binder.jms.provisioning;

import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Topic;

import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningException;
import org.springframework.jms.support.JmsUtils;

/**
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
