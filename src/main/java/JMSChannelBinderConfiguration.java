import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

public class JMSChannelBinderConfiguration {

    @Autowired
    ConnectionFactory connectionFactory;

    @Bean
    JMSMessageChannelBinder jmsMessageChannelBinder() throws JMSException {
        return new JMSMessageChannelBinder(connectionFactory);
    }

}
