package org.springframework.cloud.stream.binder.jms.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.stream.binder.jms.QueueProvisioner;
import org.springframework.integration.jms.DefaultJmsHeaderMapper;
import org.springframework.integration.jms.JmsHeaderMapper;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.MessageHeaders;

import javax.jms.JMSException;
import javax.jms.Message;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

/**
 * {@link MessageRecoverer} implementation that republishes recovered messages
 * to a specified queue with the exception stack trace stored in the
 * message header x-exception.
 * <p>
 */
public class RepublishMessageRecoverer implements MessageRecoverer {

    public static final String X_EXCEPTION_STACKTRACE = "x-exception-stacktrace";

    public static final String X_EXCEPTION_MESSAGE = "x-exception-message";

    public static final String X_ORIGINAL_QUEUE = "x-original-queue";

    private final Log logger = LogFactory.getLog(getClass());

    private final QueueProvisioner queueProvisioner;
    private final JmsTemplate jmsTemplate;

    public RepublishMessageRecoverer(QueueProvisioner queueProvisioner, JmsTemplate jmsTemplate) {
        this.queueProvisioner = queueProvisioner;
        this.jmsTemplate = jmsTemplate;
    }

    @Override
    public void recover(Message message, Throwable cause) {
        String deadLetterQueueName = queueProvisioner.provisionDeadLetterQueue();

        JmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
        Map<String, Object> headers = mapper.toHeaders(message);
        headers.put(X_EXCEPTION_STACKTRACE, getStackTraceAsString(cause));
        headers.put(X_EXCEPTION_MESSAGE, cause.getCause() != null ? cause.getCause().getMessage() : cause.getMessage());
        try {
            headers.put(X_ORIGINAL_QUEUE, message.getJMSDestination().toString());
        } catch (JMSException e) {
            logger.error("The message destination could not be retrieved", e);
        }
        Map<? extends String, ? extends Object> additionalHeaders = additionalHeaders(message, cause);
        if (additionalHeaders != null) {
            headers.putAll(additionalHeaders);
        }
        try {
            message.clearProperties();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
        mapper.fromHeaders(new MessageHeaders(headers), message);

        jmsTemplate.convertAndSend(deadLetterQueueName, message);

    }

    /**
     * Subclasses can override this method to add more headers to the republished message.
     *
     * @param message The failed message.
     * @param cause   The cause.
     * @return A {@link Map} of additional headers to add.
     */
    protected Map<? extends String, ? extends Object> additionalHeaders(Message message, Throwable cause) {
        return null;
    }

    private String getStackTraceAsString(Throwable cause) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter, true);
        cause.printStackTrace(printWriter);
        return stringWriter.getBuffer().toString();
    }

}
