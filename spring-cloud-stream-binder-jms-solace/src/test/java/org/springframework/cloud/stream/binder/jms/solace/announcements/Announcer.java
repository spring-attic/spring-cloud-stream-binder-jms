package org.springframework.cloud.stream.binder.jms.solace.announcements;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class Announcer {

    private Source source;

    @Autowired
    public Announcer(Source source) {
        this.source = source;
    }

    public void announce(Object something) {
        announce(something, new HashMap<>());
    }

    public void announce(Object something, Map<String, Object> headers) {
        MessageBuilder<Object> builder = MessageBuilder.withPayload(something);
        headers.entrySet().forEach(s -> builder.setHeader(s.getKey(), s.getValue()));
        Message message = builder.build();
        source.output().send(message);
    }
}
