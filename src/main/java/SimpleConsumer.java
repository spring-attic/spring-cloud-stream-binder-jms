import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import javax.jms.*;

class SimpleConsumer extends MessageProducerSupport implements MessageListener {

    private Connection connection;
    private Session session;
    private String queueName;
    private MessageConsumer consumer;
    private DeserializingConverter converter = new DeserializingConverter();

    SimpleConsumer(Connection connection, String name) {
        this.connection = connection;
        this.queueName = name;
    }

    @Override
    protected void doStart() {
        try {
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            logger.info(String.format("JMS Connection %s creating queue with name '%s'", connection.getClientID(), queueName));
            Queue queue = session.createQueue(this.queueName);
            consumer = session.createConsumer(queue);
            consumer.setMessageListener(this);
        } catch (JMSException e) {
            logger.error(String.format("JMS Error starting consumer: %s", e.getMessage()), e);
            this.stop();
        }
    }

    @Override
    protected void doStop() {
        try {
            this.consumer.close();
        } catch (JMSException e) {
            logger.error(String.format("JMS Error stopping consumer: %s", e.getMessage()), e);
        }
        try {
            this.session.close();
        } catch (JMSException e) {
            logger.error(String.format("JMS Error stopping consumer session: %s", e.getMessage()), e);
        }
    }

    @Override
    public void onMessage(javax.jms.Message message) {
        try {
            BytesMessage bytesMessage = (BytesMessage) message;
            long bodyLength = bytesMessage.getBodyLength();
            byte[] bytes = new byte[(int) bodyLength];
            bytesMessage.readBytes(bytes);
            Object result = converter.convert(bytes);
            Message<?> outboundMessage = MessageBuilder.withPayload(result).build();
            this.sendMessage(outboundMessage);
        } catch (JMSException e) {
            logger.error(String.format("JMS Error reading consumer message: %s", e.getMessage()), e);
        }
    }

}
