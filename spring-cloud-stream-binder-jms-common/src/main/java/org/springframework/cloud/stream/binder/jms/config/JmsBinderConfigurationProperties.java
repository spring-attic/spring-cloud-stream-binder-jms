package org.springframework.cloud.stream.binder.jms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Donovan Muller
 */
@ConfigurationProperties("spring.cloud.stream.binder.jms")
public class JmsBinderConfigurationProperties {

    private String deadLetterQueueName = "dlq";

    public String getDeadLetterQueueName() {
        return deadLetterQueueName;
    }

    public void setDeadLetterQueueName(String deadLetterQueueName) {
        this.deadLetterQueueName = deadLetterQueueName;
    }
}
