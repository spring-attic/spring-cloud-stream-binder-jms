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

package org.springframework.cloud.stream.binder.test.integration;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import javax.jms.ConnectionFactory;
import org.apache.commons.lang.ArrayUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.cloud.stream.binder.jms.config.JmsConsumerProperties;
import org.springframework.cloud.stream.binder.jms.utils.RepublishMessageRecoverer;
import org.springframework.cloud.stream.binder.test.integration.receiver.ReceiverApplication;
import org.springframework.cloud.stream.binder.test.integration.receiver.ReceiverApplication.Receiver;
import org.springframework.cloud.stream.binder.test.integration.sender.SenderApplication;
import org.springframework.cloud.stream.binder.test.integration.sender.SenderApplication.Sender;
import org.springframework.cloud.stream.converter.CompositeMessageConverterFactory;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.CompositeMessageConverter;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.springframework.cloud.stream.binder.test.TestUtils.waitFor;

public abstract class EndToEndIntegrationTests {

	private final CompositeMessageConverter messageConverter;
	private List<ConfigurableApplicationContext> startedContexts = new ArrayList<>();
	private String destination;

	protected static final String[] MESSAGE_TEXTS = {
			"first message test content",
			"second message text content",
			"third message text content",
			"fourth message text content"
	};
	protected static final String HEADER_KEY = "some_custom_header_key";
	protected static final String HEADER_VALUE = "some-custom-header-value";
	protected static final String INVALID_HEADER_KEY = "some-invalid-custom-header-key";
	protected static final String OUTPUT_DESTINATION_FORMAT = "--spring.cloud.stream.bindings.output.destination=%s";
	protected static final String INPUT_DESTINATION_FORMAT = "--spring.cloud.stream.bindings.input.destination=%s";
	protected static final String INPUT_GROUP_FORMAT = "--spring.cloud.stream.bindings.input.group=%s";
	protected static final String MAX_ATTEMPTS_1 = "--spring.cloud.stream.bindings.input.consumer.maxAttempts=1";
	protected static final String RETRY_BACKOFF_50MS = "--spring.cloud.stream.bindings.input.consumer.backOffInitialInterval=50";
	protected static final String RETRY_BACKOFF_1X = "--spring.cloud.stream.bindings.input.consumer.backOffMultiplier=1";
	protected static final String REQUIRED_GROUPS_FORMAT = "--spring.cloud.stream.bindings.output.producer.requiredGroups=%s";
	protected static final String DLQ_NAME = "dlqtest";
	protected static final String DLQ_CONSUMER_PROPERTY = "--spring.cloud.stream.jms.bindings.input.consumer.dlqName=" + DLQ_NAME;
	protected final JmsTemplate jmsTemplate;
	protected final ProvisioningProvider<ConsumerProperties, ProducerProperties> queueProvisioner;
	protected String randomGroupArg1;
	protected String randomGroupArg2;


	protected EndToEndIntegrationTests(ProvisioningProvider queueProvisioner, ConnectionFactory connectionFactory) {
		this.queueProvisioner = queueProvisioner;
		this.jmsTemplate = new JmsTemplate(connectionFactory);
		this.messageConverter = new CompositeMessageConverterFactory().getMessageConverterForAllRegistered();
	}

	@Before
	public void configureGroupsAndDestinations() throws Exception {
		deprovisionDLQ();
		this.destination = getRandomName("destination");
		randomGroupArg1 = String.format(INPUT_GROUP_FORMAT, getRandomName("group1"));
		randomGroupArg2 = String.format(INPUT_GROUP_FORMAT, getRandomName("group2"));
	}

	@After
	public void stopProducerAndConsumers() throws Exception {
		for (ConfigurableApplicationContext startedContext : startedContexts) {
			startedContext.stop();
		}
		deprovisionDLQ();
	}

	protected void deprovisionDLQ() throws Exception {
		return;
	}

	/**
	 * Deals with scenarios where Stream apps are started with only default values.
	 * Which includes 'null' consumer groups.
	 */
	@Test
	public void scs_whenNoConsumerGroups_appsReceiveMessage() throws Exception {
		Sender sender = createSender();
		Receiver receiver = createReceiver();

		for (String messageText : MESSAGE_TEXTS) {
			sender.send(messageText);
		}

		final List<Message<Object>> messages = receiver.getHandledMessages();

		waitFor(() -> {
			assertThat(messages, hasSize(4));
			assertThat(EndToEndIntegrationTests.this.extractStringPayload(messages), containsInAnyOrder(MESSAGE_TEXTS));
		});
	}

	@Test
	public void scs_whenMultipleConsumerGroups_eachGroupGetsAllMessages() throws Exception {
		Sender sender = createSender();
		Receiver receiver = createReceiver(randomGroupArg1);
		Receiver receiver2 = createReceiver(randomGroupArg2);

		for (String messageText : MESSAGE_TEXTS) {
			sender.send(messageText);
		}

		final List<Message<Object>> otherReceivedMessages = receiver2.getHandledMessages();
		final List<Message<Object>> messages = receiver.getHandledMessages();

		waitFor(() -> {
			assertThat(messages, hasSize(4));
			assertThat(otherReceivedMessages, hasSize(4));
			assertThat(EndToEndIntegrationTests.this.extractStringPayload(messages), containsInAnyOrder(MESSAGE_TEXTS));
			assertThat(EndToEndIntegrationTests.this.extractStringPayload(otherReceivedMessages),
					containsInAnyOrder(MESSAGE_TEXTS));
		});
	}

	@Test
	public void scs_whenMultipleMembersOfSameConsumerGroup_groupOnlySeesEachMessageOnce() throws Exception {
		int messageCount = 40;

		Sender sender = createSender();
		Receiver receiver = createReceiver(randomGroupArg1);
		Receiver receiver2 = createReceiver(randomGroupArg1);

		CountDownLatch latch = new CountDownLatch(messageCount);
		receiver.setLatch(latch);
		receiver2.setLatch(latch);

		for (int i = 0; i < messageCount; i++) {
			sender.send(i);
		}

		boolean completed = latch.await(20, TimeUnit.SECONDS);
		assertThat("timed out waiting for all messages to arrive", completed, is(true));

		List<Message<Object>> otherReceivedMessages = receiver2.getHandledMessages();
		List<Message<Object>> messages = receiver.getHandledMessages();

		assertThat(otherReceivedMessages.size(), lessThan(messageCount));
		assertThat(messages.size(), lessThan(messageCount));
	}

	@Test
	public void scs_whenHeadersAreSpecified_headersArePassedThrough() throws Exception {
		Sender sender = createSender();
		Receiver receiver = createReceiver(randomGroupArg1);

		sender.send(MESSAGE_TEXTS[1], ImmutableMap.of(HEADER_KEY, HEADER_VALUE));

		final List<Message<Object>> messages = receiver.getHandledMessages();

		waitFor(() ->
			assertThat(messages, contains(
					allOf(
							hasProperty("payload", is(MESSAGE_TEXTS[1].getBytes(StandardCharsets.UTF_8))),
							hasProperty("headers", hasEntry(HEADER_KEY, HEADER_VALUE))
					)
			))
		);
	}

	@Test
	public void scs_whenHeadersContainingIllegalCharactersAreSpecified_headersAreRewrittenAndPassedThrough() throws Exception {
		Sender sender = createSender();
		Receiver receiver = createReceiver(randomGroupArg1);

		sender.send(MESSAGE_TEXTS[1], ImmutableMap.<String, Object>of(INVALID_HEADER_KEY, HEADER_VALUE));

		final List<Message<Object>> messages = receiver.getHandledMessages();

		waitFor(() ->
			assertThat(messages, contains(
					allOf(
							hasProperty("payload", is(MESSAGE_TEXTS[1].getBytes(StandardCharsets.UTF_8))),
							hasProperty("headers", hasEntry(INVALID_HEADER_KEY.replaceAll("-", "_"), HEADER_VALUE))
					)
			))
		);
	}

	@Test
	public void scs_whenRequiredGroupsAreSpecified_messagesArePersistedWithoutConsumers() throws Exception {
		String group42 = getRandomName("group42");
		Sender sender = createSender(
				String.format(REQUIRED_GROUPS_FORMAT, group42));

		sender.send(MESSAGE_TEXTS[2]);

		Receiver receiver = createReceiver(
				String.format(INPUT_GROUP_FORMAT, group42));
		final List<Message<Object>> messages = receiver.getHandledMessages();


		waitFor(() -> {
			assertThat(messages, hasSize(1));
		});
		assertThat(extractStringPayload(messages), containsInAnyOrder(MESSAGE_TEXTS[2]));

	}

	@Test
	public void scs_whenMessageIsSentToDLQ_stackTraceAddedToHeaders() throws Exception {
		Sender sender = createSender();
		createReceiver(randomGroupArg1, MAX_ATTEMPTS_1, DLQ_CONSUMER_PROPERTY);
		JmsConsumerProperties jmsConsumerProperties = new JmsConsumerProperties();
		jmsConsumerProperties.setDlqName(DLQ_NAME);
		queueProvisioner.provisionConsumerDestination(this.destination, randomGroupArg1, new ExtendedConsumerProperties<>(jmsConsumerProperties));

		sender.send(Receiver.EXCEPTION_REQUEST);

		javax.jms.Message message = jmsTemplate.receive(DLQ_NAME);
		assertThat(message, notNullValue());

		String stacktrace = message.getStringProperty(
				RepublishMessageRecoverer.X_EXCEPTION_STACKTRACE);
		assertThat(stacktrace, containsString(Receiver.REQUESTED_EXCEPTION));
	}

	@Test
	public void scs_whenConsumerFails_retriesTheSpecifiedAmountOfTimes() throws Exception {
		Sender sender = createSender();
		Receiver receiver = createReceiver(
				randomGroupArg1,
				RETRY_BACKOFF_1X,
				RETRY_BACKOFF_50MS, DLQ_CONSUMER_PROPERTY);

		sender.send(Receiver.EXCEPTION_REQUEST);

		final List<Message<Object>> receivedMessages = receiver.getReceivedMessages();
		final List<Message<Object>> handledMessages = receiver.getHandledMessages();

		waitFor(1000, () -> {
			assertThat(receivedMessages, hasSize(3));
			assertThat(handledMessages, empty());
			assertThat(EndToEndIntegrationTests.this.extractStringPayload(receivedMessages),
					contains(Receiver.EXCEPTION_REQUEST,
							Receiver.EXCEPTION_REQUEST,
							Receiver.EXCEPTION_REQUEST));
		});
		JmsConsumerProperties jmsConsumerProperties = new JmsConsumerProperties();
		jmsConsumerProperties.setDlqName(DLQ_NAME);
		queueProvisioner.provisionConsumerDestination(this.destination, randomGroupArg1, new ExtendedConsumerProperties<>(new JmsConsumerProperties()));

		javax.jms.Message message = jmsTemplate.receive(DLQ_NAME);
		assertThat(message, notNullValue());
	}

	@Test
	public void scs_maxAttempts1_preventsRetry() throws Exception {
		Sender sender = createSender();
		Receiver receiver = createReceiver(randomGroupArg1, MAX_ATTEMPTS_1);

		sender.send(Receiver.EXCEPTION_REQUEST);

		Thread.sleep(500);
		assertThat(receiver.getHandledMessages(), hasSize(0));

		//we don't get a retry from the broker because we accept the message and DLQ_NAME it ourselves
		assertThat(receiver.getReceivedMessages(), hasSize(1));
		assertThat(extractStringPayload(receiver.getReceivedMessages()),
				contains(Receiver.EXCEPTION_REQUEST));
	}

	@Test
	public void scs_whenAPartitioningKeyIsConfigured_messagesAreRoutedToTheRelevantPartition() throws Exception {
		Sender sender = createSender(
				String.format("--spring.cloud.stream.bindings.output.producer.partitionKeyExpression=%s", "T(Integer).parseInt(payload) % 13 == 0 ? 1 : 0"),
				String.format("--spring.cloud.stream.bindings.output.producer.partitionCount=%s", 2)
		);

		Receiver receiverPartition0 = createReceiver(
				randomGroupArg1,
				String.format("--spring.cloud.stream.instance-index=%s", 0),
				String.format("--spring.cloud.stream.instance-count=%s", 2),
				String.format("--spring.cloud.stream.bindings.input.consumer.partitioned=%s", true)
		);
		Receiver receiverPartition1 = createReceiver(
				randomGroupArg2,
				String.format("--spring.cloud.stream.instance-index=%s", 1),
				String.format("--spring.cloud.stream.instance-count=%s", 2),
				String.format("--spring.cloud.stream.bindings.input.consumer.partitioned=%s", true)
		);

		int messageCount = 39;
		for (int i = 0; i < messageCount; i++) {
			sender.send(i);
		}
		final List<Message<Object>> messagesPartition0 = receiverPartition0.getHandledMessages();
		final List<Message<Object>> messagesPartition1 = receiverPartition1.getHandledMessages();

		waitFor(() -> {
			assertThat(messagesPartition1, hasSize(3));
			List<String> objects = EndToEndIntegrationTests.this.extractStringPayload(messagesPartition1);
			assertThat(objects, hasItems("0", "13", "26"));

			assertThat(messagesPartition0, hasSize(36));
			assertThat(EndToEndIntegrationTests.this.extractStringPayload(messagesPartition0),
					allOf(hasItems("1", "2", "38"), not(hasItem("13"))));
		});
	}


	@Test
	public void scs_whenAPartitioningKeyIsConfiguredAsWellAsRequired_messagesArePersistedInTheRelevantPartition() throws Exception {
		String group66 = getRandomName("group66");
		Sender sender = createSender(

				String.format("--spring.cloud.stream.bindings.output.producer.partitionKeyExpression=%s", "T(Integer).parseInt(payload) % 13 == 0 ? 1 : 0"),
				String.format("--spring.cloud.stream.bindings.output.producer.partitionCount=%s", 2),
				String.format(REQUIRED_GROUPS_FORMAT, group66)
		);

		int messageCount = 39;
		for (int i = 0; i < messageCount; i++) {
			sender.send(i);
		}

		Receiver receiverPartition0 = createReceiver(
				String.format(INPUT_GROUP_FORMAT, group66),
				String.format("--spring.cloud.stream.instance-index=%s", 0),
				String.format("--spring.cloud.stream.instance-count=%s", 2),
				String.format("--spring.cloud.stream.bindings.input.consumer.partitioned=%s", true)
		);
		Receiver receiverPartition1 = createReceiver(
				String.format(INPUT_GROUP_FORMAT, group66),
				String.format("--spring.cloud.stream.instance-index=%s", 1),
				String.format("--spring.cloud.stream.instance-count=%s", 2),
				String.format("--spring.cloud.stream.bindings.input.consumer.partitioned=%s", true)
		);

		final List<Message<Object>> messagesPartition0 = receiverPartition0.getHandledMessages();
		final List<Message<Object>> messagesPartition1 = receiverPartition1.getHandledMessages();

		waitFor(() -> {
			assertThat(messagesPartition1, hasSize(3));
			List<String> objects = EndToEndIntegrationTests.this.extractStringPayload(messagesPartition1);
			assertThat(objects, hasItems("0", "13", "26"));

			assertThat(messagesPartition0, hasSize(36));
			assertThat(EndToEndIntegrationTests.this.extractStringPayload(messagesPartition0),
					allOf(hasItems("1", "2", "38"), not(hasItem("13"))));
		});
	}

	@Test
	public void scs_supportsSerializable() throws Exception {
		Sender sender = createSender();
		Receiver receiver = createReceiver(randomGroupArg1, MAX_ATTEMPTS_1);

		final SerializableTest serializableTest = new SerializableTest("some value");
		sender.send(serializableTest);

		final List<Message<Object>> messages = receiver.getHandledMessages();

		waitFor(() -> {
			assertThat(messages, hasSize(1));
			List<SerializableTest> actual = (List<SerializableTest>) EndToEndIntegrationTests.this.extractPayload(messages, SerializableTest.class);
			assertThat(actual, containsInAnyOrder(serializableTest));
		});
	}

	@Test
	public void scs_supportsPrimitive() throws Exception {
		Sender sender = createSender();
		Receiver receiver = createReceiver(randomGroupArg1);

		sender.send(11);

		final List<Message<Object>> messages = receiver.getHandledMessages();

		waitFor(() -> {
			assertThat(messages, hasSize(1));
			assertThat(EndToEndIntegrationTests.this.extractPayload(messages, Integer.class), containsInAnyOrder(11));
		});
	}

	protected Sender createSender(String... arguments) {
		String[] startupArgs = (String[]) ArrayUtils.addAll(arguments, new String[] {

		});
		ConfigurableApplicationContext context = new SpringApplicationBuilder(SenderApplication.class)
				.bannerMode(Banner.Mode.OFF)
				.build()
				.run(applicationArguments(String.format(OUTPUT_DESTINATION_FORMAT, this.destination), startupArgs));

		startedContexts.add(context);
		return context.getBean(Sender.class);
	}

	protected Receiver createReceiver(String... arguments) {
		String[] startupArgs = (String[]) ArrayUtils.addAll(arguments, new String[] {

		});
		ConfigurableApplicationContext context = new SpringApplicationBuilder(ReceiverApplication.class)
				.bannerMode(Banner.Mode.OFF)
				.build()
				.run(applicationArguments(String.format(INPUT_DESTINATION_FORMAT, this.destination), startupArgs));

		startedContexts.add(context);
		return context.getBean(Receiver.class);
	}

	private String[] applicationArguments(String destinationArg, String[] arguments) {
		return (String[]) ArrayUtils.addAll(arguments, new String[]{
				destinationArg,
				"--logging.level.org.springframework=WARN",
				"--logging.level.org.springframework.boot=WARN"
		});
	}

	private List<String> extractStringPayload(List<Message<Object>> messages) {
		return (List<String>) extractPayload(messages, String.class);
	}

	private List<?> extractPayload(List<Message<Object>> messages, Class<?> clazz) {
		List<Object> output = new ArrayList<>();
		messages.forEach(message -> output.add(this.messageConverter.fromMessage(message, clazz)));
		return output;
	}

	private String getRandomName(String prefix) {
		return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
	}

	public static class SerializableTest implements Serializable {
		public final String value;

		@JsonCreator
		public SerializableTest(@JsonProperty("value") String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return "SerializableTest<" + value + ">";
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			SerializableTest that = (SerializableTest) o;

			return value.equals(that.value);

		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}
	}
}
