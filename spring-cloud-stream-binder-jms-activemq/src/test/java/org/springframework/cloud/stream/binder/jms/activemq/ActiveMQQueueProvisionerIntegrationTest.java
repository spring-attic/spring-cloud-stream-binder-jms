package org.springframework.cloud.stream.binder.jms.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.store.kahadb.KahaDBPersistenceAdapter;
import org.junit.Test;
import org.springframework.cloud.stream.binder.jms.spi.QueueProvisioner;
import org.springframework.cloud.stream.binder.jms.test.ActiveMQTestUtils;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author José Carlos Valero
 * @since 1.1
 */
public class ActiveMQQueueProvisionerIntegrationTest {


    @Test
    public void provisionTopicAndConsumerGroup_whenSingleGroup_createsInfrastructure() throws Exception {
        ActiveMQConnectionFactory activeMQConnectionFactory = ActiveMQTestUtils.startEmbeddedActiveMQServer();
        ActiveMQQueueProvisioner target = new ActiveMQQueueProvisioner(activeMQConnectionFactory);
        JmsTemplate jmsTemplate = new JmsTemplate(activeMQConnectionFactory);

        QueueProvisioner.Destinations destinations = target.provisionTopicAndConsumerGroup("topic", "group1");
        Destination group = destinations.getGroups()[0];
        Destination topic = destinations.getTopic();

        jmsTemplate.convertAndSend(topic, "hi activemq");
        CountingListener messageListener = new CountingListener(1);
        activeMQConnectionFactory.createConnection().createSession(true,0).createConsumer(group).setMessageListener(messageListener);
        messageListener.awaitExpectedMessages();

        assertThat(messageListener.getPayloads().get(0)).isEqualTo("hi activemq");
    }

    private class CountingListener implements MessageListener {
        private final CountDownLatch latch;

        private final List<Exception> errors = new ArrayList<>();

        private final List<String> payloads = new ArrayList<>();
        private final List<Message> messages = new ArrayList<>();

        public CountingListener(CountDownLatch latch) {
            this.latch = latch;
        }

        public CountingListener(int expectedMessages) {
            this.latch = new CountDownLatch(expectedMessages);
        }

        @Override
        public void onMessage(Message message) {
            if (message instanceof StreamMessage) {
                try {
                    payloads.add(((StreamMessage)message).readString());
                } catch (JMSException e) {
                    errors.add(e);
                }
            }
            else {
                payloads.add(message.toString());
            }

            messages.add(message);
            latch.countDown();
        }

        boolean awaitExpectedMessages() throws InterruptedException {
            return awaitExpectedMessages(2000);
        }

        boolean awaitExpectedMessages(int timeout) throws InterruptedException {
            return latch.await(timeout, TimeUnit.MILLISECONDS);
        }
        public List<Exception> getErrors() {
            return errors;
        }

        public List<String> getPayloads() {
            return payloads;
        }

        public List<Message> getMessages() {
            return messages;
        }


    }
}