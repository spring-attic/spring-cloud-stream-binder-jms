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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.ProducerProperties;

/**
 * Component responsible of building up the name of a specific queue given some context
 *
 * @author José Carlos Valero
 * @since 1.1
 */
public class QueueNameResolver {

    public String resolveQueueNameForInputGroup(String group,
                                                ConsumerProperties properties) {
        return properties.isPartitioned() ? buildName(properties.getInstanceIndex(),
                group) : group;
    }

    public Map<String, String[]> resolveQueueNameForRequiredGroups(String topic,
                                                                   ProducerProperties properties) {
        Map<String, String[]> output = new HashMap<>(properties.getPartitionCount());
        if (properties.isPartitioned()) {
            IntStream.range(0, properties.getPartitionCount()).forEach(index -> {
                String[] requiredPartitionGroupNames = Arrays.stream(properties.getRequiredGroups())
                        .map(group -> buildName(index, group))
                        .toArray(size -> new String[size]);
                String topicName = buildName(index, topic);
                output.put(topicName, requiredPartitionGroupNames);
            });
        }else {
            output.put(topic, properties.getRequiredGroups());
        }
        return output;
    }

    private String buildName(int index, String group) {
        return String.format("%s-%s", group, index);
    }

}
