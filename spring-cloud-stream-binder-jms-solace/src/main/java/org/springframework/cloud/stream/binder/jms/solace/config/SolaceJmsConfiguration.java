package org.springframework.cloud.stream.binder.jms.solace.config;

import com.solacesystems.jms.SolConnectionFactoryImpl;
import com.solacesystems.jms.property.JMSProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.jms.ConnectionFactory;
import java.util.Hashtable;

@Configuration
@EnableConfigurationProperties(SolaceConfigurationProperties.class)
public class SolaceJmsConfiguration {

    @ConditionalOnMissingBean(ConnectionFactory.class)
    @Bean
    public ConnectionFactory connectionFactory(SolaceConfigurationProperties config) throws Exception {
        JMSProperties properties = new JMSProperties((Hashtable<?, ?>) null);
        SolConnectionFactoryImpl solConnectionFactory = new SolConnectionFactoryImpl(properties);
        solConnectionFactory.setProperty("Host", config.getHost());
        solConnectionFactory.setProperty("Username", config.getUsername());
        solConnectionFactory.setProperty("Password", config.getPassword());
        return solConnectionFactory;
    }

}
