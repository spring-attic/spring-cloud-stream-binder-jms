package org.springframework.cloud.stream.binder.jms.solace.integration;

import org.hamcrest.Matchers;
import org.springframework.cloud.stream.binder.jms.solace.announcements.Announcer;
import org.springframework.cloud.stream.binder.jms.solace.announcements.AnnouncerApplication;
import com.google.common.collect.ImmutableMap;
import org.springframework.cloud.stream.binder.jms.solace.greetings.Greeter;
import org.springframework.cloud.stream.binder.jms.solace.greetings.GreeterApplication;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.Message;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SourceSinkIntegrationTest extends SpringBinderIntegrationTest {

    private ConfigurableApplicationContext greeterContext;
    private ConfigurableApplicationContext announcerContext;
    private ConfigurableApplicationContext greeterContext2;

    @Before
    public void setUp() {
        greeterContext = SpringApplication.run(GreeterApplication.class,"--spring.cloud.stream.bindings.input.group=y");
        greeterContext2 = SpringApplication.run(GreeterApplication.class,"--spring.cloud.stream.bindings.input.group=x");
        announcerContext = SpringApplication.run(AnnouncerApplication.class);
    }

    @After
    public void tearDown() {
        if (greeterContext != null) {
            greeterContext.stop();
        }
        if (greeterContext2 != null) {
            greeterContext2.stop();
        }
        if (announcerContext != null) {
            announcerContext.stop();
        }
    }

    @Test
    public void sinkReceivesMessages() throws Exception {
        Announcer announcer = announcerContext.getBean(Announcer.class);

        announcer.announce("Joseph");
        announcer.announce("Jack");
        announcer.announce(123);
        announcer.announce(Arrays.asList("hi", "world"));

        Greeter greeter = greeterContext.getBean(Greeter.class);

        Greeter greeter2 = greeterContext2.getBean(Greeter.class);

        List<Message> otherReceivedMessages = greeter2.getHandledMessages();
        List<Message> messages = greeter.getHandledMessages();

        waitFor(() -> {
            assertThat(extractPayload(messages), containsInAnyOrder("Joseph", "Jack", 123, Arrays.asList("hi", "world")));
            assertThat(extractPayload(otherReceivedMessages), containsInAnyOrder("Joseph", "Jack", 123, Arrays.asList("hi", "world")));
        });
    }

    @Test
    public void propagatesHeaders() throws Exception {
        Announcer announcer = announcerContext.getBean(Announcer.class);

        announcer.announce("Joseph", ImmutableMap.of("holy", "header"));

        Greeter greeter = greeterContext.getBean(Greeter.class);
        List<Message> messages = greeter.getHandledMessages();

        waitFor(() -> assertThat(messages, contains(
                allOf(
                    hasProperty("payload", is("Joseph")),
                    hasProperty("headers", hasEntry("holy", "header"))
                )
        )));

    }

    @Test
    public void retriesByDefault() throws Exception {
        Announcer announcer = announcerContext.getBean(Announcer.class);

        announcer.announce(Greeter.PLEASE_THROW_AN_EXCEPTION);

        Greeter greeter = greeterContext.getBean(Greeter.class);
        List<Message> receivedMessages = greeter.getReceivedMessages();
        List<Message> handledMessages = greeter.getHandledMessages();

        waitFor(5000, () -> {
            assertThat(extractPayload(receivedMessages), Matchers.contains(Greeter.PLEASE_THROW_AN_EXCEPTION, Greeter.PLEASE_THROW_AN_EXCEPTION, Greeter.PLEASE_THROW_AN_EXCEPTION));
            assertThat(extractPayload(handledMessages), empty());
        });

    }

    @Ignore
    public void serializesPayloads() throws Exception {
        Announcer announcer = announcerContext.getBean(Announcer.class);

        SerializableTest serializableTest = new SerializableTest("some value");
        announcer.announce(serializableTest);

        Greeter greeter = greeterContext.getBean(Greeter.class);
        Greeter greeter2 = greeterContext2.getBean(Greeter.class);

        List<Message> otherReceivedMessages = greeter2.getHandledMessages();
        List<Message> messages = greeter.getHandledMessages();

        waitFor(() -> {
            assertThat(extractPayload(messages), containsInAnyOrder(serializableTest));
            assertThat(extractPayload(otherReceivedMessages), containsInAnyOrder(serializableTest));
        });
    }


}
