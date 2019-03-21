/*
 *  Copyright 2002-2017 the original author or authors.
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

package org.springframework.cloud.stream.binder.jms.utils;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author José Carlos Valero
 * @since 20/08/16
 */
public class DestinationNames {

	private final String topicName;
	private final String[] groupNames;
	private final Integer partitionIndex;

	public DestinationNames(String topicName, String[] groupNames, int partitionIndex) {
		this.topicName = topicName;
		this.groupNames = groupNames;
		this.partitionIndex = partitionIndex;
	}

	public DestinationNames(String topicName, String[] groupNames) {
		this.topicName = topicName;
		this.groupNames = groupNames;
		partitionIndex = null;
	}

	public String getTopicName() {
		return topicName;
	}

	public String[] getGroupNames() {
		return groupNames;
	}

	public Integer getPartitionIndex() {
		return partitionIndex;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DestinationNames that = (DestinationNames) o;
		return Objects.equals(topicName, that.topicName) &&
				Arrays.equals(groupNames, that.groupNames) &&
				Objects.equals(partitionIndex, that.partitionIndex);
	}

	@Override
	public int hashCode() {
		return Objects.hash(topicName, groupNames, partitionIndex);
	}

	@Override
	public String toString() {
		return "DestinationNames{" +
				"topicName='" + topicName + '\'' +
				", groupNames=" + Arrays.toString(groupNames) +
				", partitionIndex=" + partitionIndex +
				'}';
	}
}
