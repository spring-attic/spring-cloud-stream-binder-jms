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

package org.springframework.cloud.stream.binder.jms.solace.integration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.ImmutableMap;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.jms.solace.SolaceQueueProvisioner;
import org.springframework.cloud.stream.binder.jms.solace.SolaceTestUtils;
import org.springframework.cloud.stream.binder.jms.solace.config.SolaceConfigurationProperties;
import org.springframework.cloud.stream.binder.jms.solace.config.SolaceJmsConfiguration;
import org.springframework.cloud.stream.binder.jms.solace.integration.receiver.ReceiverApplication;
import org.springframework.cloud.stream.binder.jms.solace.integration.receiver.ReceiverApplication.Receiver;
import org.springframework.cloud.stream.binder.jms.solace.integration.sender.SenderApplication;
import org.springframework.cloud.stream.binder.jms.solace.integration.sender.SenderApplication.Sender;
import org.springframework.cloud.stream.binder.jms.utils.RepublishMessageRecoverer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.springframework.cloud.stream.binder.jms.solace.SolaceTestUtils.waitFor;
import static org.springframework.cloud.stream.binder.jms.solace.SolaceTestUtils.waitForDeadLetter;

public class EndToEndIntegrationTests {

    private static final String OUTPUT_DESTINATION_FORMAT = "--spring.cloud.stream.bindings.output.destination=%s";
    private static final String INPUT_DESTINATION_FORMAT = "--spring.cloud.stream.bindings.input.destination=%s";
    private static final String INPUT_GROUP_FORMAT = "--spring.cloud.stream.bindings.input.group=%s";
    private static final String MAX_ATTEMPTS_1 = "--spring.cloud.stream.bindings.input.consumer.maxAttempts=1";
    private List<ConfigurableApplicationContext> startedContexts = new ArrayList<>();
    private String destination;
    private String randomGroupArg1;
    private String randomGroupArg2;

    @Before
    public void configureGroupsAndDestinations() throws JCSMPException {
        SolaceTestUtils.deprovisionDLQ();
        this.destination = getRandomName("destination");
        randomGroupArg1 = String.format(INPUT_GROUP_FORMAT, getRandomName("group1"));
        randomGroupArg2 = String.format(INPUT_GROUP_FORMAT, getRandomName("group2"));
    }

    @After
    public void stopProducerAndConsumers() throws JCSMPException {
        startedContexts.forEach(ConfigurableApplicationContext::stop);
        SolaceTestUtils.deprovisionDLQ();
    }

    @Test
    public void scs_whenMultipleConsumerGroups_eachGroupGetsAllMessages() throws Exception {
        Sender sender = createSender();
        Receiver receiver = createReceiver(randomGroupArg1);
        Receiver receiver2 = createReceiver(randomGroupArg2);

        sender.send("Joseph");
        sender.send("Jack");
        sender.send("123");
        sender.send("hi world");


        List<Message> otherReceivedMessages = receiver2.getHandledMessages();
        List<Message> messages = receiver.getHandledMessages();

        waitFor(() -> {
            assertThat(messages, hasSize(4));
            assertThat(otherReceivedMessages, hasSize(4));
            assertThat(extractPayload(messages), containsInAnyOrder("Joseph", "Jack", "123", "hi world"));
            assertThat(extractPayload(otherReceivedMessages), containsInAnyOrder("Joseph", "Jack", "123", "hi world"));
        });
    }

    @Test
    public void scs_whenMultipleMembersOfSameConsumerGroup_groupOnlySeesEachMessageOnce() throws Exception {
        Sender sender = createSender();
        Receiver receiver = createReceiver(randomGroupArg1);
        Receiver receiver2 = createReceiver(randomGroupArg1);

        IntStream.range(0, 1000).mapToObj(String::valueOf).forEach(sender::send);

        List<Message> otherReceivedMessages = receiver2.getHandledMessages();
        List<Message> messages = receiver.getHandledMessages();

        waitFor(() -> assertThat(otherReceivedMessages.size() + messages.size(), is(1000)));

        waitFor(() -> {
            assertThat(otherReceivedMessages, iterableWithSize(lessThan(1000)));
            assertThat(messages, iterableWithSize(lessThan(1000)));
        });
    }

    @Test
    public void scs_whenHeadersAreSpecified_headersArePassedThrough() throws Exception {
        Sender sender = createSender();
        Receiver receiver = createReceiver(randomGroupArg1);

        sender.send("Joseph", ImmutableMap.of("holy", "header"));

        List<Message> messages = receiver.getHandledMessages();

        waitFor(() -> assertThat(messages, contains(
                allOf(
                        hasProperty("payload", is("Joseph")),
                        hasProperty("headers", hasEntry("holy", "header"))
                )
        )));

    }

    @Test
    public void scs_whenMessageIsSentToDLQ_stackTraceAddedToHeaders() throws Exception {
        Sender sender = createSender();
        createReceiver(randomGroupArg1);

        new SolaceQueueProvisioner(SolaceTestUtils.getSolaceProperties()).provisionDeadLetterQueue();

        sender.send(Receiver.PLEASE_THROW_AN_EXCEPTION);

        BytesXMLMessage bytesXMLMessage = waitForDeadLetter(10000);
        assertThat(bytesXMLMessage, notNullValue());

        String stacktrace = (String) bytesXMLMessage.getProperties().get(RepublishMessageRecoverer.X_EXCEPTION_STACKTRACE);
        assertThat(stacktrace, containsString("Your wish is my command"));
    }

    @Test
    public void scs_whenConsumerFails_retriesTheSpecifiedAmountOfTimes() throws Exception {
        Sender sender = createSender();
        Receiver receiver = createReceiver(randomGroupArg1);

        sender.send(Receiver.PLEASE_THROW_AN_EXCEPTION);

        List<Message> receivedMessages = receiver.getReceivedMessages();
        List<Message> handledMessages = receiver.getHandledMessages();

        waitFor(10000, () -> {
            assertThat(receivedMessages, hasSize(3));
            assertThat(handledMessages, empty());
            assertThat(extractPayload(receivedMessages), Matchers.contains(Receiver.PLEASE_THROW_AN_EXCEPTION, Receiver.PLEASE_THROW_AN_EXCEPTION, Receiver.PLEASE_THROW_AN_EXCEPTION));
        });

        BytesXMLMessage bytesXMLMessage = waitForDeadLetter();
        assertThat(bytesXMLMessage, notNullValue());

    }

    @Test
    public void scs_whenRetryIsDisabled_doesNotRetry() throws Exception {
        Sender sender = createSender();
        Receiver receiver = createReceiver(randomGroupArg1, MAX_ATTEMPTS_1);

        sender.send("José");

        List<Message> messages = receiver.getHandledMessages();

        waitFor(() -> {
            assertThat(messages, hasSize(1));
            assertThat(extractPayload(messages), containsInAnyOrder("José"));
        });
    }

    @Test
    public void scs_whenAPartitioningKeyIsConfigured_messagesAreRoutedToTheRelevantPartition() throws Exception {
        Sender sender = createSender(
                String.format("--spring.cloud.stream.bindings.output.producer.partitionKeyExpression=%s", "payload.equals('foo')?0:1"),
                String.format("--spring.cloud.stream.bindings.output.producer.partitionCount=%s", 2)
        );

        Receiver receiverPartition0 = createReceiver(
                randomGroupArg1,
                String.format("--spring.cloud.stream.instance-index=%s", 0),
                String.format("--spring.cloud.stream.instance-count=%s", 2),
                String.format("--spring.cloud.stream.bindings.input.consumer.partitioned=%s", true)
        );
        Receiver receiverPartition1 = createReceiver(
                randomGroupArg1,
                String.format("--spring.cloud.stream.instance-index=%s", 1),
                String.format("--spring.cloud.stream.instance-count=%s", 2),
                String.format("--spring.cloud.stream.bindings.input.consumer.partitioned=%s", true)
        );

        sender.send("foo");
        sender.send("bar");
        sender.send("baz");
        sender.send("foo");
        IntStream.range(0, 1000).mapToObj(String::valueOf).forEach(sender::send);

        List<Message> messagesPartition0 = receiverPartition0.getHandledMessages();
        List<Message> messagesPartition1 = receiverPartition1.getHandledMessages();

        waitFor(() -> {
            assertThat(messagesPartition1, hasSize(1002));
            List<String> objects = (List<String>) extractPayload(messagesPartition1);
            assertThat(objects, hasItems("bar", "baz"));

            assertThat(messagesPartition0, hasSize(2));
            assertThat(extractPayload(messagesPartition0), containsInAnyOrder("foo", "foo"));
        });
    }

    @Test
    public void scs_supportsSerializable() throws Exception {
        Sender sender = createSender();
        Receiver receiver = createReceiver(randomGroupArg1, MAX_ATTEMPTS_1);

        SerializableTest serializableTest = new SerializableTest("some value");
        sender.send(serializableTest);

        List<Message> messages = receiver.getHandledMessages();

        waitFor(() -> {
            assertThat(messages, hasSize(1));
            assertThat(extractPayload(messages), containsInAnyOrder(serializableTest));
        });
    }

    @Test
    public void scs_supportsPrimitive() throws Exception {
        Sender sender = createSender();
        Receiver receiver = createReceiver(randomGroupArg1);

        sender.send(11);

        List<Message> messages = receiver.getHandledMessages();

        waitFor(() -> {
            assertThat(messages, hasSize(1));
            assertThat(extractPayload(messages), containsInAnyOrder(11));
        });
    }

    private Sender createSender(String... arguments) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(SenderApplication.class)
                .bannerMode(Banner.Mode.OFF)
                .build()
                .run(applicationArguments(String.format(OUTPUT_DESTINATION_FORMAT, this.destination), arguments));

        startedContexts.add(context);
        return context.getBean(Sender.class);
    }

    private Receiver createReceiver(String... arguments) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(ReceiverApplication.class)
                .bannerMode(Banner.Mode.OFF)
                .build()
                .run(applicationArguments(String.format(INPUT_DESTINATION_FORMAT, this.destination), arguments));

        startedContexts.add(context);
        return context.getBean(Receiver.class);
    }

    private String[] applicationArguments(String destinationArg, String[] arguments) {
        return Stream.concat(Arrays.stream(arguments), Stream.of(
                destinationArg,
                "--logging.level.org.springframework=WARN",
                "--logging.level.org.springframework.boot=WARN"
        )).toArray(String[]::new);
    }

    List<? extends Object> extractPayload(Iterable<Message> messages) {
        return StreamSupport.stream(messages.spliterator(), false).map(Message::getPayload).collect(Collectors.toList());
    }

    private String getRandomName(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    public static class SerializableTest implements Serializable {
        public final String value;

        public SerializableTest(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "SerializableTest<" + value + ">";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SerializableTest that = (SerializableTest) o;

            return value.equals(that.value);

        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }
}
