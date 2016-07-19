package org.springframework.cloud.stream.binder.jms;

public interface QueueProvisioner {

    /**
     *
     * @param topicName
     * @param consumerGroupName
     */
    void provisionTopicAndConsumerGroup(String topicName, String...consumerGroupName);
}
