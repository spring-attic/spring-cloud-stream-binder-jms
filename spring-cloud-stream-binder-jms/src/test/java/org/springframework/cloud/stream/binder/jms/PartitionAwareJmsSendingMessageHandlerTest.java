package org.springframework.cloud.stream.binder.jms;

import org.junit.Test;
import org.springframework.cloud.stream.binder.PartitionHandler;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import javax.jms.JMSException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.getField;

public class PartitionAwareJmsSendingMessageHandlerTest {

    private static final String PARTITION_HEADER = "partition";
    PartitionHandler partitionHandler = mock(PartitionHandler.class);
    JmsTemplate jmsTemplate = mock(JmsTemplate.class);
    JMSMessageChannelBinder parent = new JMSMessageChannelBinder(null, null, null);
    private ProducerProperties producerProperties = new ProducerProperties();
    private SpelExpressionParser parser = new SpelExpressionParser();

    public PartitionAwareJmsSendingMessageHandlerTest() throws JMSException {
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

        assertThat(ReflectionTestUtils.getField(target,"destinationName"), is("name"));
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