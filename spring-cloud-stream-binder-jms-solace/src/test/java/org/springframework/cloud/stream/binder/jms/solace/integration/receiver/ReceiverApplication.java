package org.springframework.cloud.stream.binder.jms.solace.integration.receiver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication(exclude = {EmbeddedServletContainerAutoConfiguration.class, WebMvcAutoConfiguration.class, JmxAutoConfiguration.class})
@EnableBinding(Sink.class)
public class ReceiverApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReceiverApplication.class, args);
    }

    @Component
    public static class Receiver {

        public static final String PLEASE_THROW_AN_EXCEPTION = "Please throw an exception";
        private final List<Message> handledMessages = new ArrayList<>();

        private final List<Message> receivedMessages = new ArrayList<>();

        @StreamListener(Sink.INPUT)
        public void greet(Message message) {
            receivedMessages.add(message);

            if (message.getPayload().equals(PLEASE_THROW_AN_EXCEPTION)) {
                throw new RuntimeException("Your wish is my command");
            }

            handledMessages.add(message);
        }

        public List<Message> getHandledMessages() {
            return handledMessages;
        }

        public List<Message> getReceivedMessages() {
            return receivedMessages;
        }
    }
}
