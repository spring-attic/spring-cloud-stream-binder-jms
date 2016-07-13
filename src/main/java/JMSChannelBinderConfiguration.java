import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.context.annotation.Bean;

import javax.jms.JMSException;

public class JMSChannelBinderConfiguration {

    @Bean
    JMSBinder jmsMessageChannelBinder() throws JMSException {
        return new JMSBinder(new ActiveMQConnectionFactory("tcp://localhost:61616"));
    }
}
