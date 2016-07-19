package org.springframework.cloud.stream.binder.jms.greetings;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = GreeterApplication.class)
@WebAppConfiguration
@DirtiesContext
public class GreeterApplicationTest {

    @Autowired
    private Sink sink;

    @Test
    public void contextLoads() {
        assertNotNull(this.sink.input());
    }
}