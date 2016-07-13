package integration;

import announcements.Announcer;
import announcements.AnnouncerApplication;
import greetings.Greeter;
import greetings.GreeterApplication;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertThat;

public class SourceSinkIntegrationTest extends SpringBinderIntegrationTest {

    private ConfigurableApplicationContext greeterContext;
    private ConfigurableApplicationContext announcerContext;

    @Before
    public void setUp() {
        greeterContext = SpringApplication.run(GreeterApplication.class);
        announcerContext = SpringApplication.run(AnnouncerApplication.class);
    }

    @After
    public void tearDown() {
        greeterContext.stop();
        announcerContext.stop();
    }

    @Test
    public void sinkReceivesMessages() throws Exception {
        Announcer announcer = announcerContext.getBean(Announcer.class);

        announcer.announce("Joseph");
        announcer.announce("Jack");

        Greeter greeter = greeterContext.getBean(Greeter.class);
        List<String> messages = greeter.getReceivedMessages();

        waitFor(() -> {
            assertThat(messages, hasSize(2));
            assertThat(messages, containsInAnyOrder("Joseph", "Jack"));
        });
    }

}
