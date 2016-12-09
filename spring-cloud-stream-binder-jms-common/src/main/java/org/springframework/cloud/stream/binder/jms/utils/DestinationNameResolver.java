/*
 *  Copyright 2002-2016 the original author or authors.
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

package org.springframework.cloud.stream.binder.jms.utils;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.util.StringUtils;

/**
 * Component responsible of building up the name of a specific queue given some context
 *
 * @author José Carlos Valero
 * @author Donovan Muller
 * @since 1.1
 */
public class DestinationNameResolver {

	private AnonymousNamingStrategy namingStrategy;

	public DestinationNameResolver(AnonymousNamingStrategy namingStrategy) {
		this.namingStrategy = namingStrategy;
	}

	public String resolveQueueNameForInputGroup(String group,
			ConsumerProperties properties) {
		boolean anonymous = !StringUtils.hasText(group);
		String baseQueueName = anonymous ? namingStrategy.generateName() : group;
		return properties.isPartitioned()
				? buildName(properties.getInstanceIndex(), baseQueueName) : baseQueueName;
	}

	public Collection<DestinationNames> resolveTopicAndQueueNameForRequiredGroups(
			String topic, ProducerProperties properties) {
		Collection<DestinationNames> output = new ArrayList<>(
				properties.getPartitionCount());
		if (properties.isPartitioned()) {
			String[] requiredGroups = properties.getRequiredGroups();
			for (int index = 0; index < properties.getPartitionCount(); index++) {
				String[] requiredPartitionGroupNames = new String[properties
						.getRequiredGroups().length];
				for (int j = 0; j < requiredGroups.length; j++) {
					requiredPartitionGroupNames[j] = buildName(index, requiredGroups[j]);
				}
				String topicName = buildName(index, topic);
				output.add(new DestinationNames(topicName, requiredPartitionGroupNames,
						index));
			}
		}
		else {
			output.add(new DestinationNames(topic, properties.getRequiredGroups()));
		}

		return output;
	}

	private String buildName(int index, String group) {
		return String.format("%s-%s", group, index);
	}

}
