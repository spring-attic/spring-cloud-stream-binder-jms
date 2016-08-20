package org.springframework.cloud.stream.binder.jms.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.cloud.stream.binder.jms.spi.QueueProvisioner;
import org.springframework.cloud.stream.binder.jms.test.ActiveMQTestUtils;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.*;
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

    private static JmsTemplate jmsTemplate;
    private static ActiveMQQueueProvisioner target;
    private static ActiveMQConnectionFactory activeMQConnectionFactory;

    @BeforeClass
    public static void initTests() throws Exception {
        activeMQConnectionFactory = ActiveMQTestUtils.startEmbeddedActiveMQServer();
        jmsTemplate = new JmsTemplate(activeMQConnectionFactory);
    }

    @Before
    public void setUp() throws Exception {
        target = new ActiveMQQueueProvisioner(activeMQConnectionFactory);
    }

    @Test
    public void provisionTopicAndConsumerGroup_whenSingleGroup_createsInfrastructure() throws Exception {
        QueueProvisioner.Destinations destinations = target.provisionTopicAndConsumerGroup("topic", "group1");
        Destination group = destinations.getGroups()[0];
        Destination topic = destinations.getTopic();

        jmsTemplate.convertAndSend(topic, "hi jms scs");
        Object payloadGroup1 = jmsTemplate.receiveAndConvert(group);

        assertThat(payloadGroup1).isEqualTo("hi jms scs");
    }

    @Test
    public void provisionTopicAndConsumerGroup_whenMultipleGroups_createsInfrastructure() throws Exception {
        QueueProvisioner.Destinations destinations = target.provisionTopicAndConsumerGroup("topic", "group1", "group2");
        Destination group1 = destinations.getGroups()[0];
        Destination group2 = destinations.getGroups()[1];
        Destination topic = destinations.getTopic();

        jmsTemplate.convertAndSend(topic, "hi groups");
        Object payloadGroup1 = jmsTemplate.receiveAndConvert(group1);
        Object payloadGroup2 = jmsTemplate.receiveAndConvert(group2);

        assertThat(payloadGroup1).isEqualTo("hi groups");
        assertThat(payloadGroup2).isEqualTo("hi groups");
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