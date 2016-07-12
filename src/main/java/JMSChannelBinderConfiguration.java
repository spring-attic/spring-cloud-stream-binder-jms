import org.springframework.context.annotation.Bean;

public class JMSChannelBinderConfiguration {

    @Bean
    JMSBinder jmsMessageChannelBinder() {
        return new JMSBinder();
    }
}
