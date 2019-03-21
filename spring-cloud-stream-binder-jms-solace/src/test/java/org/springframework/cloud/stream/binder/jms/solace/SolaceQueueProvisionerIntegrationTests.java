/*
 *  Copyright 2002-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.cloud.stream.binder.jms.solace.SolaceTestUtils.RollbackListener;
import org.springframework.cloud.stream.binder.jms.solace.config.SolaceConfigurationProperties;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static org.springframework.cloud.stream.binder.jms.solace.SolaceTestUtils.DLQ_NAME;

public class SolaceQueueProvisionerIntegrationTests {

    public static final String TEXT_CONTENT = "some text content";
    public static final String MORE_TEXT_CONTENT = "some more text content";

    private SolaceQueueProvisioner solaceQueueProvisioner;
    private JCSMPSession session;
    private XMLMessageProducer messageProducer;
    private Topic topic;
    private SolaceConfigurationProperties solaceConfigurationProperties;

    @Before
    public void setUp() throws Exception {
        solaceConfigurationProperties = SolaceTestUtils.getSolaceProperties();

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

        messageProducer.send(createMessage(TEXT_CONTENT), topic);
        CountingListener countingListener = listenToQueue(consumerGroupName);

        countingListener.awaitExpectedMessages();

        assertThat(countingListener.getErrors(), empty());
        assertThat(countingListener.getPayloads(), contains(TEXT_CONTENT));
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

        messageProducer.send(createMessage(TEXT_CONTENT), topic);

        String consumerGroupName = getRandomName("consumerGroup");
        solaceQueueProvisioner.provisionTopicAndConsumerGroup(topic.getName(), consumerGroupName);

        CountingListener countingListener = listenToQueue(consumerGroupName);

        Thread.sleep(500); // We assume 500 milliseconds as a sensible time to be confident no messages will be received.
        assertThat(countingListener.getErrors(), empty());
        assertThat(countingListener.getPayloads(), empty());
    }

    @Test
    public void provision_whenMultipleSubscriptionsArePresent_allGroupsReceiveAllMessages() throws Exception {
        String consumerGroup1Name = getRandomName("consumerGroup1");
        String consumerGroup2Name = getRandomName("consumerGroup2");

        solaceQueueProvisioner.provisionTopicAndConsumerGroup(topic.getName(), consumerGroup1Name, consumerGroup2Name);

        messageProducer.send(createMessage(TEXT_CONTENT), topic);

        CountingListener countingListener = listenToQueue(consumerGroup1Name);
        CountingListener countingListener2 = listenToQueue(consumerGroup2Name);

        countingListener.awaitExpectedMessages();
        countingListener2.awaitExpectedMessages();

        assertThat(countingListener.getErrors(), empty());
        assertThat(countingListener.getPayloads(), contains(TEXT_CONTENT));

        assertThat(countingListener2.getErrors(), empty());
        assertThat(countingListener2.getPayloads(), contains(TEXT_CONTENT));
    }

    @Test
    public void provision_whenMultipleListenersOnOneQueue_listenersCompeteForMessages() throws Exception {
        int numberOfMessages = 200;
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

        ConsumerFlowProperties consumerFlowProperties = createConsumerFlowProperties(
                consumerGroupName);

        CountDownLatch latch = new CountDownLatch(numberOfMessages);
        CountingListener countingListener = new CountingListener(latch);
        CountingListener countingListener2 = new CountingListener(latch);

        JCSMPSession session = SolaceTestUtils.createSession();
        JCSMPSession session2 = SolaceTestUtils.createSession();

        FlowReceiver consumer = session.createFlow(countingListener, consumerFlowProperties);
        FlowReceiver consumer2 = session2.createFlow(countingListener2, consumerFlowProperties);

        consumer.start();
        consumer2.start();

        latch.await();

        assertThat(countingListener.getErrors(), empty());
        assertThat(countingListener2.getErrors(), empty());

        assertThat("We missed some messages!", Iterables.concat(countingListener.getPayloads(), countingListener2.getPayloads()), iterableWithSize(numberOfMessages));

        assertThat("listener one got all the messages!", countingListener.getPayloads(), iterableWithSize(lessThan(numberOfMessages)));
        assertThat("listener two got all the messages!", countingListener2.getPayloads(), iterableWithSize(lessThan(numberOfMessages)));
    }

    @Test
    public void provision_whenASecondSubscriptionIsAdded_itGetsSubsequentMessages() throws Exception {
        String firstConsumerGroup = getRandomName("consumerGroup1");
        solaceQueueProvisioner.provisionTopicAndConsumerGroup(topic.getName(),
                firstConsumerGroup);

        messageProducer.send(createMessage(TEXT_CONTENT), topic);
        CountingListener countingListener = listenToQueue(firstConsumerGroup, 2);

        String secondConsumerGroup = getRandomName("consumerGroup2");
        solaceQueueProvisioner.provisionTopicAndConsumerGroup(topic.getName(),
                secondConsumerGroup);

        messageProducer.send(createMessage(MORE_TEXT_CONTENT), topic);
        CountingListener countingListener2 = listenToQueue(secondConsumerGroup);

        countingListener.awaitExpectedMessages();
        countingListener2.awaitExpectedMessages();

        assertThat(countingListener.getErrors(), empty());
        assertThat(countingListener.getPayloads(), contains(TEXT_CONTENT,
                MORE_TEXT_CONTENT));

        assertThat(countingListener2.getErrors(), empty());
        assertThat(countingListener2.getPayloads(), contains(MORE_TEXT_CONTENT));
    }

    @Test
    public void provision_whenMaxRedeliveryAttemptsNonzero_shouldRetryAndThenSendToDLQ() throws Exception {
        int maxRetries = 4;
        solaceConfigurationProperties.setMaxRedeliveryAttempts(maxRetries);

        SolaceQueueProvisioner reconfiguredProvisioner = new SolaceQueueProvisioner(
                solaceConfigurationProperties);

        TransactedSession transactedSession = session.createTransactedSession();
        String consumerGroupName = getRandomName("consumerGroup");
        reconfiguredProvisioner.provisionDeadLetterQueue();
        reconfiguredProvisioner.provisionTopicAndConsumerGroup(topic.getName(), consumerGroupName);

        messageProducer.send(createMessage(TEXT_CONTENT), topic);
        RollbackListener rollbackListener = listenAndRollback(consumerGroupName,
                transactedSession);
        CountingListener countingListener = listenToQueue(DLQ_NAME);

        countingListener.awaitExpectedMessages();
        assertThat(rollbackListener.getReceivedMessageCount(), is(maxRetries + 1));
        assertThat(countingListener.getPayloads().get(0), is(TEXT_CONTENT));
    }

    @Test
    public void provisionDLQ_createsANativeSolaceQueue() throws Exception {
        String deadLetterQueue = solaceQueueProvisioner.provisionDeadLetterQueue();

        //createQueue creates a local reference to the queue, the actual queue has to exist
        messageProducer.send(createMessage(TEXT_CONTENT), SolaceTestUtils.DLQ);
        CountingListener countingListener = listenToQueue(DLQ_NAME);
        countingListener.awaitExpectedMessages();

        assertThat(countingListener.getPayloads().size(), is(1));
        assertThat(countingListener.getPayloads().get(0), is(TEXT_CONTENT));
        assertThat(deadLetterQueue, is(DLQ_NAME));
    }

    private CountingListener listenToQueue(String queueName) throws JCSMPException {
        return listenToQueue(queueName, 1);
    }

    private CountingListener listenToQueue(String queueName, int expectedMessages) throws JCSMPException {
        ConsumerFlowProperties consumerFlowProperties = createConsumerFlowProperties(
                queueName);

        CountingListener countingListener = new CountingListener(expectedMessages);
        FlowReceiver consumer = session.createFlow(countingListener, consumerFlowProperties);
        consumer.start();

        return countingListener;
    }

    private RollbackListener listenAndRollback(String queueName, TransactedSession transactedSession) throws JCSMPException {
        ConsumerFlowProperties consumerFlowProperties = createConsumerFlowProperties(
                queueName);

        RollbackListener failingListener = new RollbackListener(transactedSession);
        FlowReceiver consumer = transactedSession.createFlow(failingListener, consumerFlowProperties, new EndpointProperties());
        consumer.start();

        return failingListener;
    }

    private ConsumerFlowProperties createConsumerFlowProperties(String queueName) {
        Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);

        ConsumerFlowProperties consumerFlowProperties = new ConsumerFlowProperties();
        consumerFlowProperties.setEndpoint(queue);
        return consumerFlowProperties;
    }

    private BytesXMLMessage createMessage(String userData) {
        BytesXMLMessage message = JCSMPFactory.onlyInstance().createMessage(BytesXMLMessage.class);
        message.setDeliveryMode(DeliveryMode.PERSISTENT);
        message.setDMQEligible(true);
        message.writeBytes(userData.getBytes());
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