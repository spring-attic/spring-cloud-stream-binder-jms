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

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

import com.google.common.collect.Iterables;
import com.solacesystems.jcsmp.*;
import com.solacesystems.jcsmp.transaction.TransactedSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.stream.binder.jms.solace.SolaceTestUtils.CountingListener;
import org.springframework.cloud.stream.binder.jms.solace.SolaceTestUtils.FailingListener;
import org.springframework.cloud.stream.binder.jms.solace.config.SolaceConfigurationProperties;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

public class SolaceQueueProvisionerIntegrationTests {

    private SolaceQueueProvisioner solaceQueueProvisioner;
    private JCSMPSession session;
    private XMLMessageProducer messageProducer;
    private Topic topic;
    private SolaceConfigurationProperties solaceConfigurationProperties = new SolaceConfigurationProperties();

    @Before
    public void setUp() throws Exception {
        solaceConfigurationProperties.setMaxRedeliveryAttempts(null);
        this.solaceQueueProvisioner = new SolaceQueueProvisioner(solaceConfigurationProperties);
        this.session = SolaceTestUtils.createSession();
        this.messageProducer = session.getMessageProducer(new MessageProducerVoidEventHandler());
        this.topic = JCSMPFactory.onlyInstance().createTopic(getRandomName("topic"));
    }

    @After
    public void tearDown() throws Exception {
        SolaceTestUtils.deprovisionDLQ();
    }

    @Test
    public void provision_whenSingleMessageAndSingleConsumer_shouldReceiveTheMessage() throws Exception {
        String consumerGroupName = getRandomName("consumerGroup");
        solaceQueueProvisioner.provisionTopicAndConsumerGroup(topic.getName(), consumerGroupName);

        messageProducer.send(createMessage("hello jimmy"), topic);
        CountingListener countingListener = listenToQueue(consumerGroupName);

        countingListener.awaitExpectedMessages();

        assertThat(countingListener.getErrors(), empty());
        assertThat(countingListener.getPayloads(), contains("hello jimmy"));
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
        assertThat(countingListener.getPayloads(), empty());
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
        assertThat(countingListener.getPayloads(), contains("hello jimmy"));

        assertThat(countingListener2.getErrors(), empty());
        assertThat(countingListener2.getPayloads(), contains("hello jimmy"));
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
                    } catch (JCSMPException e) {
                        throw new RuntimeException(e);
                    }
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

        JCSMPSession session = SolaceTestUtils.createSession();
        JCSMPSession session2 = SolaceTestUtils.createSession();

        FlowReceiver consumer = session.createFlow(countingListener, consumerFlowProperties);
        FlowReceiver consumer2 = session2.createFlow(countingListener2, consumerFlowProperties2);

        consumer.start();
        consumer2.start();


        latch.await();

        assertThat(countingListener.getErrors(), empty());
        assertThat(countingListener2.getErrors(), empty());

        assertThat("We missed some messages!", Iterables.concat(countingListener.getPayloads(), countingListener2.getPayloads()), iterableWithSize(numberOfMessages));

        assertThat("listener one got all the messages!", countingListener.getPayloads(), iterableWithSize(lessThan(numberOfMessages)));
        assertThat("listener two got all the messages!", countingListener2.getPayloads(), iterableWithSize(lessThan(numberOfMessages)));

        System.out.println(String.format("boomba! %d %d", countingListener.getPayloads().size(), countingListener2.getPayloads().size()));
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
        assertThat(countingListener.getPayloads(), contains("message one", "message two"));

        assertThat(countingListener2.getErrors(), empty());
        assertThat(countingListener2.getPayloads(), contains("message two"));
    }

    @Test
    public void provision_whenMaxRetryAttemptsGreaterThan0_shouldSetMRAInQueueAndProvisionDMQ() throws Exception {
        solaceConfigurationProperties.setMaxRedeliveryAttempts(1);
        TransactedSession transactedSession = session.createTransactedSession();
        String consumerGroupName = getRandomName("consumerGroup");

        solaceQueueProvisioner.provisionDeadLetterQueue();
        solaceQueueProvisioner.provisionTopicAndConsumerGroup(topic.getName(), consumerGroupName);

        messageProducer.send(createMessage("hello jimmy"), topic);
        consumeAndThrowException(consumerGroupName, transactedSession);

        String messagePayload = awaitUntilDMQHasAMessage();

        assertThat(messagePayload, is("hello jimmy"));
    }

    @Test
    public void provisionDLQ_createsANativeSolaceDLQ() throws Exception {
        String DEATH_LETTER = "I got a letter this morning";

        String deadLetterQueue = solaceQueueProvisioner.provisionDeadLetterQueue();

        //createQueue creates a local reference to the queue, the actual queue has to exist
        messageProducer.send(createMessage(DEATH_LETTER), SolaceTestUtils.DLQ);
        CountingListener countingListener = listenToQueue(SolaceTestUtils.DLQ_NAME);
        countingListener.awaitExpectedMessages();

        assertThat(countingListener.getPayloads().size(), is(1));
        assertThat(countingListener.getPayloads().get(0), is(DEATH_LETTER));
        assertThat(deadLetterQueue, is(SolaceTestUtils.DLQ_NAME));

    }

    private String awaitUntilDMQHasAMessage() throws JCSMPException, InterruptedException {
        CountingListener countingListener = listenToQueue("#DEAD_MSG_QUEUE");

        countingListener.awaitExpectedMessages();

        return countingListener.getPayloads().get(0);
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
        message.writeBytes(userData.getBytes());
        message.writeAttachment("i am an attachment".getBytes());
        return message;
    }

    private String getRandomName(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
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