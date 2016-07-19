package org.springframework.cloud.stream.binder.jms.integration;

import org.springframework.cloud.stream.binder.jms.announcements.Announcer;
import org.springframework.cloud.stream.binder.jms.announcements.AnnouncerApplication;
import org.springframework.cloud.stream.binder.jms.greetings.Greeter;
import org.springframework.cloud.stream.binder.jms.greetings.GreeterApplication;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.Message;

import java.util.List;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SourceSinkNoRetryIntegrationTest extends SpringBinderIntegrationTest {

    private ConfigurableApplicationContext greeterContext;
    private ConfigurableApplicationContext announcerContext;

    @Before
    public void setUp() {
        greeterContext = new SpringApplicationBuilder().profiles("noretry").sources(GreeterApplication.class).build().run();
        announcerContext = SpringApplication.run(AnnouncerApplication.class);
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
