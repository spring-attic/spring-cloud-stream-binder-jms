package org.springframework.cloud.stream.binder.jms.announcements;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(exclude = {EmbeddedServletContainerAutoConfiguration.class, WebMvcAutoConfiguration.class})
@EnableBinding(Source.class)
public class AnnouncerApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AnnouncerApplication.class, args);
        Announcer generator = context.getBean(Announcer.class);
        generator.announce("Joseph");
        generator.announce("Jack");
    }

}
