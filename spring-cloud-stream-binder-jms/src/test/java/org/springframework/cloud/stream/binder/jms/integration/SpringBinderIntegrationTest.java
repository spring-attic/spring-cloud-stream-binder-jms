package org.springframework.cloud.stream.binder.jms.integration;

import org.springframework.messaging.Message;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

public abstract class SpringBinderIntegrationTest {

    public void waitFor(Runnable assertion) throws InterruptedException {
        waitFor(1000, assertion);
    }

    public void waitFor(int millis, Runnable assertion) throws InterruptedException {
        long endTime = System.currentTimeMillis() + millis;

        while (true) {
            try {
                assertion.run();
                return;
            } catch (AssertionError e) {
                if (System.currentTimeMillis() > endTime) {
                    throw e;
                }
            }
            Thread.sleep(millis / 10);
        }
    }

    List<? extends Object> extractPayload(List<Message> messages) {
        return messages.stream().map(Message::getPayload).collect(Collectors.toList());
    }

    public static class SerializableTest implements Serializable {
        public final String value;

        public SerializableTest(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "SerializableTest<" + value + ">";
        }
    }
}
