package org.springframework.cloud.stream.binder.jms.solace.announcements;

import com.solacesystems.jms.SolConnectionFactoryImpl;
import com.solacesystems.jms.property.JMSProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binder.jms.solace.config.SolaceJmsConfiguration;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import java.util.Hashtable;

@SpringBootApplication(exclude = {EmbeddedServletContainerAutoConfiguration.class, WebMvcAutoConfiguration.class})
@Import(SolaceJmsConfiguration.class)
@EnableBinding(Source.class)
public class AnnouncerApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AnnouncerApplication.class, args);
        Announcer generator = context.getBean(Announcer.class);
        generator.announce("Joseph");
        generator.announce("Jack");
    }

}
