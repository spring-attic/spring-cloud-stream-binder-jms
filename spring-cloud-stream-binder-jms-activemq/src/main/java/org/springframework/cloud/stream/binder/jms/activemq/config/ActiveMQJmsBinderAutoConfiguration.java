/*
 *  Copyright 2002-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.stream.binder.jms.activemq.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.jms.JndiConnectionFactoryAutoConfiguration;
import org.springframework.cloud.stream.binder.jms.config.JmsBinderAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration class to enable the ActiveMQ JMS binder.
 *
 * @author Tim Ysewyn
 * @since 2.0
 */
@Configuration
@AutoConfigureAfter({JndiConnectionFactoryAutoConfiguration.class})
@AutoConfigureBefore({JmsBinderAutoConfiguration.class})
@Import(ActiveMQJmsConfiguration.class)
public class ActiveMQJmsBinderAutoConfiguration {

}
