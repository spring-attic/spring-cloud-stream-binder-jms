package org.springframework.cloud.stream.binder.jms.solace;

import com.solacesystems.jcsmp.*;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.impl.DurableTopicEndpointImpl;
import org.springframework.cloud.stream.binder.jms.QueueProvisioner;

import static com.solacesystems.jcsmp.JCSMPErrorResponseSubcodeEx.SUBSCRIPTION_ALREADY_PRESENT;

public class SolaceQueueProvisioner implements QueueProvisioner {

    @Override
    public void provisionTopicAndConsumerGroup(String name, String group) {
        if (group == null) return;

        try {

            Topic topic = JCSMPFactory.onlyInstance().createTopic(name);
            JCSMPSession session = new SessionFactory().build();
            TopicEndpoint topicEndpoint = new DurableTopicEndpointImpl(name);
            session.provision(topicEndpoint, null, JCSMPSession.FLAG_IGNORE_ALREADY_EXISTS);

            if (group != null) {

                Queue addedQueue = JCSMPFactory.onlyInstance().createQueue(group);

                EndpointProperties endpointProperties = new EndpointProperties();
                endpointProperties.setPermission(EndpointProperties.PERMISSION_DELETE);
                endpointProperties.setAccessType(EndpointProperties.ACCESSTYPE_EXCLUSIVE);
                endpointProperties.setQuota(100);

                session.provision(addedQueue, endpointProperties, JCSMPSession.FLAG_IGNORE_ALREADY_EXISTS);
                session.addSubscription(addedQueue, topic, JCSMPSession.WAIT_FOR_CONFIRM);
            }
        } catch (JCSMPErrorResponseException e) {
            if (SUBSCRIPTION_ALREADY_PRESENT != e.getSubcodeEx()) {
                throw new RuntimeException(e);
            }
        } catch (JCSMPException e) {
            throw new RuntimeException(e);
        }
    }

    private class SessionFactory {
        public JCSMPSession build() throws InvalidPropertiesException {
            JCSMPProperties sessionProperties = new JCSMPProperties();
            sessionProperties.setProperty("host", "192.168.99.100");
            sessionProperties.setProperty("username", "admin");
            sessionProperties.setProperty("password", "admin");

            return JCSMPFactory.onlyInstance().createSession(sessionProperties);
        }
    }
}