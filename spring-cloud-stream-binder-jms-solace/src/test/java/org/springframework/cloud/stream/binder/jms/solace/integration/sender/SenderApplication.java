package org.springframework.cloud.stream.binder.jms.solace.integration.sender;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication(exclude = {EmbeddedServletContainerAutoConfiguration.class, WebMvcAutoConfiguration.class})
@EnableBinding(Source.class)
public class SenderApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(SenderApplication.class, args);
        Sender generator = context.getBean(Sender.class);
        generator.send("Joseph");
        generator.send("Jack");
    }

    @Component
    public static class Sender {

        private Source source;

        @Autowired
        public Sender(Source source) {
            this.source = source;
        }

        public void send(Object something) {
            send(something, new HashMap<>());
        }

        public void send(Object something, Map<String, Object> headers) {
            MessageBuilder<Object> builder = MessageBuilder.withPayload(something);
            headers.entrySet().forEach(s -> builder.setHeader(s.getKey(), s.getValue()));
            Message message = builder.build();
            source.output().send(message);
        }
    }
}
