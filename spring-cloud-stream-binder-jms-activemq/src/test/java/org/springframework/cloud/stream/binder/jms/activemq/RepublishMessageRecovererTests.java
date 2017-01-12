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

package org.springframework.cloud.stream.binder.jms.activemq;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.cloud.stream.binder.jms.spi.QueueProvisioner;
import org.springframework.cloud.stream.binder.jms.test.ActiveMQTestUtils;
import org.springframework.cloud.stream.binder.jms.utils.RepublishMessageRecoverer;
import org.springframework.integration.jms.DefaultJmsHeaderMapper;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RepublishMessageRecovererTests {

    public static final String DEAD_LETTER_QUEUE = "dead-letter-queue";
    static RepublishMessageRecoverer target;
    static RepublishMessageRecoverer additionalHeadersTarget;
    static JmsTemplate jmsTemplate;
    static QueueProvisioner queueProvisioner = mock(QueueProvisioner.class);
    private Message message = createMessage(ImmutableMap.of("fancy", "header"));
    private final String exceptionMessage = "I am an unhappy exception";
    private Throwable cause = new RuntimeException(exceptionMessage);

    @BeforeClass
    public static void initTests() throws Exception {
        ActiveMQConnectionFactory connectionFactory = ActiveMQTestUtils.startEmbeddedActiveMQServer();

        jmsTemplate = new JmsTemplate(connectionFactory);
        jmsTemplate.setDefaultDestinationName("my-fancy-queue");
        target = new RepublishMessageRecoverer(queueProvisioner, jmsTemplate, new DefaultJmsHeaderMapper());
        additionalHeadersTarget = new AdditionalHeadersMessageRecoverer(queueProvisioner, jmsTemplate);
    }

    @Before
    public void setUp() throws Exception {
        Mockito.reset(queueProvisioner);
        when(queueProvisioner.provisionDeadLetterQueue()).thenReturn(DEAD_LETTER_QUEUE);
    }

    @Test
    public void recover_provisionsDeadLetterQueue() throws Exception {
        target.recover(createMessage(ImmutableMap.of("fancy", "header")), cause);
        message = jmsTemplate.receive(DEAD_LETTER_QUEUE);

        verify(queueProvisioner, times(1)).provisionDeadLetterQueue();
    }

    @Test
    public void recover_addsStacktraceToMessageHeaders() throws Exception {
        target.recover(message, cause);
        message = jmsTemplate.receive(DEAD_LETTER_QUEUE);

        assertThat(message.getStringProperty(RepublishMessageRecoverer.X_EXCEPTION_STACKTRACE), containsString(exceptionMessage));
    }

    @Test
    public void recover_whenCauseHasCause_addsSubcauseMessageToHeaders() throws Exception {
        Throwable nestedCause = new RuntimeException("I am the parent", cause);

        target.recover(message, nestedCause);
        message = jmsTemplate.receive(DEAD_LETTER_QUEUE);

        assertThat(message.getStringProperty(RepublishMessageRecoverer.X_EXCEPTION_MESSAGE), is(exceptionMessage));

    }

    @Test
    public void recover_addsCauseMessageToHeaders() throws Exception {
        target.recover(message, cause);
        message = jmsTemplate.receive(DEAD_LETTER_QUEUE);

        assertThat(message.getStringProperty(RepublishMessageRecoverer.X_EXCEPTION_MESSAGE), is(exceptionMessage));
    }

    @Test
    public void recover_addsOriginalDestinationToMessageHeaders() throws Exception {
        target.recover(message, cause);
        message = jmsTemplate.receive(DEAD_LETTER_QUEUE);

        assertThat(message.getStringProperty(RepublishMessageRecoverer.X_ORIGINAL_QUEUE), is("queue://my-fancy-queue"));
    }

    @Test
    public void recover_retainsExistingMessageProperties() throws Exception {
        target.recover(message, cause);
        message = jmsTemplate.receive(DEAD_LETTER_QUEUE);

        assertThat(message.getStringProperty("fancy"), is("header"));
    }

    @Test
    public void recover_sendsMessageToDLQ() throws Exception {
        target.recover(message, cause);

        Object deadLetter = jmsTemplate.receiveAndConvert(DEAD_LETTER_QUEUE);
        assertThat(deadLetter, Matchers.<Object>is("Amazing payload"));
    }

    @Test
    public void recover_whenAdditionalHeadersMethodProvided_addsDefinedHeaders() throws Exception {
        additionalHeadersTarget.recover(message, cause);
        message = jmsTemplate.receive(DEAD_LETTER_QUEUE);

        assertThat(message.getStringProperty("additional"), is("extra-header"));
    }

    private Message createMessage(final ImmutableMap<String, String> headers) {
        jmsTemplate.send(new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                TextMessage textMessage = session.createTextMessage("Amazing payload");
                ImmutableSet<Map.Entry<String, String>> entries = headers.entrySet();
                for(Map.Entry<String, String> entry : entries){
                    try {
                        textMessage.setStringProperty(entry.getKey(), entry.getValue());
                    } catch (JMSException e) {
                        throw new RuntimeException(e);
                    }
                }
                return textMessage;
            }
        });
        return jmsTemplate.receive();
    }

    private static class AdditionalHeadersMessageRecoverer extends RepublishMessageRecoverer {
        public AdditionalHeadersMessageRecoverer(QueueProvisioner queueProvisioner, JmsTemplate jmsTemplate) {
            super(queueProvisioner, jmsTemplate, new DefaultJmsHeaderMapper());
        }

        @Override
        protected Map<? extends String, ? extends Object> additionalHeaders(Message message, Throwable cause) {
            return ImmutableMap.of("additional", "extra-header");
        }
    }

}
