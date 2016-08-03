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

package org.springframework.cloud.stream.binder.jms.solace.config;


import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
/**
 * Configuration properties for Solace specific features.
 *
 * @author Jack Galilee
 * @author Jonathan Sharpe
 * @author Joseph Taylor
 * @author José Carlos Valero
 * @since 1.1
 */
@ConfigurationProperties(prefix = "spring.solace")
public class SolaceConfigurationProperties {

    /** Host of solace server e.g. 192.168.99.100 */
    private String host;

    /** Username, e.g. admin */
    private String username;

    /** Password, e.g. admin */
    private String password;

    /**
     * Solace maximum redelivery attempts. If specified, once a message has exceeded the
     * specified amount of re-deliveries, it will be automatically sent to the DLQ by the
     * broker.
     *
     * 0 means retry forever, which can cause Poison Messages. 1 means re-try once, which
     * implies up to 2 delivery attempts. For any value greater than 0 a DLQ named
     * "#DEAD_MSG_QUEUE" will be automatically created per message VPN.
     *
     * NOTE: This is complementary to spring.cloud.stream.bindings.[your-input].consumer.maxAttempts
     * since the second provides redelivery behaviour at the JMS binder level. Setting
     * maxRedeliveryAttempts flag to some value higher than 0 grants that a poison message will eventually
     * appear in the DLQ even if it can not be fetched by Spring Cloud Stream.
     *
     * Both can be safely configured. If the JMS binder is set to retry, it will consume
     * the message and send it directly to the DMQ after a failure scenario. If the message
     * cannot be delivered to the binder (e.g. corrupt message) the broker itself will
     * route it to the DMQ after maxRedeliveryAttempts.
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
