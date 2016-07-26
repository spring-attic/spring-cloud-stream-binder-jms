package org.springframework.cloud.stream.binder.jms.solace;

import com.solacesystems.jcsmp.*;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.impl.DurableTopicEndpointImpl;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.cloud.stream.binder.jms.QueueProvisioner;

import java.util.Arrays;

import static com.solacesystems.jcsmp.JCSMPErrorResponseSubcodeEx.SUBSCRIPTION_ALREADY_PRESENT;

public class SolaceQueueProvisioner implements QueueProvisioner {

    @Override
    public void provisionTopicAndConsumerGroup(String name, String...groups) {
        if (ArrayUtils.isEmpty(groups)) return;

        try {

            Topic topic = JCSMPFactory.onlyInstance().createTopic(name);
            JCSMPSession session = new SessionFactory().build();

            //TODO: we don't need the topic to be durable
            TopicEndpoint topicEndpoint = new DurableTopicEndpointImpl(name);
            session.provision(topicEndpoint, null, JCSMPSession.FLAG_IGNORE_ALREADY_EXISTS);

            for(String group : groups){
                doProvision(session, topic, group);
            }

        } catch (JCSMPErrorResponseException e) {
            if (SUBSCRIPTION_ALREADY_PRESENT != e.getSubcodeEx()) {
                throw new RuntimeException(e);
            }
        } catch (JCSMPException e) {
            throw new RuntimeException(e);
        }
    }

    private void doProvision(JCSMPSession session, Topic topic, String group) throws JCSMPException {
        if (group != null) {

            Queue addedQueue = JCSMPFactory.onlyInstance().createQueue(group);

            EndpointProperties endpointProperties = new EndpointProperties();
            endpointProperties.setPermission(EndpointProperties.PERMISSION_DELETE);
            endpointProperties.setAccessType(EndpointProperties.ACCESSTYPE_NONEXCLUSIVE);
            endpointProperties.setQuota(100);

            session.provision(addedQueue, endpointProperties, JCSMPSession.FLAG_IGNORE_ALREADY_EXISTS);
            session.addSubscription(addedQueue, topic, JCSMPSession.WAIT_FOR_CONFIRM);
        }
    }

    private class SessionFactory {
        //TODO: Use spring properties instead
        public JCSMPSession build() throws InvalidPropertiesException {
            JCSMPProperties sessionProperties = new JCSMPProperties();
            sessionProperties.setProperty("host", "192.168.99.101");
            sessionProperties.setProperty("username", "admin");
            sessionProperties.setProperty("password", "admin");

            return JCSMPFactory.onlyInstance().createSession(sessionProperties);
        }
    }
}