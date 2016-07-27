package org.springframework.cloud.stream.binder.jms;

import java.util.Optional;

public interface QueueProvisioner {

    /**
     *
     * <p>Create a mixed topology (pub/sub - queue). So producers can send messages to
     * the topic and multiple consumers can compete in the different groups for messages.</p>
     *
     * <p>A possibility is to provision one topic, provision n queues (one per group)
     * and bind every queue to the topic so they receive messages from it.</p>
     *
     * @param topicName
     * @param consumerGroupName
     */
    void provisionTopicAndConsumerGroup(String topicName, String... consumerGroupName);

    /**
     * <p>Creates the Dead Letter Queue (DLQ) where messages that cannot be
     * consumed due to consumer failure are eventually sent.</p>
     *
     * <p>Messages will be sent to the DLQ when the maximum number of attempts is met.</p>
     *
     * <p>NOTE: This method is optional, if it is not implemented a regular queue named
     * error.topic.group will be created as long as DLQ support is enabled.</p>
     *
     * <p>NOTE: This method is expected to be idempotent. More than one call should be
     * expected, and it should not create duplicate queues or fail.</p>
     *
     * @return the name of the DLQ if created, empty otherwise.
     */
    default Optional<String> provisionDeadLetterQueue() {
        return Optional.empty();
    }
}
