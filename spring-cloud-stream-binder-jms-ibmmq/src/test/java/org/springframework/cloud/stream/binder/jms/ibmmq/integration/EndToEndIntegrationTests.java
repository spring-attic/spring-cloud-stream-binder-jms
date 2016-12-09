package org.springframework.cloud.stream.binder.jms.ibmmq.integration;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.springframework.cloud.stream.binder.test.TestUtils.waitFor;

import java.util.List;

import org.junit.Test;
import org.springframework.cloud.stream.binder.jms.config.JmsBinderConfigurationProperties;
import org.springframework.cloud.stream.binder.jms.ibmmq.IBMMQQueueProvisioner;
import org.springframework.cloud.stream.binder.jms.ibmmq.IBMMQTestUtils;
import org.springframework.cloud.stream.binder.jms.utils.RepublishMessageRecoverer;
import org.springframework.cloud.stream.binder.test.integration.receiver.ReceiverApplication;
import org.springframework.cloud.stream.binder.test.integration.sender.SenderApplication;
import org.springframework.messaging.Message;

import com.google.common.collect.ImmutableMap;

/**
 * @author Donovan Muller
 */
public class EndToEndIntegrationTests extends
		org.springframework.cloud.stream.binder.test.integration.EndToEndIntegrationTests {

	public EndToEndIntegrationTests() throws Exception {
		super(new IBMMQQueueProvisioner(IBMMQTestUtils.createConnectionFactory(),
				IBMMQTestUtils.getIBMMQProperties(),
				new JmsBinderConfigurationProperties()),
				IBMMQTestUtils.createConnectionFactory());
	}

	/**
	 * Overridden to account for the invalid header name value. According to the JMS Spec,
	 * header names must be valid Java identifier part characters.
	 *
	 * See http://download.oracle.com/otn-pub/jcp/jms-2_0-fr-eval-spec/JMS20.pdf
	 */
	@Test
	@Override
	public void scs_whenMessageIsSentToDLQ_stackTraceAddedToHeaders() throws Exception {
		SenderApplication.Sender sender = createSender();
		createReceiver(randomGroupArg1, MAX_ATTEMPTS_1);

		String deadLetterQueue = queueProvisioner.provisionDeadLetterQueue();

		sender.send(ReceiverApplication.Receiver.EXCEPTION_REQUEST);

		javax.jms.Message message = jmsTemplate.receive(deadLetterQueue);
		assertThat(message, notNullValue());

		String stacktrace = message.getStringProperty(
				RepublishMessageRecoverer.X_EXCEPTION_STACKTRACE.replaceAll("-", "_"));
		assertThat(stacktrace,
				containsString(ReceiverApplication.Receiver.REQUESTED_EXCEPTION));
	}

	/**
	 * Overridden to account for the invalid header name value. According to the JMS Spec,
	 * header names must be valid Java identifier part characters.
	 *
	 * See http://download.oracle.com/otn-pub/jcp/jms-2_0-fr-eval-spec/JMS20.pdf
	 */
	@Test
	public void scs_whenHeadersAreSpecified_headersArePassedThrough() throws Exception {
		SenderApplication.Sender sender = createSender();
		ReceiverApplication.Receiver receiver = createReceiver(randomGroupArg1);

		sender.send(MESSAGE_TEXTS[1],
				ImmutableMap.<String, Object> of(HEADER_KEY, HEADER_VALUE));

		final List<Message> messages = receiver.getHandledMessages();

		waitFor(new Runnable() {
			@Override
			public void run() {
				assertThat(messages,
						contains(allOf(hasProperty("payload", is(MESSAGE_TEXTS[1])),
								hasProperty("headers",
										hasEntry(HEADER_KEY.replaceAll("-", "_"),
												HEADER_VALUE)))));
			}
		});
	}

	@Override
	protected void deprovisionDLQ() throws Exception {
		IBMMQTestUtils.deprovisionDLQ();
	}

}
