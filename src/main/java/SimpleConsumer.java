import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import javax.jms.*;

/**
 * Created by pivotal on 12/07/2016.
 */
class SimpleConsumer extends AbstractEndpoint {
    private volatile boolean shouldContinue;
    private ConnectionFactory factory;
    private MessageChannel inputTarget;
    private Connection connection;
    private Session session;
    private MessageConsumer consumer;
    private Queue queue;
    private Thread thread;

    SimpleConsumer(ConnectionFactory factory, MessageChannel inputTarget) {
        this.factory = factory;
        this.inputTarget = inputTarget;
        this.shouldContinue = true;
    }

    @Override
    protected void doStart() {
        try {
            connection = factory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            queue = session.createQueue("test.queue");
            consumer = session.createConsumer(queue);
            connection.start();
            thread = new Thread(() -> {
                try {
                    while (shouldContinue) {
                        Message msg = consumer.receive(100);
                        if (msg == null) continue;
                        TextMessage tm = (TextMessage) msg;
                        inputTarget.send(MessageBuilder.withPayload(tm.getText()).build());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            thread.start();
        } catch (Exception e) {
            e.printStackTrace();
            shouldContinue = false;
            this.stop();
        }
    }

    @Override
    protected void doStop() {
        shouldContinue = false;
        try {
            this.consumer.close();
            this.session.close();
            this.connection.stop();
            this.thread.join();
        } catch (JMSException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
