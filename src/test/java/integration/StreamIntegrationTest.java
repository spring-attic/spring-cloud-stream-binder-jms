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

public class StreamIntegrationTest {

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

//        waitFor(() -> {
        Thread.sleep(1000);
            assertThat(messages, hasSize(2));
            assertThat(messages, containsInAnyOrder("Joseph", "Jack"));
//        });
    }

    public void waitFor(Runnable assertion) throws InterruptedException {
        waitFor(1000, assertion);
    }

    public void waitFor(int millis, Runnable assertion) throws InterruptedException {
        while (true) {
            long endTime = System.currentTimeMillis() + millis;
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
}
