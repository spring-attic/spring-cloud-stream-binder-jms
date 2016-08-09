/*
 *  Copyright 2002-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.stream.binder.jms.utils;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.integration.jms.JmsSendingMessageHandler;
import org.springframework.jms.core.JmsTemplate;

import static org.springframework.cloud.stream.binder.BinderHeaders.PARTITION_HEADER;

/**
 * Factory to create JMS message handlers
 *
 * @author José Carlos Valero
 * @since 1.1
 */
public class JmsSendingMessageHandlerFactory {

    private final JmsTemplate template;
    private final BeanFactory beanFactory;

    public JmsSendingMessageHandlerFactory(JmsTemplate template,
                                           BeanFactory beanFactory) {
        this.template = template;
        this.beanFactory = beanFactory;
    }

    public JmsSendingMessageHandler build(String name,
                                          ProducerProperties producerProperties) {
        template.setPubSubDomain(true);
        JmsSendingMessageHandler handler = new PartitionAwareJmsSendingMessageHandler(
                this.template,
                producerProperties);
        handler.setDestinationName(name);
        handler.setBeanFactory(beanFactory);
        handler.afterPropertiesSet();

        return handler;
    }
}
