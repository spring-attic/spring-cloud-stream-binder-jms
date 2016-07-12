package greetings;

import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Greeter {

    private final List<String> messages = new ArrayList<>();

    @StreamListener(Sink.INPUT)
    public void greet(Object message) {
        messages.add(message.toString());
        System.out.println(String.format("Hello %s", message));
    }

    public List<String> getReceivedMessages() {
        return messages;
    }
}
