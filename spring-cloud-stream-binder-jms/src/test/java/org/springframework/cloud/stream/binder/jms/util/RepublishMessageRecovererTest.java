package org.springframework.cloud.stream.binder.jms.util;

import com.google.common.collect.ImmutableMap;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.store.kahadb.KahaDBPersistenceAdapter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.cloud.stream.binder.jms.QueueProvisioner;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.io.File;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.cloud.stream.binder.jms.util.RepublishMessageRecoverer.*;

public class RepublishMessageRecovererTest {

    static RepublishMessageRecoverer target;
    static RepublishMessageRecoverer additionalHeadersTarget;
    static JmsTemplate jmsTemplate ;
    static QueueProvisioner queueProvisioner = mock(QueueProvisioner.class);
    private final Message message = createMessage(ImmutableMap.of("fancy", "header"));

    @BeforeClass
    public static void initTests() throws Exception{

        BrokerService broker = new BrokerService();

        File testDataDir = new File("target/activemq-data/QueuePurgeTest");
        broker.setDataDirectoryFile(testDataDir);
        broker.setUseJmx(true);
        broker.setDeleteAllMessagesOnStartup(true);
        KahaDBPersistenceAdapter persistenceAdapter = new KahaDBPersistenceAdapter();
        persistenceAdapter.setDirectory(new File(testDataDir, "kahadb"));
        broker.setPersistenceAdapter(persistenceAdapter);
        broker.addConnector("tcp://localhost:0");
        broker.start();
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(broker.getTransportConnectors().get(0).getConnectUri().toString());

        jmsTemplate = new JmsTemplate(connectionFactory);
        jmsTemplate.setDefaultDestinationName("my-fancy-queue");
        target = new RepublishMessageRecoverer(queueProvisioner, jmsTemplate);
        additionalHeadersTarget = new AdditionalHeadersMessageRecoverer(queueProvisioner, jmsTemplate);
    }

    @Before
    public void setUp() throws Exception {
        Mockito.reset(queueProvisioner);
        when(queueProvisioner.provisionDeadLetterQueue()).thenReturn("dead-letter-queue");
    }

    private final String exceptionMessage = "I am an unhappy exception";
    private Throwable cause = new RuntimeException(exceptionMessage);



    @Test
    public void recover_provisionsDeadLetterQueue() throws Exception {
        target.recover(createMessage(ImmutableMap.of("fancy", "header")), cause);

        verify(queueProvisioner, times(1)).provisionDeadLetterQueue();
    }

    @Test
    public void recover_addsStacktraceToMessageHeaders() throws Exception {
        target.recover(message, cause);

        assertThat(message.getStringProperty(X_EXCEPTION_STACKTRACE), containsString(exceptionMessage));
    }

    @Test
    public void recover_whenCauseHasCause_addsSubcauseMessageToHeaders() throws Exception {
        Throwable nestedCause = new RuntimeException("I am the parent", cause);

        target.recover(message, nestedCause);

        assertThat(message.getStringProperty(X_EXCEPTION_MESSAGE), is(exceptionMessage));

    }

    @Test
    public void recover_addsCauseMessageToHeaders() throws Exception {
        target.recover(message, cause);

        assertThat(message.getStringProperty(X_EXCEPTION_MESSAGE), is(exceptionMessage));
    }

    @Test
    public void recover_addsOriginalDestinationToMessageHeaders() throws Exception {
        target.recover(message, cause);

        assertThat(message.getStringProperty(X_ORIGINAL_QUEUE), is("queue://my-fancy-queue"));
    }

    @Test
    public void recover_retainsExistingMessageProperties() throws Exception {
        target.recover(message, cause);

        assertThat(message.getStringProperty("fancy"), is("header"));
    }

    @Test
    public void recover_sendsMessageToDLQ() throws Exception {
        target.recover(message, cause);

        Object deadLetter = jmsTemplate.receiveAndConvert("dead-letter-queue");
        assertThat(deadLetter, is("Amazing payload"));
    }

    @Test
    public void recover_whenAdditionalHeadersMethodProvided_addsDefinedHeaders() throws Exception {
        additionalHeadersTarget.recover(message, cause);

        assertThat(message.getStringProperty("additional"), is("extra-header"));
    }

    private static class AdditionalHeadersMessageRecoverer extends RepublishMessageRecoverer{
        public AdditionalHeadersMessageRecoverer(QueueProvisioner queueProvisioner, JmsTemplate jmsTemplate) {
            super(queueProvisioner, jmsTemplate);
        }

        @Override
        protected Map<? extends String, ? extends Object> additionalHeaders(Message message, Throwable cause) {
            return ImmutableMap.of("additional", "extra-header");
        }
    }

    private Message createMessage(final ImmutableMap<String, String> headers) {
        jmsTemplate.send(new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                TextMessage textMessage = session.createTextMessage("Amazing payload");
                headers.forEach((key, value) -> {
                    try {
                        textMessage.setStringProperty(key, value);
                    } catch (JMSException e) {
                        throw new RuntimeException(e);
                    }
                });
                return textMessage;
            }
        });
        return jmsTemplate.receive();
    }

}