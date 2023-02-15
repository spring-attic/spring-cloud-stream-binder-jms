/*
 *  Copyright 2002-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *		https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.stream.binder.jms.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.jms.JmsHeaderMapper;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.messaging.MessageHeaders;

/**
 * {@link MessageRecoverer} implementation that republishes recovered messages
 * to a specified queue with the exception information stored in the message
 * headers.
 *
 * <p>It allows further customization through
 * {@link RepublishMessageRecoverer#additionalHeaders(Message, Throwable)}.
 *
 * @author Jack Galilee
 * @author Jonathan Sharpe
 * @author Joseph Taylor
 * @author José Carlos Valero
 * @since 1.1
 */
public class RepublishMessageRecoverer implements MessageRecoverer {

	public static final String X_EXCEPTION_MESSAGE = "x_exception_message";
	public static final String X_ORIGINAL_QUEUE = "x_original_queue";
	public static final String X_EXCEPTION_STACKTRACE = "x_exception_stacktrace";

	private final Log logger = LogFactory.getLog(getClass());

	private final JmsTemplate jmsTemplate;
	private final JmsHeaderMapper mapper;

	public RepublishMessageRecoverer(JmsTemplate jmsTemplate, JmsHeaderMapper mapper) {
		this.jmsTemplate = jmsTemplate;
		this.mapper = mapper;
	}

	@Override
	public void recover(Message undeliveredMessage, String dlq, Throwable cause) {
		//String deadLetterQueueName = destination.getDlq();

		MessageConverter converter = new SimpleMessageConverter();
		Object payload = null;

		try {
			payload = converter.fromMessage(undeliveredMessage);
		} catch (JMSException e) {
			logger.error("The message payload could not be retrieved. It will be lost.", e);
		}

		final Map<String, Object> headers = mapper.toHeaders(undeliveredMessage);
		headers.put(X_EXCEPTION_STACKTRACE, getStackTraceAsString(cause));
		headers.put(X_EXCEPTION_MESSAGE, cause.getCause() != null ? cause.getCause().getMessage() : cause.getMessage());
		try {
			headers.put(X_ORIGINAL_QUEUE, undeliveredMessage.getJMSDestination().toString());
		} catch (JMSException e) {
			logger.error("The message destination could not be retrieved", e);
		}
		Map<? extends String, ? extends Object> additionalHeaders = additionalHeaders(undeliveredMessage, cause);
		if (additionalHeaders != null) {
			headers.putAll(additionalHeaders);
		}

		jmsTemplate.convertAndSend(dlq, payload, new MessagePostProcessor() {
			@Override
			public Message postProcessMessage(Message message) throws JMSException {
				mapper.fromHeaders(new MessageHeaders(headers), message);
				return message;
			}
		});

	}

	/**
	 * Provide additional headers for the message.
	 *
	 * <p>Subclasses can override this method to add more headers to the
	 * undelivered message when it is republished to the DLQ.
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
