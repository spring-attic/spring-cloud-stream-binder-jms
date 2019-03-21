/*
 *  Copyright 2002-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.stream.binder.jms;

import javax.jms.JMSException;

import org.junit.Test;

import org.springframework.cloud.stream.binder.PartitionHandler;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.util.ReflectionTestUtils.getField;

public class PartitionAwareJmsSendingMessageHandlerTests {

    private static final String PARTITION_HEADER = "partition";
    PartitionHandler partitionHandler = mock(PartitionHandler.class);
    JmsTemplate jmsTemplate = mock(JmsTemplate.class);
    JMSMessageChannelBinder parent = new JMSMessageChannelBinder(null, null, null);
    private ProducerProperties producerProperties = new ProducerProperties();
    private SpelExpressionParser parser = new SpelExpressionParser();

    public PartitionAwareJmsSendingMessageHandlerTests() throws JMSException {
    }

    @Test
    public void setDestinationName_whenDeterminePartitionIsNotNull_setsDestinationExpression() throws Exception {
        JMSMessageChannelBinder.PartitionAwareJmsSendingMessageHandler target = parent.new PartitionAwareJmsSendingMessageHandler(jmsTemplate, producerProperties, partitionHandler, PARTITION_HEADER);
        producerProperties.setPartitionKeyExpression(parser.parseExpression("whatever"));
        target.setDestinationName("name");

        ExpressionEvaluatingMessageProcessor expressionProcessor = (ExpressionEvaluatingMessageProcessor) getField(target, "destinationExpressionProcessor");
        Expression expression = (Expression) ReflectionTestUtils.getField(expressionProcessor, "expression");
        assertThat(expression.getExpressionString(), is("'name-' + headers['partition']"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setDestinationName_whenIllegalCharactersAsDestinationName_throwsException() throws Exception {
        JMSMessageChannelBinder.PartitionAwareJmsSendingMessageHandler target = parent.new PartitionAwareJmsSendingMessageHandler(jmsTemplate, producerProperties, partitionHandler, PARTITION_HEADER);
        producerProperties.setPartitionKeyExpression(parser.parseExpression("whatever"));

        target.setDestinationName("na'me");
    }

    @Test
    public void setDestinationName_whenNotPartitioned_setsRawDestinationName() throws Exception {
        JMSMessageChannelBinder.PartitionAwareJmsSendingMessageHandler target = parent.new PartitionAwareJmsSendingMessageHandler(jmsTemplate, producerProperties, partitionHandler, PARTITION_HEADER);

        target.setDestinationName("name");

        assertThat(ReflectionTestUtils.getField(target, "destinationName"), is("name"));
    }


    @Test(expected = IllegalArgumentException.class)
    public void partitionAwareEvaluatingMessageProcessor_whenIllegalCharactersAsHeaderName_throwsException() throws Exception {
        parent.new PartitionAwareJmsSendingMessageHandler(jmsTemplate, producerProperties, partitionHandler, "P'ARTITION_HEADER");
    }


    @Test(expected = UnsupportedOperationException.class)
    public void setDestination_throwsException() throws Exception {
        JMSMessageChannelBinder.PartitionAwareJmsSendingMessageHandler target = parent.new PartitionAwareJmsSendingMessageHandler(jmsTemplate, producerProperties, partitionHandler, PARTITION_HEADER);
        producerProperties.setPartitionKeyExpression(parser.parseExpression("whatever"));

        target.setDestination(null);
    }

}