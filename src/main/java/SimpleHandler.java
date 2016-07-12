import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

import javax.jms.*;

/**
 * Created by pivotal on 12/07/2016.
 */
class SimpleHandler implements MessageHandler {

    private ConnectionFactory factory;

    SimpleHandler(ConnectionFactory factory) {
        this.factory = factory;
    }

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        try {
            Connection producerConnection = factory.createConnection();
            try {
                Session session = producerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Queue queue = session.createQueue("test.queue");
                MessageProducer producer = session.createProducer(queue);
                javax.jms.Message msg = session.createTextMessage(message.getPayload().toString());
                producer.send(msg);
            } finally {
                producerConnection.close();
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
