package org.springframework.cloud.stream.binder.jms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.stream.binder.Binder;
import org.springframework.cloud.stream.config.codec.kryo.KryoCodecAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnMissingBean(Binder.class)
@Import({JMSChannelBinderConfiguration.class, KryoCodecAutoConfiguration.class})
public class JMSAutoConfiguration {

}
