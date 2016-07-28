package org.springframework.cloud.stream.binder.jms.solace.integration;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.jms.solace.integration.receiver.ReceiverApplication;
import org.springframework.cloud.stream.binder.jms.solace.integration.receiver.ReceiverApplication.Receiver;
import org.springframework.cloud.stream.binder.jms.solace.integration.sender.SenderApplication;
import org.springframework.cloud.stream.binder.jms.solace.integration.sender.SenderApplication.Sender;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.Message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EndToEndIntegrationTest {

    private List<ConfigurableApplicationContext> startedContexts = new ArrayList<>();

    private String destination;

    private static final String OUTPUT_DESTINATION_FORMAT = "--spring.cloud.stream.bindings.output.destination=%s";
    private static final String INPUT_DESTINATION_FORMAT = "--spring.cloud.stream.bindings.input.destination=%s";
    private static final String INPUT_GROUP_FORMAT = "--spring.cloud.stream.bindings.input.group=%s";
    private static final String MAX_ATTEMPTS_1 = "--spring.cloud.stream.bindings.input.consumer.maxAttempts=1";

    private String randomGroupArg1;
    private String randomGroupArg2;

    @Before
    public void startProducerAndConsumers() {
        this.destination = getRandomName("destination");
        randomGroupArg1 = String.format(INPUT_GROUP_FORMAT, getRandomName("group1"));
        randomGroupArg2 = String.format(INPUT_GROUP_FORMAT, getRandomName("group2"));
    }

    @After
    public void stopProducerAndConsumers() {
        startedContexts.forEach(ConfigurableApplicationContext::stop);
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
    public void scs_whenConsumerFails_retriesTheSpecifiedAmountOfTimes() throws Exception {
        Sender sender = createSender();
        Receiver receiver = createReceiver(randomGroupArg1);

        sender.send(Receiver.PLEASE_THROW_AN_EXCEPTION);

        List<Message> receivedMessages = receiver.getReceivedMessages();
        List<Message> handledMessages = receiver.getHandledMessages();

        waitFor(5000, () -> {
            assertThat(receivedMessages, hasSize(3));
            assertThat(handledMessages, empty());
            assertThat(extractPayload(receivedMessages), Matchers.contains(Receiver.PLEASE_THROW_AN_EXCEPTION, Receiver.PLEASE_THROW_AN_EXCEPTION, Receiver.PLEASE_THROW_AN_EXCEPTION));
        });

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
        IntStream.range(0,1000).mapToObj(String::valueOf).forEach(sender::send);

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
    @Ignore
    public void scs_supportsSerializable() throws Exception {
        Sender sender = createSender();
        Receiver receiver = createReceiver(randomGroupArg1);

        SerializableTest serializableTest = new SerializableTest("some value");
        sender.send(serializableTest);

        List<Message> messages = receiver.getHandledMessages();

        waitFor(() -> {
            assertThat(messages, hasSize(1));
            assertThat(extractPayload(messages), containsInAnyOrder(serializableTest));
        });
    }

    @Test
    @Ignore
    public void scs_supportsList() throws Exception {
        Sender sender = createSender();
        Receiver receiver = createReceiver(randomGroupArg1);

        List<String> stringList = Arrays.asList("hello", "world");
        sender.send(stringList);

        List<Message> messages = receiver.getHandledMessages();

        waitFor(() -> {
            assertThat(messages, hasSize(1));
            assertThat(extractPayload(messages), containsInAnyOrder(stringList));
        });
    }

    @Test
    @Ignore
    public void scs_supportsInt() throws Exception {
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
        String destinationArg = String.format(OUTPUT_DESTINATION_FORMAT, this.destination);
        arguments = Stream.concat(Arrays.stream(arguments), Stream.of(destinationArg)).toArray(String[]::new);

        ConfigurableApplicationContext context = new SpringApplicationBuilder(SenderApplication.class).build().run(arguments);
        startedContexts.add(context);
        return context.getBean(Sender.class);
    }

    private Receiver createReceiver(String... arguments) {
        String destinationArg = String.format(INPUT_DESTINATION_FORMAT, this.destination);
        arguments = Stream.concat(Arrays.stream(arguments), Stream.of(destinationArg)).toArray(String[]::new);

        ConfigurableApplicationContext context = new SpringApplicationBuilder(ReceiverApplication.class).build().run(arguments);
        startedContexts.add(context);
        return context.getBean(Receiver.class);
    }

    public void waitFor(Runnable assertion) throws InterruptedException {
        waitFor(1000, assertion);
    }

    public void waitFor(int millis, Runnable assertion) throws InterruptedException {
        long endTime = System.currentTimeMillis() + millis;

        while (true) {
            try {
                assertion.run();
                return;
            } catch (AssertionError e) {
                if (System.currentTimeMillis() > endTime) {
                    throw e;
                }
            }
            Thread.sleep(millis / 10);
        }
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
    }
}
