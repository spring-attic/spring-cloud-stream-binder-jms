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

import org.junit.Test;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.ProducerProperties;

import java.util.Collection;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author José Carlos Valero
 * @since 1.1
 */
public class DestinationNameResolverTest {

    private DestinationNameResolver target = new DestinationNameResolver();

    @Test
    public void resolveNameForConsumer_whenNotPartitioned_returnsRawGroup() throws Exception {
        ConsumerProperties properties = new ConsumerProperties();
        properties.setPartitioned(false);

        String queueName = target.resolveQueueNameForInputGroup("group", properties);

        assertThat(queueName, is("group"));
    }

    @Test
    public void resolveNameForConsumer_whenPartitioned_returnsGroupWithIndex() throws Exception {
        ConsumerProperties properties = new ConsumerProperties();
        properties.setPartitioned(true);
        properties.setInstanceIndex(33);

        String queueName = target.resolveQueueNameForInputGroup("group", properties);

        assertThat(queueName, is("group-33"));
    }

    @Test
    public void resolveNameForProducer_whenNotPartitioned_returnsRawTopicAndRequiredGroups() throws Exception {
        ProducerProperties properties = new ProducerProperties();
        properties.setPartitionCount(1);
        properties.setRequiredGroups("requiredGroup1","requiredGroup2");

        Collection<DestinationNames> names = target.resolveTopicAndQueueNameForRequiredGroups("topic", properties);

        assertThat(names, hasSize(1));
        assertThat(names, contains(new DestinationNames("topic", new String[]{"requiredGroup1", "requiredGroup2"})));
    }

    @Test
    public void resolveNameForProducer_whenPartitioned_returnsTopicWithIndexAndRequiredGroupsWithIndex() throws Exception {
        ProducerProperties properties = new ProducerProperties();
        properties.setPartitionCount(2);
        properties.setPartitionKeyExtractorClass(Object.class); // Irrelevant at this point, yet necessary
        properties.setRequiredGroups("requiredGroup1","requiredGroup2");

        Collection<DestinationNames> names = target.resolveTopicAndQueueNameForRequiredGroups("topic", properties);

        assertThat(names, hasSize(2));
        assertThat(names, contains(
                new DestinationNames("topic-0", new String[]{"requiredGroup1-0", "requiredGroup2-0"}, 0),
                new DestinationNames("topic-1", new String[]{"requiredGroup1-1", "requiredGroup2-1"}, 1)));
    }
}