package org.springframework.cloud.stream.binder.jms.solace;

import com.solacesystems.jcsmp.*;
import com.solacesystems.jcsmp.impl.XMLContentMessageImpl;
import com.solacesystems.jcsmp.transaction.TransactedSession;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.solacesystems.jcsmp.JCSMPSession.FLAG_IGNORE_DOES_NOT_EXIST;
import static com.solacesystems.jcsmp.JCSMPSession.WAIT_FOR_CONFIRM;

public class SolaceTestUtils {

    public static final String DLQ_NAME = "#DEAD_MSG_QUEUE";
    public static final Queue DLQ = JCSMPFactory.onlyInstance().createQueue(DLQ_NAME);

    public static JCSMPSession createSession() {
        //TODO: Use Spring properties instead
        JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty("username", "admin");
        properties.setProperty("password", "admin");
        properties.setProperty("host", "192.168.99.101");

        try {
            return JCSMPFactory.onlyInstance().createSession(properties);
        } catch (InvalidPropertiesException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deprovisionDLQ() throws JCSMPException {
        createSession().deprovision(DLQ, WAIT_FOR_CONFIRM | FLAG_IGNORE_DOES_NOT_EXIST);
    }

    public static BytesXMLMessage waitForDeadLetter() {

        ConsumerFlowProperties consumerFlowProperties = new ConsumerFlowProperties();
        consumerFlowProperties.setEndpoint(DLQ);

        CountDownLatch latch = new CountDownLatch(1);
        CountingListener countingListener = new CountingListener(latch);

        waitForItToWork(5000, () -> {
            try {
                FlowReceiver consumer = createSession().createFlow(countingListener, consumerFlowProperties);
                consumer.start();
                countingListener.awaitExpectedMessages();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return countingListener.getMessages().get(0);
    }

    public static class CountingListener implements XMLMessageListener {
        private final CountDownLatch latch;

        private final List<JCSMPException> errors = new ArrayList<>();

        private final List<String> payloads = new ArrayList<>();

        public List<BytesXMLMessage> getMessages() {
            return messages;
        }

        private final List<BytesXMLMessage> messages = new ArrayList<>();

        public CountingListener(CountDownLatch latch) {
            this.latch = latch;
        }

        public CountingListener(int expectedMessages) {
            this.latch = new CountDownLatch(expectedMessages);
        }

        @Override
        public void onReceive(BytesXMLMessage bytesXMLMessage) {
            payloads.add(new String(((XMLContentMessageImpl) bytesXMLMessage).getXMLContent()));
            messages.add(bytesXMLMessage);

            long count = latch.getCount();

            if (count % 1000 == 0) {
                System.out.println(String.format("Message %s", count));
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            latch.countDown();
        }

        @Override
        public void onException(JCSMPException e) {
            errors.add(e);
        }

        void awaitExpectedMessages() throws InterruptedException {
            latch.await(2, TimeUnit.SECONDS);
        }

        List<JCSMPException> getErrors() {
            return errors;
        }

        List<String> getPayloads() {
            return payloads;
        }
    }

    public static class FailingListener implements XMLMessageListener {

        private TransactedSession transactedSession;

        public FailingListener(TransactedSession transactedSession) {

            this.transactedSession = transactedSession;
        }

        @Override
        public void onReceive(BytesXMLMessage bytesXMLMessage) {
            try {
                transactedSession.rollback();
            } catch (JCSMPException e) {
                e.printStackTrace();
            }
            throw new RuntimeException("You shall not pass");
        }

        @Override
        public void onException(JCSMPException e) {
//            throw new RuntimeException("You shall not pass", e);
        }

    }

    public static void waitFor(Runnable assertion) {
        waitFor(1000, assertion);
    }

    public static void waitFor(int millis, Runnable assertion) {
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
            try {
                Thread.sleep(millis / 10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void waitForItToWork(int millis, Runnable assertion) {
        long endTime = System.currentTimeMillis() + millis;

        while (true) {
            try {
                assertion.run();
                return;
            } catch (Exception e) {
                if (System.currentTimeMillis() > endTime) {
                    throw e;
                }
            }
            try {
                Thread.sleep(millis / 10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
