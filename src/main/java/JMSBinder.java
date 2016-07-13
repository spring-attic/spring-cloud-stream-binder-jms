import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.cloud.stream.binder.*;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.messaging.*;
import org.springframework.stereotype.Component;

import javax.jms.*;

@Component
public class JMSBinder extends AbstractBinder<MessageChannel, ConsumerProperties, ProducerProperties> {

    private Connection consumerConnection;
    private Connection producerConnection;

    public JMSBinder(ConnectionFactory factory) throws JMSException {
        this.consumerConnection = factory.createConnection();
        this.producerConnection = factory.createConnection();
        consumerConnection.start();
        logger.info(String.format("JMS started consumer connection %s", consumerConnection.getClientID()));
        producerConnection.start();
        logger.info(String.format("JMS started producer connection %s", producerConnection.getClientID()));
    }

    @Override
    protected Binding<MessageChannel> doBindConsumer(String name, String group, MessageChannel inputTarget, ConsumerProperties properties) {
        MessageProducerSupport endpoint = new SimpleConsumer(consumerConnection, name);
        endpoint.setOutputChannel(inputTarget);
        DefaultBinding<MessageChannel> binding = new DefaultBinding<>(name, group, inputTarget, endpoint);
        endpoint.setBeanName("inbound." + name);
        endpoint.start();
        return binding;
    }

    @Override
    protected Binding<MessageChannel> doBindProducer(String name, MessageChannel outboundBindTarget, ProducerProperties properties) {
        DefaultBinding<MessageChannel> binding = null;
        try {
            MessageHandler handler = new SimpleHandler(producerConnection, name);
            AbstractEndpoint consumer = new EventDrivenConsumer((SubscribableChannel) outboundBindTarget, handler);
            binding = new DefaultBinding<>(name, null, outboundBindTarget, consumer);
            consumer.setBeanFactory(getBeanFactory());
            consumer.setBeanName("outbound." + name);
            consumer.afterPropertiesSet();
            consumer.start();
        } catch (JMSException e) {
            logger.error(String.format("JMS error binding producer: %s", e.getMessage()), e);
        }
        return binding;
    }

}
