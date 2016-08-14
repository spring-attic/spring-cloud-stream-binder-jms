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

package org.springframework.cloud.stream.binder.jms.solace;

import com.solacesystems.jcsmp.*;
import com.solacesystems.jcsmp.impl.DurableTopicEndpointImpl;
import org.apache.commons.lang.ArrayUtils;

import org.springframework.cloud.stream.binder.jms.spi.QueueProvisioner;
import org.springframework.cloud.stream.binder.jms.solace.config.SolaceConfigurationProperties;

/**
 * {@link QueueProvisioner} for Solace.
 *
 * @author Jack Galilee
 * @author Jonathan Sharpe
 * @author Joseph Taylor
 * @author José Carlos Valero
 * @since 1.1
 */
public class SolaceQueueProvisioner implements QueueProvisioner {
    private static final String DMQ_NAME = "#DEAD_MSG_QUEUE";
    private final SessionFactory sessionFactory;

    private SolaceConfigurationProperties solaceConfigurationProperties;

    public SolaceQueueProvisioner(SolaceConfigurationProperties solaceConfigurationProperties) {
        this.solaceConfigurationProperties = solaceConfigurationProperties;
        this.sessionFactory = new SessionFactory(solaceConfigurationProperties);
    }

    @Override
    public Destinations provisionTopicAndConsumerGroup(String name, String... groups) {
        if (ArrayUtils.isEmpty(groups)) return null;//TODO: return JMS Destinations

        try {
            Topic topic = JCSMPFactory.onlyInstance().createTopic(name);
            JCSMPSession session = sessionFactory.build();

            // Using Durable... because non-durable Solace TopicEndpoints don't have names
            TopicEndpoint topicEndpoint = new DurableTopicEndpointImpl(name);
            session.provision(topicEndpoint, null, JCSMPSession.FLAG_IGNORE_ALREADY_EXISTS);

            for (String group : groups) {
                doProvision(session, topic, group);
            }

        } catch (JCSMPErrorResponseException e) {
            if (JCSMPErrorResponseSubcodeEx.SUBSCRIPTION_ALREADY_PRESENT != e.getSubcodeEx()) {
                throw new RuntimeException(e);
            }
        } catch (JCSMPException e) {
            throw new RuntimeException(e);
        }
        return null;//TODO: return JMS Destinations
    }

    @Override
    public String provisionDeadLetterQueue() {
        EndpointProperties properties = new EndpointProperties();
        properties.setPermission(EndpointProperties.PERMISSION_DELETE);
        properties.setAccessType(EndpointProperties.ACCESSTYPE_EXCLUSIVE);

        Queue deadMsgQueue = JCSMPFactory.onlyInstance().createQueue(DMQ_NAME);

        try {
            sessionFactory.build().provision(deadMsgQueue, properties, JCSMPSession.FLAG_IGNORE_ALREADY_EXISTS);
        } catch (JCSMPException e) {
            throw new RuntimeException(e);
        }
        return DMQ_NAME;
    }

    private void doProvision(JCSMPSession session, Topic topic, String group) throws JCSMPException {
        if (group != null) {

            Queue addedQueue = JCSMPFactory.onlyInstance().createQueue(group);

            EndpointProperties endpointProperties = new EndpointProperties();
            endpointProperties.setPermission(EndpointProperties.PERMISSION_DELETE);
            endpointProperties.setAccessType(EndpointProperties.ACCESSTYPE_NONEXCLUSIVE);

            Integer maxRedeliveryAttempts = solaceConfigurationProperties.getMaxRedeliveryAttempts();
            if (maxRedeliveryAttempts != null && maxRedeliveryAttempts >= 0) {
                endpointProperties.setMaxMsgRedelivery(maxRedeliveryAttempts);
            }

            endpointProperties.setQuota(100);

            session.provision(addedQueue, endpointProperties, JCSMPSession.FLAG_IGNORE_ALREADY_EXISTS);
            session.addSubscription(addedQueue, topic, JCSMPSession.WAIT_FOR_CONFIRM);
        }
    }

    protected static class SessionFactory {
        private SolaceConfigurationProperties properties;

        public SessionFactory(SolaceConfigurationProperties properties) {
            this.properties = properties;
        }

        public JCSMPSession build() throws InvalidPropertiesException {
            JCSMPProperties sessionProperties = new JCSMPProperties();
            sessionProperties.setProperty("host", properties.getHost());
            sessionProperties.setProperty("username", properties.getUsername());
            sessionProperties.setProperty("password", properties.getPassword());

            return JCSMPFactory.onlyInstance().createSession(sessionProperties);
        }
    }
}