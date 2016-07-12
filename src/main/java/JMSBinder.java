import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.cloud.stream.binder.*;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.messaging.*;
import org.springframework.stereotype.Component;

import javax.jms.*;

@Component
public class JMSBinder extends AbstractBinder<MessageChannel, ConsumerProperties, ProducerProperties> {

    @Override
    protected Binding<MessageChannel> doBindConsumer(String name, String group, MessageChannel inputTarget, ConsumerProperties properties) {
        ConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        SimpleConsumer endpoint = new SimpleConsumer(factory, inputTarget);
        DefaultBinding<MessageChannel> binding = new DefaultBinding<>(name, group, inputTarget, endpoint);
        endpoint.setBeanName("inbound." + name);
        endpoint.start();
        return binding;
    }

    @Override
    protected Binding<MessageChannel> doBindProducer(String name, MessageChannel outboundBindTarget, ProducerProperties properties) {
        ConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        EventDrivenConsumer consumer = new EventDrivenConsumer((SubscribableChannel) outboundBindTarget, new SimpleHandler(factory));
        DefaultBinding<MessageChannel> producerBinding = new DefaultBinding<>(name, null, outboundBindTarget, consumer);
        consumer.setBeanFactory(getBeanFactory());
        consumer.setBeanName("outbound." + name);
        consumer.afterPropertiesSet();
        consumer.start();
        return producerBinding;
    }

}
