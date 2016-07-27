package org.springframework.cloud.stream.binder.jms.solace;

import com.google.common.collect.Iterables;
import com.solacesystems.jcsmp.*;
import com.solacesystems.jcsmp.DeliveryMode;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.transaction.TransactedSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.stream.binder.jms.solace.config.SolaceConfigurationProperties;

import javax.jms.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.solacesystems.jcsmp.JCSMPSession.FLAG_IGNORE_DOES_NOT_EXIST;
import static com.solacesystems.jcsmp.JCSMPSession.WAIT_FOR_CONFIRM;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.*;

public class SolaceQueueProvisionerIntegrationTest {

    private SolaceQueueProvisioner solaceQueueProvisioner;
    private JCSMPSession session;
    private XMLMessageProducer messageProducer;
    private Topic topic;
    private SolaceConfigurationProperties solaceConfigurationProperties = new SolaceConfigurationProperties();
    public static final String DLQ_NAME = "#DEAD_MSG_QUEUE";
    public static final Queue DLQ = JCSMPFactory.onlyInstance().createQueue(DLQ_NAME);

    @Before
    public void setUp() throws Exception {
        solaceConfigurationProperties.setMaxRedeliveryAttempts(null);
        this.solaceQueueProvisioner = new SolaceQueueProvisioner(solaceConfigurationProperties);
        this.session = createSession();
        this.messageProducer = session.getMessageProducer(new MessageProducerVoidEventHandler());
        this.topic = JCSMPFactory.onlyInstance().createTopic(getRandomName("topic"));
    }

    @After
    public void tearDown() throws Exception {
        session.deprovision(DLQ,WAIT_FOR_CONFIRM | FLAG_IGNORE_DOES_NOT_EXIST);
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
        solaceQueueProvisioner.provisionTopicAndConsumerGroup(topic.getName());

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

        solaceQueueProvisioner.provisionTopicAndConsumerGroup(topic.getName(), consumerGroup1Name, consumerGroup2Name);

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
    public void provision_whenMultipleListenersOnOneQueue_listenersCompeteForMessages() throws Exception {
        int numberOfMessages = 1000;

        String consumerGroupName = getRandomName("consumerGroup");
        solaceQueueProvisioner.provisionTopicAndConsumerGroup(topic.getName(), consumerGroupName);

        IntStream.range(0, numberOfMessages)
                .mapToObj(String::valueOf)
                .map(this::createMessage)
                .forEach(m -> {
                    try {
                        messageProducer.send(m, topic);
                    }
                    catch (JCSMPException e) { throw new RuntimeException(e); }
                });

        Queue queue = JCSMPFactory.onlyInstance().createQueue(consumerGroupName);

        ConsumerFlowProperties consumerFlowProperties = new ConsumerFlowProperties();
        consumerFlowProperties.setEndpoint(queue);

        CountDownLatch latch = new CountDownLatch(numberOfMessages);
        CountingListener countingListener = new CountingListener(latch);

        Queue queue2 = JCSMPFactory.onlyInstance().createQueue(consumerGroupName);
        ConsumerFlowProperties consumerFlowProperties2 = new ConsumerFlowProperties();
        consumerFlowProperties2.setEndpoint(queue2);
        CountingListener countingListener2 = new CountingListener(latch);

        JCSMPSession session = createSession();
        JCSMPSession session2 = createSession();

        FlowReceiver consumer = session.createFlow(countingListener, consumerFlowProperties);
        FlowReceiver consumer2 = session2.createFlow(countingListener2, consumerFlowProperties2);

        consumer.start();
        consumer2.start();


        latch.await();

        assertThat(countingListener.getErrors(), empty());
        assertThat(countingListener2.getErrors(), empty());

        assertThat("We missed some messages!", Iterables.concat(countingListener.getMessages(), countingListener2.getMessages()), iterableWithSize(numberOfMessages));

        assertThat("listener one got all the messages!", countingListener.getMessages(), iterableWithSize(lessThan(numberOfMessages)));
        assertThat("listener two got all the messages!", countingListener2.getMessages(), iterableWithSize(lessThan(numberOfMessages)));

        System.out.println(String.format("boomba! %d %d", countingListener.getMessages().size(), countingListener2.getMessages().size()));
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

    @Test
    public void provision_whenMaxRetryAttemptsGreaterThan0_shouldSetMRAInQueueAndProvisionDMQ() throws Exception {
        solaceConfigurationProperties.setMaxRedeliveryAttempts(1);
        TransactedSession transactedSession = session.createTransactedSession();
        String consumerGroupName = getRandomName("consumerGroup");

        solaceQueueProvisioner.provisionDeadLetterQueue();
        solaceQueueProvisioner.provisionTopicAndConsumerGroup(topic.getName(), consumerGroupName);

        messageProducer.send(createMessage("hello jimmy"), topic);
        consumeAndThrowException(consumerGroupName,transactedSession);

        String messagePayload = awaitUntilDMQHasAMessage();

        assertThat(messagePayload, is("hello jimmy"));
    }

    @Test
    public void provisionDLQ_createsANativeSolaceDLQ() throws Exception {
        String DEATH_LETTER = "I got a letter this morning";

        Optional<String> deadLetterQueue = solaceQueueProvisioner.provisionDeadLetterQueue();

        //createQueue creates a local reference to the queue, the actual queue has to exist
        messageProducer.send(
                createMessage(DEATH_LETTER),
                DLQ);
        CountingListener countingListener = listenToQueue(DLQ_NAME);
        countingListener.awaitExpectedMessages();

        assertThat(countingListener.getMessages().size(), is(1));
        assertThat(countingListener.getMessages().get(0), is(DEATH_LETTER));
        assertThat(deadLetterQueue.get(), is(DLQ_NAME));

    }

    private String awaitUntilDMQHasAMessage() throws JCSMPException, InterruptedException {
        CountingListener countingListener = listenToQueue("#DEAD_MSG_QUEUE");

        countingListener.awaitExpectedMessages();

        return countingListener.getMessages().get(0);
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

    private FailingListener consumeAndThrowException(String queueName, TransactedSession transactedSession) throws JCSMPException {
        Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);

        ConsumerFlowProperties consumerFlowProperties = new ConsumerFlowProperties();
        consumerFlowProperties.setEndpoint(queue);

        FailingListener failingListener = new FailingListener(transactedSession);
        EndpointProperties endpointProperties = new EndpointProperties();
        FlowReceiver consumer = transactedSession.createFlow(failingListener, consumerFlowProperties, endpointProperties);
        consumer.start();
        return failingListener;
    }

    private BytesXMLMessage createMessage(String userData) {
        BytesXMLMessage message = JCSMPFactory.onlyInstance().createMessage(BytesXMLMessage.class);
        message.setDeliveryMode(DeliveryMode.PERSISTENT);
        message.setDMQEligible(true);
        message.setUserData(userData.getBytes());
        message.writeAttachment("i am an attachment".getBytes());
        return message;
    }

    private String getRandomName(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private JCSMPSession createSession() throws InvalidPropertiesException {
        //TODO: Use Spring properties instead
        JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty("username", "admin");
        properties.setProperty("password", "admin");
        properties.setProperty("host", "192.168.99.101");

        return JCSMPFactory.onlyInstance().createSession(properties);
    }

    private class CountingListener implements XMLMessageListener {
        private final CountDownLatch latch;

        private final List<JCSMPException> errors = new ArrayList<>();

        private final List<String> messages = new ArrayList<>();

        private CountingListener(CountDownLatch latch) {
            this.latch = latch;
        }

        private CountingListener(int expectedMessages) {
            this.latch = new CountDownLatch(expectedMessages);
        }

        @Override
        public void onReceive(BytesXMLMessage bytesXMLMessage) {
            messages.add(new String(bytesXMLMessage.getUserData()));

            long count = latch.getCount();

            if(count % 1000 == 0){
                System.out.println(String.format("Message %s", count));
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

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

    private class FailingListener implements XMLMessageListener {

        private TransactedSession transactedSession;

        public FailingListener(TransactedSession transactedSession) {

            this.transactedSession = transactedSession;
        }

        @Override
        public void onReceive(BytesXMLMessage bytesXMLMessage) {
            try {
                transactedSession.rollback();
            } catch (JCSMPException e) {
                e.printStackTrace();
            }
            throw new RuntimeException("You shall not pass");
        }

        @Override
        public void onException(JCSMPException e) {
//            throw new RuntimeException("You shall not pass", e);
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