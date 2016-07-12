package announcements;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AnnouncerApplication.class)
@WebAppConfiguration
@DirtiesContext
public class AnnouncerApplicationTest {

    @Autowired
    private Source source;

    @Test
    public void contextLoads() {
        assertNotNull(this.source.output());
    }
}