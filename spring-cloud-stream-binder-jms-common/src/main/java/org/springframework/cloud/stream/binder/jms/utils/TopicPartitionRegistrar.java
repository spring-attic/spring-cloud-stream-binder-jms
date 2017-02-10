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

package org.springframework.cloud.stream.binder.jms.utils;

import java.util.concurrent.ConcurrentHashMap;
import javax.jms.Topic;

import org.apache.commons.lang.math.NumberUtils;

/**
 * @author José Carlos Valero
 * @since 20/08/16
 */
public class TopicPartitionRegistrar {
	private static final Integer DEFAULT_TOPIC = -1;
	private final ConcurrentHashMap<Integer, Topic> destinations;

	public TopicPartitionRegistrar() {
		destinations = new ConcurrentHashMap<>(10);
	}

	public void addDestination(Integer partition, Topic topic){
		if(partition != null){
			this.destinations.put(partition, topic);
		}else{
			this.destinations.put(DEFAULT_TOPIC, topic);
		}
	}

	public Topic getDestination(Object partition){
		if (partition == null){
			return getNonPartitionedDestination();
		}
		if (partition instanceof Integer) {
			return this.destinations.get(partition);
		}
		if (partition instanceof String) {
			return this.destinations.get(Integer.parseInt((String) partition));
		}
		if (NumberUtils.isDigits(partition.toString())) {
			return this.destinations.get(Integer.parseInt(partition.toString()));
		}
		throw new IllegalArgumentException(String.format("The provided partition '%s' is not a valid format", partition));
	}

	private Topic getNonPartitionedDestination(){
		return this.destinations.get(DEFAULT_TOPIC);
	}

}
