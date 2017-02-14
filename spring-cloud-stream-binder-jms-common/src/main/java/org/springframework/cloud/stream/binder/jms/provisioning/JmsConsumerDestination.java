package org.springframework.cloud.stream.binder.jms.provisioning;

import javax.jms.JMSException;
import javax.jms.Queue;

import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningException;
import org.springframework.jms.support.JmsUtils;

/**
 * @author Donovan Muller
 */
public class JmsConsumerDestination implements ConsumerDestination {

	private final Queue queue;

	public JmsConsumerDestination(final Queue queue) {
		this.queue = queue;
	}

	@Override
	public String getName() {
		try {
			return this.queue.getQueueName();
		}
		catch (JMSException e) {
			throw new ProvisioningException("Error getting queue name",
					JmsUtils.convertJmsAccessException(e));
		}
	}

	@Override
	public String toString() {
		return "JmsConsumerDestination{" + "queue=" + queue + '}';
	}
}
