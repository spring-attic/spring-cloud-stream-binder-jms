package org.springframework.cloud.stream.binder.jms.greetings;

import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Greeter {

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
