import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

import javax.jms.*;

class SimpleHandler implements MessageHandler {

    private final Session session;
    private MessageProducer producer;
    private SerializingConverter converter = new SerializingConverter();

    SimpleHandler(Connection connection, String name) throws JMSException {
        this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = session.createQueue(name);
        this.producer = session.createProducer(queue);
    }

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        try {
            BytesMessage bytesMessage = session.createBytesMessage();
            byte[] output = converter.convert(message.getPayload());
            bytesMessage.writeBytes(output);
            this.producer.send(bytesMessage);
        } catch (JMSException e) {
            throw new MessagingException("JMS error forwarding message ", e);
        }
    }

}
