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

import javax.jms.Destination;

import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.context.Lifecycle;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.jms.JmsSendingMessageHandler;
import org.springframework.jms.core.JmsTemplate;

import static org.springframework.cloud.stream.binder.BinderHeaders.PARTITION_HEADER;

/**
 * Extension of {@link JmsSendingMessageHandler}, with partition awareness.
 *
 * <p>Whenever a destination name is set, it builds up a SpEL expression
 * using the partition index of the message to route it to the appropriate
 * partition.
 *
 * @author José Carlos Valero
 * @since 1.1
 */
public class PartitionAwareJmsSendingMessageHandler extends JmsSendingMessageHandler implements Lifecycle {


    private final ProducerProperties producerProperties;

    public PartitionAwareJmsSendingMessageHandler(JmsTemplate jmsTemplate,
                                                  ProducerProperties producerProperties) {
        super(jmsTemplate);
        this.producerProperties = producerProperties;
    }

    @Override
    public void setDestination(Destination destination) {
        throw new UnsupportedOperationException(
                "Destination is not supported. Please use destination name instead");
    }

    @Override
    public void setDestinationName(String destinationName) {
        if (!producerProperties.isPartitioned()) {
            super.setDestinationName(destinationName);
        } else {
            sanitizeSpelConstant(destinationName);
            Expression destinationExpression = new SpelExpressionParser()
                    .parseExpression(String.format("'%s-' + headers['%s']",
                            destinationName,PARTITION_HEADER));
            super.setDestinationExpression(destinationExpression);
        }
    }

    private void sanitizeSpelConstant(String spelConstant) {
        if (spelConstant.contains("'"))
            throw new IllegalArgumentException(
                    "The value %s contains an illegal character \"'\" ");
    }

    /*
    TODO: This has to be refactored, there is an open issue https://github.com/spring-cloud/spring-cloud-stream/issues/607
    that requires some love first
     */
    private boolean running;
    @Override
    public synchronized void start() {
        running = true;
    }

    @Override
    public synchronized void stop() {
        running = false;
    }

    @Override
    public synchronized boolean isRunning() {
        return running;
    }
}
