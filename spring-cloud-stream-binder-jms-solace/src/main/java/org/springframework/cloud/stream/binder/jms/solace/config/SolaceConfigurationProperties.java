package org.springframework.cloud.stream.binder.jms.solace.config;


import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@ConfigurationProperties(prefix = "spring.solace")
public class SolaceConfigurationProperties {

    /**
     * Host of solace server e.g. 192.168.99.100
     */
    private String host;

    /**
     * Username, e.g. admin
     */
    private String username;

    /**
     * Password, e.g. admin
     */
    private String password;

    /**
     * Solace maximum redelivery attempts. If specified, once a message has exceeded the
     * specified amount of re-deliveries, it will be automatically sent to the DLQ by the
     * router.
     *
     * 0 means retry forever. For any value greater than 0 a DLQ named "#DEAD_MSG_QUEUE"
     * will be automatically created per message VPN.
     *
     * NOTE: This is complementary to spring.cloud.stream.bindings.input.consumer.maxAttempts
     * since the second provides redelivery behaviour at the application level. A good
     * configuration set might be:
     *
     *      spring.cloud.stream.bindings.input.consumer.maxAttempts=3
     *      spring.solace.maxRedeliveryAttempts=1
     *
     * If "dead letters" are handled in the application or:
     *
     *      spring.cloud.stream.bindings.input.consumer.maxAttempts=1
     *      spring.solace.maxRedeliveryAttempts=3
     *
     * If you prefer them to be handled in the router.
     */
    @Max(255)
    @Min(0)
    private Integer maxRedeliveryAttempts = 1;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getMaxRedeliveryAttempts() {
        return maxRedeliveryAttempts;
    }

    public void setMaxRedeliveryAttempts(Integer maxRedeliveryAttempts) {
        this.maxRedeliveryAttempts = maxRedeliveryAttempts;
    }
}
