package announcements;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class Announcer {

    private Source source;

    @Autowired
    public Announcer(Source source) {
        this.source = source;
    }

    public void announce(String name) {
        Message message = MessageBuilder.withPayload(name).build();
        source.output().send(message);
    }
}
