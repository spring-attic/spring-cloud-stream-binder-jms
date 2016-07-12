package integration;

import announcements.Announcer;
import announcements.AnnouncerApplication;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Created by pivotal on 12/07/2016.
 */
public class AnnouncementTest {

    @Test
    public void testSendMessage() {
        ConfigurableApplicationContext announcerContext = SpringApplication.run(AnnouncerApplication.class);
        Announcer announcer = announcerContext.getBean(Announcer.class);
        announcer.announce("Hello!");
    }

}
