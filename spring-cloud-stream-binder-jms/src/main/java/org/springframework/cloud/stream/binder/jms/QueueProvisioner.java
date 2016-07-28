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
     * <p>NOTE: This method is expected to be idempotent. More than one call should be
     * expected, and it should not create duplicate queues or fail.</p>
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
     * <p>NOTE: This method is expected to be idempotent. More than one call should be
     * expected, and it should not create duplicate queues or fail.</p>
     *
     * <p>Your JMS provider might implement native DLQ features, if that is the case, you
     * might prefer to provide configuration capabilities in the specific binder and disable
     * by default retry capabilities of Spring Cloud Stream.</p>
     *
     * <p>On the other hand if your JMS provider treats DLQ as regular queues (e.g. Solace)
     * you might prefer to return that queue to ensure all dead letters end up in the same
     * place.</p>
     *
     * @return the name of the created DLQ.
     */
    String provisionDeadLetterQueue();
}
