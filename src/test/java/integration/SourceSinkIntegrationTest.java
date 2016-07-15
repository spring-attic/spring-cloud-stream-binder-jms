package integration;

import announcements.Announcer;
import announcements.AnnouncerApplication;
import com.google.common.collect.ImmutableMap;
import greetings.Greeter;
import greetings.GreeterApplication;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

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
        greeterContext = SpringApplication.run(GreeterApplication.class);
        greeterContext2 = SpringApplication.run(GreeterApplication.class);
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

        List<Message> otherReceivedMessages = greeter2.getReceivedMessages();
        List<Message> messages = greeter.getReceivedMessages();

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
        List<Message> messages = greeter.getReceivedMessages();

        waitFor(() -> assertThat(messages, contains(
                allOf(
                    hasProperty("payload", is("Joseph")),
                    hasProperty("headers", hasEntry("holy", "header"))
                )
        )));

    }

    @Ignore
    public void serializesPayloads() throws Exception {
        Announcer announcer = announcerContext.getBean(Announcer.class);

        SerializableTest serializableTest = new SerializableTest("some value");
        announcer.announce(serializableTest);

        Greeter greeter = greeterContext.getBean(Greeter.class);
        Greeter greeter2 = greeterContext2.getBean(Greeter.class);

        List<Message> otherReceivedMessages = greeter2.getReceivedMessages();
        List<Message> messages = greeter.getReceivedMessages();

        waitFor(() -> {
            assertThat(extractPayload(messages), containsInAnyOrder(serializableTest));
            assertThat(extractPayload(otherReceivedMessages), containsInAnyOrder(serializableTest));
        });
    }

    private List<? extends Object> extractPayload(List<Message> messages) {
        return messages.stream().map(Message::getPayload).collect(Collectors.toList());
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
