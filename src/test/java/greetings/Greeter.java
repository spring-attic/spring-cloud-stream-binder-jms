package greetings;

import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Greeter {

    private final List<Message> messages = new ArrayList<>();

    @StreamListener(Sink.INPUT)
    public void greet(Message message) {
        messages.add(message);
    }

    public List<Message> getReceivedMessages() {
        return messages;
    }
}
