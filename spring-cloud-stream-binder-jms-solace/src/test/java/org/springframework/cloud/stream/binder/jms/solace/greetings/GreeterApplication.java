package org.springframework.cloud.stream.binder.jms.solace.greetings;

import com.solacesystems.jms.SolConnectionFactory;
import com.solacesystems.jms.SolConnectionFactoryImpl;
import com.solacesystems.jms.SolJmsUtility;
import com.solacesystems.jms.property.JMSProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binder.jms.solace.config.SolaceJmsConfiguration;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;

@SpringBootApplication(exclude = {EmbeddedServletContainerAutoConfiguration.class, WebMvcAutoConfiguration.class, JmxAutoConfiguration.class})
@Import(SolaceJmsConfiguration.class)
@EnableBinding(Sink.class)
public class GreeterApplication {

    public static void main(String[] args) {
        SpringApplication.run(GreeterApplication.class, args);
    }

}
