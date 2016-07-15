import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

public class JMSChannelBinderConfiguration {

    @Bean
    JMSMessageChannelBinder jmsMessageChannelBinder(JmsTemplate template, ConnectionFactory connectionFactory) throws JMSException {
        return new JMSMessageChannelBinder(connectionFactory, template);
    }

}
