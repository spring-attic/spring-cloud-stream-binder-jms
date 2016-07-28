package org.springframework.cloud.stream.binder.jms.solace.config;

import com.solacesystems.jms.SolConnectionFactoryImpl;
import com.solacesystems.jms.property.JMSProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JndiConnectionFactoryAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.binder.jms.QueueProvisioner;
import org.springframework.cloud.stream.binder.jms.solace.SolaceQueueProvisioner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.jms.ConnectionFactory;
import java.util.Hashtable;

@Configuration
@AutoConfigureBefore(JmsAutoConfiguration.class)
@AutoConfigureAfter({ JndiConnectionFactoryAutoConfiguration.class })
@ConditionalOnClass({ ConnectionFactory.class, SolConnectionFactoryImpl.class })
@ConditionalOnMissingBean(ConnectionFactory.class)
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
        //Disabling direct transport allows JMS to use transacted sessions. Enabling at the same time
        //DLQ routing if maxRedeliveryAttempts is set
        solConnectionFactory.setDirectTransport(false);
        return solConnectionFactory;
    }

    @Bean
    public QueueProvisioner solaceQueueProvisioner(SolaceConfigurationProperties solaceConfigurationProperties) {
        return new SolaceQueueProvisioner(solaceConfigurationProperties);
    }

}
