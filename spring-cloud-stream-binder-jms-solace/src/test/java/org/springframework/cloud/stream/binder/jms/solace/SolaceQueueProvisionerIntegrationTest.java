package org.springframework.cloud.stream.binder.jms.solace;

import com.solacesystems.jcsmp.*;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.*;

public class SolaceQueueProvisionerIntegrationTest {

    private SolaceQueueProvisioner solaceQueueProvisioner;
    private JCSMPSession session;
    private XMLMessageProducer messageProducer;
    private Topic topic;

    @Before
    public void setUp() throws Exception {
        this.solaceQueueProvisioner = new SolaceQueueProvisioner();
        this.session = createSession();
        this.messageProducer = session.getMessageProducer(new MessageProducerVoidEventHandler());
        this.topic = JCSMPFactory.onlyInstance().createTopic(getRandomName("topic"));
    }

    @Test
    public void provision_whenSingleMessageAndSingleConsumer_shouldReceiveTheMessage() throws Exception {
        String consumerGroupName = getRandomName("consumerGroup");
        solaceQueueProvisioner.provisionTopicAndConsumerGroup(topic.getName(), consumerGroupName);

        messageProducer.send(createMessage("hello jimmy"), topic);
        CountingListener countingListener = listenToQueue(consumerGroupName);

        countingListener.awaitExpectedMessages();

        assertThat(countingListener.getErrors(), empty());
        assertThat(countingListener.getMessages(), contains("hello jimmy"));
    }

    /**
     * As discussed in slack, topics do not persist messages, so messages sent to topics without consumers
     * will never be seen (Required Groups can specified to provision consumer at the same time as the topic).
     *
     * @throws Exception
     */
    @Test
    public void provision_whenTopicProvisionedWithoutConsumers_itShouldDiscardMessages() throws Exception {
        //provision just the topic
        solaceQueueProvisioner.provisionTopicAndConsumerGroup(topic.getName(), null);

        messageProducer.send(createMessage("hello jimmy"), topic);

        String consumerGroupName = getRandomName("consumerGroup");
        solaceQueueProvisioner.provisionTopicAndConsumerGroup(topic.getName(), consumerGroupName);

        CountingListener countingListener = listenToQueue(consumerGroupName);

        Thread.sleep(2000); // We assume 2 seconds as a sensible time to be confident no messages will be received.
        assertThat(countingListener.getErrors(), empty());
        assertThat(countingListener.getMessages(), empty());
    }

    @Test
    public void provision_whenMultipleSubscriptionsArePresent_allGroupsReceiveAllMessages() throws Exception {
        String consumerGroup1Name = getRandomName("consumerGroup1");
        String consumerGroup2Name = getRandomName("consumerGroup2");

        solaceQueueProvisioner.provisionTopicAndConsumerGroup(topic.getName(), consumerGroup1Name);
        solaceQueueProvisioner.provisionTopicAndConsumerGroup(topic.getName(), consumerGroup2Name);

        messageProducer.send(createMessage("hello jimmy"), topic);

        CountingListener countingListener = listenToQueue(consumerGroup1Name);
        CountingListener countingListener2 = listenToQueue(consumerGroup2Name);

        countingListener.awaitExpectedMessages();
        countingListener2.awaitExpectedMessages();

        assertThat(countingListener.getErrors(), empty());
        assertThat(countingListener.getMessages(), contains("hello jimmy"));

        assertThat(countingListener2.getErrors(), empty());
        assertThat(countingListener2.getMessages(), contains("hello jimmy"));
    }

    @Test
    public void provision_whenASecondSubscriptionIsAdded_itGetsSubsequentMessages() throws Exception {
        String firstConsumerGroup = getRandomName("consumerGroup1");
        solaceQueueProvisioner.provisionTopicAndConsumerGroup(topic.getName(), firstConsumerGroup);

        messageProducer.send(createMessage("message one"), topic);
        CountingListener countingListener = listenToQueue(firstConsumerGroup, 2);

        String secondConsumerGroup = getRandomName("consumerGroup2");
        solaceQueueProvisioner.provisionTopicAndConsumerGroup(topic.getName(), secondConsumerGroup);

        messageProducer.send(createMessage("message two"), topic);
        CountingListener countingListener2 = listenToQueue(secondConsumerGroup);

        countingListener.awaitExpectedMessages();
        countingListener2.awaitExpectedMessages();

        assertThat(countingListener.getErrors(), empty());
        assertThat(countingListener.getMessages(), contains("message one", "message two"));

        assertThat(countingListener2.getErrors(), empty());
        assertThat(countingListener2.getMessages(), contains("message two"));
    }

    private CountingListener listenToQueue(String queueName) throws JCSMPException {
        return listenToQueue(queueName, 1);
    }

    private CountingListener listenToQueue(String queueName, int expectedMessages) throws JCSMPException {
        Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);

        ConsumerFlowProperties consumerFlowProperties = new ConsumerFlowProperties();
        consumerFlowProperties.setEndpoint(queue);

        CountingListener countingListener = new CountingListener(expectedMessages);
        FlowReceiver consumer = session.createFlow(countingListener, consumerFlowProperties);
        consumer.start();
        return countingListener;
    }

    private BytesXMLMessage createMessage(String userData) {
        BytesXMLMessage message = JCSMPFactory.onlyInstance().createMessage(BytesXMLMessage.class);
        message.setDeliveryMode(DeliveryMode.PERSISTENT);
        message.setUserData(userData.getBytes());
        message.writeAttachment("i am an attachment".getBytes());
        return message;
    }

    private String getRandomName(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private JCSMPSession createSession() throws InvalidPropertiesException {
        JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty("username", "admin");
        properties.setProperty("password", "admin");
        properties.setProperty("host", "192.168.99.100");

        return JCSMPFactory.onlyInstance().createSession(properties);
    }

    private class CountingListener implements XMLMessageListener {
        private final CountDownLatch latch;

        private final List<JCSMPException> errors = new ArrayList<>();

        private final List<String> messages = new ArrayList<>();

        private CountingListener(int expectedMessages) {
            this.latch = new CountDownLatch(expectedMessages);
        }

        @Override
        public void onReceive(BytesXMLMessage bytesXMLMessage) {
            messages.add(new String(bytesXMLMessage.getUserData()));
            latch.countDown();
        }

        @Override
        public void onException(JCSMPException e) {
            errors.add(e);
        }

        void awaitExpectedMessages() throws InterruptedException {
            latch.await(2, TimeUnit.SECONDS);
        }

        List<JCSMPException> getErrors() {
            return errors;
        }

        List<String> getMessages() {
            return messages;
        }
    }

    private class MessageProducerVoidEventHandler implements JCSMPStreamingPublishEventHandler {
        @Override
        public void handleError(String id, JCSMPException e, long timestamp) {
            System.err.println(String.format("Error in message producer for message id: '%s' at timestamp %d", id, timestamp));
            e.printStackTrace();
        }

        public void responseReceived(String messageID) {
            //do nothing
        }
    }
}