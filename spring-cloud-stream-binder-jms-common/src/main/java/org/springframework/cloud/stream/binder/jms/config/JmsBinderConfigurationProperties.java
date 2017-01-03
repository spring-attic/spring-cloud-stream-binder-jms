package org.springframework.cloud.stream.binder.jms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Donovan Muller
 */
@ConfigurationProperties("spring.cloud.stream.binders.jms")
public class JmsBinderConfigurationProperties {

    private boolean transacted = true;

    public boolean isTransacted() {
        return transacted;
    }

    public void setTransacted(boolean transacted) {
        this.transacted = transacted;
    }
}
