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

package org.springframework.cloud.stream.binder.jms;

/**
 * SPI defining vendor-specific provisioning methods to grant compatibility
 * with the JMS binder.
 *
 * @author Jack Galilee
 * @author Jonathan Sharpe
 * @author Joseph Taylor
 * @author José Carlos Valero
 * @since 1.1
 */
public interface QueueProvisioner {

    /**
     * Create a mixed topology (pub/sub - queue) so producers can send messages
     * to a topic and multiple consumers can compete for messages in the
     * different groups.
     * <p>A possibility is to provision one topic, provision n queues (one per
     * group) and bind every queue to the topic so they receive messages from it.
     * <p>NOTE: This method is expected to be idempotent. More than one call
     * should be expected, and it should not create duplicate queues or fail.
     * @param topicName the name of the topic
     * @param consumerGroupName the name of the consumer group
     */
    void provisionTopicAndConsumerGroup(String topicName, String... consumerGroupName);

    /**
     * Creates the Dead Letter Queue (DLQ) where messages that cannot be
     * consumed due to consumer failure are eventually sent.
     * <p>Messages will be sent to the DLQ when the maximum number of attempts
     * is met.
     * <p>NOTE: This method is expected to be idempotent. More than one call
     * should be expected, and it should not create duplicate queues or fail.
     * <p>Your JMS provider might implement native DLQ features, if that is the
     * case, you might prefer to provide configuration capabilities in the
     * specific binder and disable by default retry capabilities of Spring Cloud
     * Stream.
     * <p>On the other hand if your JMS provider treats DLQ as regular queues
     * (e.g. Solace) you might prefer to return that queue to ensure all dead
     * letters end up in the same place.
     * @return the name of the created DLQ.
     */
    String provisionDeadLetterQueue();
}
