package org.springframework.cloud.stream.binder.jms.solace.integration;

import org.springframework.cloud.stream.binder.jms.solace.announcements.Announcer;
import org.springframework.cloud.stream.binder.jms.solace.announcements.AnnouncerApplication;
import org.springframework.cloud.stream.binder.jms.solace.greetings.Greeter;
import org.springframework.cloud.stream.binder.jms.solace.greetings.GreeterApplication;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.Repeat;

import java.util.List;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SourceSinkNoRetryIntegrationTest extends SpringBinderIntegrationTest {

    private ConfigurableApplicationContext greeterContext;
    private ConfigurableApplicationContext announcerContext;

    @Before
    public void setUp() {
        String destination = getRandomName("destination");
        String inputDestinationFormat = "--spring.cloud.stream.bindings.input.destination=%s";
        String inputGroupFormat = "--spring.cloud.stream.bindings.input.group=%s";
        String outputDestinationFormat = "--spring.cloud.stream.bindings.output.destination=%s";

        greeterContext = SpringApplication.run(GreeterApplication.class, String.format(inputGroupFormat, getRandomName("group")), String.format(inputDestinationFormat, destination));
        announcerContext = SpringApplication.run(AnnouncerApplication.class, String.format(outputDestinationFormat, destination));
    }

    @After
    public void tearDown() {
        if (greeterContext != null) {
            greeterContext.stop();
        }
        if (announcerContext != null) {
            announcerContext.stop();
        }
    }

    @Test
    public void supportsNoRetry() throws Exception {
        Announcer announcer = announcerContext.getBean(Announcer.class);

        announcer.announce("José");

        Greeter greeter = greeterContext.getBean(Greeter.class);
        List<Message> messages = greeter.getHandledMessages();

        waitFor(() -> assertThat(extractPayload(messages), containsInAnyOrder("José")));

    }


}
