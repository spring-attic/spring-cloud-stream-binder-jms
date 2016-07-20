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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EndToEndIntegrationTest {

    private LazyApplicationContext receiverContextGroup1;
    private LazyApplicationContext receiverContext2Group1;
    private LazyApplicationContext senderContext;
    private LazyApplicationContext receiverContextGroup2;
    private LazyApplicationContext receiverContextNoRetry;


    @Before
    public void startProducerAndConsumers() {
        String destination = getRandomName("destination");
        String inputDestinationFormat = "--spring.cloud.stream.bindings.input.destination=%s";
        String inputGroupFormat = "--spring.cloud.stream.bindings.input.group=%s";
        String outputDestinationFormat = "--spring.cloud.stream.bindings.output.destination=%s";
        String group1Name = getRandomName("group1");

        receiverContextGroup1 = new LazyApplicationContext(() -> new SpringApplicationBuilder(ReceiverApplication.class).build().run(String.format(inputGroupFormat, group1Name), String.format(inputDestinationFormat, destination)));
        receiverContext2Group1 = new LazyApplicationContext(() -> new SpringApplicationBuilder(ReceiverApplication.class).build().run(String.format(inputGroupFormat, group1Name), String.format(inputDestinationFormat, destination)));
        receiverContextGroup2 = new LazyApplicationContext(() -> new SpringApplicationBuilder(ReceiverApplication.class).build().run(String.format(inputGroupFormat, getRandomName("group2")), String.format(inputDestinationFormat, destination)));
        receiverContextNoRetry = new LazyApplicationContext(() -> new SpringApplicationBuilder(ReceiverApplication.class).profiles("noretry").build().run(String.format(inputGroupFormat, getRandomName("group3")), String.format(inputDestinationFormat, destination)));
        senderContext = new LazyApplicationContext(() -> new SpringApplicationBuilder(SenderApplication.class).build().run(String.format(outputDestinationFormat, destination)));
    }

    @After
    public void stopProducerAndConsumers() {
        receiverContextGroup1.stopIfStarted();
        receiverContextGroup2.stopIfStarted();
        receiverContext2Group1.stopIfStarted();
        receiverContextNoRetry.stopIfStarted();
        senderContext.stopIfStarted();

    }

    @Test
    public void scs_whenMultipleConsumerGroups_eachGroupGetsAllMessages() throws Exception {
        Sender sender = senderContext.get().getBean(Sender.class);
        Receiver receiver = receiverContextGroup1.get().getBean(Receiver.class);
        Receiver receiver2 = receiverContextGroup2.get().getBean(Receiver.class);

        sender.send("Joseph");
        sender.send("Jack");
        sender.send(123);
        sender.send(Arrays.asList("hi", "world"));


        List<Message> otherReceivedMessages = receiver2.getHandledMessages();
        List<Message> messages = receiver.getHandledMessages();

        waitFor(() -> {
            assertThat(extractPayload(messages), containsInAnyOrder("Joseph", "Jack", 123, Arrays.asList("hi", "world")));
            assertThat(extractPayload(otherReceivedMessages), containsInAnyOrder("Joseph", "Jack", 123, Arrays.asList("hi", "world")));
        });
    }

    @Test
    public void scs_whenMultipleMembersOfSameConsumerGroup_groupOnlySeesEachMessageOnce() throws Exception {
        Sender sender = senderContext.get().getBean(Sender.class);
        Receiver receiver = receiverContextGroup1.get().getBean(Receiver.class);
        Receiver receiver2 = receiverContext2Group1.get().getBean(Receiver.class);

        IntStream.range(0,1000).forEach(sender::send);

        List<Message> otherReceivedMessages = receiver2.getHandledMessages();
        List<Message> messages = receiver.getHandledMessages();

        Iterable<Message> combinedMessages = Iterables.concat(messages, otherReceivedMessages);

        waitFor(() -> {
            assertThat(combinedMessages, iterableWithSize(1000));
            assertThat(otherReceivedMessages, iterableWithSize(lessThan(1000)));
            assertThat(messages, iterableWithSize(lessThan(1000)));
        });
    }

    @Test
    public void scs_whenHeadersAreSpecified_headersArePassedThrough() throws Exception {
        Sender sender = senderContext.get().getBean(Sender.class);
        Receiver receiver = receiverContextGroup1.get().getBean(Receiver.class);

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
    public void scs_whenThereConsumerFails_retriesTheSpecifiedAmountOfTimes() throws Exception {
        Sender sender = senderContext.get().getBean(Sender.class);
        Receiver receiver = receiverContextGroup1.get().getBean(Receiver.class);

        sender.send(Receiver.PLEASE_THROW_AN_EXCEPTION);

        List<Message> receivedMessages = receiver.getReceivedMessages();
        List<Message> handledMessages = receiver.getHandledMessages();

        waitFor(5000, () -> {
            assertThat(extractPayload(receivedMessages), Matchers.contains(Receiver.PLEASE_THROW_AN_EXCEPTION, Receiver.PLEASE_THROW_AN_EXCEPTION, Receiver.PLEASE_THROW_AN_EXCEPTION));
            assertThat(extractPayload(handledMessages), empty());
        });

    }

    @Test
    public void scs_whenRetryIsDisabled_doesNotRetry() throws Exception {
        Sender sender = senderContext.get().getBean(Sender.class);
        Receiver receiver = receiverContextNoRetry.get().getBean(Receiver.class);

        sender.send("José");

        List<Message> messages = receiver.getHandledMessages();

        waitFor(() -> assertThat(extractPayload(messages), containsInAnyOrder("José")));
    }

    @Ignore
    public void serializesPayloads() throws Exception {
        Sender sender = senderContext.get().getBean(Sender.class);

        SerializableTest serializableTest = new SerializableTest("some value");
        sender.send(serializableTest);

        Receiver receiver = receiverContextGroup1.get().getBean(Receiver.class);
        Receiver receiver2 = receiverContextGroup2.get().getBean(Receiver.class);

        List<Message> otherReceivedMessages = receiver2.getHandledMessages();
        List<Message> messages = receiver.getHandledMessages();

        waitFor(() -> {
            assertThat(extractPayload(messages), containsInAnyOrder(serializableTest));
            assertThat(extractPayload(otherReceivedMessages), containsInAnyOrder(serializableTest));
        });
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
        return StreamSupport.stream(messages.spliterator(),false).map(Message::getPayload).collect(Collectors.toList());
    }

    private String getRandomName(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }


    private static class LazyApplicationContext {

        private final Supplier<ConfigurableApplicationContext> supplier;
        private ConfigurableApplicationContext app;

        public LazyApplicationContext(Supplier<ConfigurableApplicationContext> supplier) {
            this.supplier = supplier;
        }

        public ConfigurableApplicationContext get() {
            if(!isCreated()) {
                app = supplier.get();
            }
            return app;
        }

        private boolean isCreated() {
            return app != null;
        }

        public void stopIfStarted(){
            if (isCreated()) {
                app.stop();
            }
        }
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
