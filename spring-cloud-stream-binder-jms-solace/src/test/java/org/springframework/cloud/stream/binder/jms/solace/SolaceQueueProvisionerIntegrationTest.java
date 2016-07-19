package org.springframework.cloud.stream.binder.jms.solace;

import com.solacesystems.jcsmp.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.*;

public class SolaceQueueProvisionerIntegrationTest {

    /**
     * As discussed in slack, topics do not persist messages, so messages sent to topics without consumers
     * will never be seen (Required Groups can specified to provision consumer at the same time as the topic).
     * @throws Exception
     */
    @Test
    public void whenTopicProvisionedWithoutConsumers_itShouldDiscardMessages() throws Exception {
        JCSMPSession session = createSession();

        //provision just the topic
        SolaceQueueProvisioner solaceQueueProvisioner = new SolaceQueueProvisioner();
        String topicName = getRandomName("topic");
        solaceQueueProvisioner.provisionTopicAndConsumerGroup(topicName, null);

        //send some messages to the topic

        BytesXMLMessage message = JCSMPFactory.onlyInstance().createMessage(BytesXMLMessage.class);
        message.setDeliveryMode(DeliveryMode.PERSISTENT);

        message.setUserData("hello jimmy".getBytes());
        message.writeAttachment("i am an attachment".getBytes());
        XMLMessageProducer messageProducer = session.getMessageProducer(new PrintingPubCallback());

        Topic topic = JCSMPFactory.onlyInstance().createTopic(topicName);
        messageProducer.send(message, topic);

        //now create the consumer group
        String consumerGroupName = getRandomName("consumerGroup");
        solaceQueueProvisioner.provisionTopicAndConsumerGroup(topicName, consumerGroupName);

        //now try to read some messages
        Queue queue = JCSMPFactory.onlyInstance().createQueue(consumerGroupName);

        ConsumerFlowProperties consumerFlowProperties = new ConsumerFlowProperties();
        consumerFlowProperties.setEndpoint(queue);

        CountingListener countingListener = new CountingListener();
        FlowReceiver consumer = session.createFlow(countingListener, consumerFlowProperties);
        consumer.start();

        Thread.sleep(2000);
        assertThat(countingListener.getErrors(), empty());
        assertThat(countingListener.getMessages(), empty());
    }

    @Test
    public void whenASecondSubscriptionIsAdded_itGetsSubsequentMessages() throws Exception {
        SolaceQueueProvisioner solaceQueueProvisioner = new SolaceQueueProvisioner();
        String topicName = getRandomName("topic");

        String consumerGroup1Name = getRandomName("consumerGroup1");
        solaceQueueProvisioner.provisionTopicAndConsumerGroup(topicName, consumerGroup1Name);

        JCSMPSession session = createSession();

        BytesXMLMessage message = JCSMPFactory.onlyInstance().createMessage(BytesXMLMessage.class);
        message.setDeliveryMode(DeliveryMode.PERSISTENT);

        message.setUserData("message one".getBytes());
        message.writeAttachment("i am an attachment".getBytes());
        XMLMessageProducer messageProducer = session.getMessageProducer(new PrintingPubCallback());

        Topic topic = JCSMPFactory.onlyInstance().createTopic(topicName);
        messageProducer.send(message, topic);

        Queue queue = JCSMPFactory.onlyInstance().createQueue(consumerGroup1Name);

        ConsumerFlowProperties consumerFlowProperties = new ConsumerFlowProperties();
        consumerFlowProperties.setEndpoint(queue);

        CountingListener countingListener = new CountingListener();
        FlowReceiver consumer = session.createFlow(countingListener, consumerFlowProperties);
        consumer.start();

        String consumerGroup2Name = getRandomName("consumerGroup2");
        solaceQueueProvisioner.provisionTopicAndConsumerGroup(topicName, consumerGroup2Name);

        BytesXMLMessage messageTwo = JCSMPFactory.onlyInstance().createMessage(BytesXMLMessage.class);
        messageTwo.setDeliveryMode(DeliveryMode.PERSISTENT);

        messageTwo.setUserData("message two".getBytes());
        messageTwo.writeAttachment("i am an attachment".getBytes());
        messageProducer.send(messageTwo, topic);

        Queue queue2 = JCSMPFactory.onlyInstance().createQueue(consumerGroup2Name);

        ConsumerFlowProperties consumerFlow2Properties = new ConsumerFlowProperties();
        consumerFlow2Properties.setEndpoint(queue2);

        CountingListener countingListener2 = new CountingListener();
        FlowReceiver consumer2 = session.createFlow(countingListener2, consumerFlow2Properties);
        consumer2.start();

        Thread.sleep(2000);
        assertThat(countingListener.getErrors(), empty());
        assertThat(countingListener.getMessages(), contains("message one", "message two"));

        assertThat(countingListener2.getErrors(), empty());
        assertThat(countingListener2.getMessages(), contains("message two"));
    }

    @Test
    public void whenMultipleSubscriptionsArePresent_allGroupsReceiveAllMessages() throws Exception {
        SolaceQueueProvisioner solaceQueueProvisioner = new SolaceQueueProvisioner();
        String topicName = getRandomName("topic");
        String consumerGroup1Name = getRandomName("consumerGroup1");
        String consumerGroup2Name = getRandomName("consumerGroup2");

        solaceQueueProvisioner.provisionTopicAndConsumerGroup(topicName, consumerGroup1Name);
        solaceQueueProvisioner.provisionTopicAndConsumerGroup(topicName, consumerGroup2Name);

        JCSMPSession session = createSession();

        BytesXMLMessage message = JCSMPFactory.onlyInstance().createMessage(BytesXMLMessage.class);
        message.setDeliveryMode(DeliveryMode.PERSISTENT);

        message.setUserData("hello jimmy".getBytes());
        message.writeAttachment("i am an attachment".getBytes());
        XMLMessageProducer messageProducer = session.getMessageProducer(new PrintingPubCallback());

        Topic topic = JCSMPFactory.onlyInstance().createTopic(topicName);
        messageProducer.send(message, topic);

        Queue queue = JCSMPFactory.onlyInstance().createQueue(consumerGroup1Name);

        ConsumerFlowProperties consumerFlowProperties = new ConsumerFlowProperties();
        consumerFlowProperties.setEndpoint(queue);

        CountingListener countingListener = new CountingListener();
        FlowReceiver consumer = session.createFlow(countingListener, consumerFlowProperties);
        consumer.start();

        Queue queue2 = JCSMPFactory.onlyInstance().createQueue(consumerGroup2Name);

        ConsumerFlowProperties consumerFlow2Properties = new ConsumerFlowProperties();
        consumerFlow2Properties.setEndpoint(queue2);

        CountingListener countingListener2 = new CountingListener();
        FlowReceiver consumer2 = session.createFlow(countingListener2, consumerFlow2Properties);
        consumer2.start();

        Thread.sleep(2000);
        assertThat(countingListener.getErrors(), empty());
        assertThat(countingListener.getMessages(), contains("hello jimmy"));

        assertThat(countingListener2.getErrors(), empty());
        assertThat(countingListener2.getMessages(), contains("hello jimmy"));
    }

    @Test
    public void provision() throws Exception {
        SolaceQueueProvisioner solaceQueueProvisioner = new SolaceQueueProvisioner();
        String topicName = getRandomName("topic");
        String consumerGroupName = getRandomName("consumerGroup");

        solaceQueueProvisioner.provisionTopicAndConsumerGroup(topicName, consumerGroupName);

        JCSMPSession session = createSession();

        BytesXMLMessage message = JCSMPFactory.onlyInstance().createMessage(BytesXMLMessage.class);
        message.setDeliveryMode(DeliveryMode.PERSISTENT);

        message.setUserData("hello jimmy".getBytes());
        message.writeAttachment("i am an attachment".getBytes());
        XMLMessageProducer messageProducer = session.getMessageProducer(new PrintingPubCallback());

        Topic topic = JCSMPFactory.onlyInstance().createTopic(topicName);
        messageProducer.send(message, topic);

        Queue queue = JCSMPFactory.onlyInstance().createQueue(consumerGroupName);

        ConsumerFlowProperties consumerFlowProperties = new ConsumerFlowProperties();
        consumerFlowProperties.setEndpoint(queue);

        CountingListener countingListener = new CountingListener();
        FlowReceiver consumer = session.createFlow(countingListener, consumerFlowProperties);
        consumer.start();

        Thread.sleep(2000);
        assertThat(countingListener.getErrors(), empty());
        assertThat(countingListener.getMessages(), contains("hello jimmy"));
    }

    private String getRandomName(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private JCSMPSession createSession() throws InvalidPropertiesException {
        JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty("host", "192.168.99.100");
        properties.setProperty("username", "admin");
        properties.setProperty("password", "admin");

        return JCSMPFactory.onlyInstance().createSession(properties);
    }

    private class CountingListener implements XMLMessageListener {
        private List<JCSMPException> errors = new ArrayList<>();

        private List<String> messages = new ArrayList<>();

        @Override
        public void onReceive(BytesXMLMessage bytesXMLMessage) {
            messages.add(new String(bytesXMLMessage.getUserData()));
        }

        @Override
        public void onException(JCSMPException e) {
            errors.add(e);
        }

        List<JCSMPException> getErrors() {
            return errors;
        }

        public List<String> getMessages() {
            return messages;
        }
    }

    private class PrintingPubCallback implements JCSMPStreamingPublishEventHandler {
        public void handleError(String messageID, JCSMPException cause, long timestamp) {
            System.err.println("Error occurred for message: " + messageID);
            cause.printStackTrace();
        }

        // This method is only invoked for persistent and non-persistent
        // messages.
        public void responseReceived(String messageID) {
            System.out.println("Response received for message: " + messageID);
        }
    }
}