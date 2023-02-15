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

package org.springframework.cloud.stream.binder.test.integration.receiver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@SpringBootApplication(exclude = {EmbeddedServletContainerAutoConfiguration.class, WebMvcAutoConfiguration.class, JmxAutoConfiguration.class})
@EnableBinding(Sink.class)
public class ReceiverApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReceiverApplication.class, args);
	}

	@Component
	public static class Receiver {

		public static final String EXCEPTION_REQUEST = "Please throw an exception";
		public static final String REQUESTED_EXCEPTION = "Here you go";

		private final List<Message> handledMessages = new ArrayList<>();

		private final List<Message> receivedMessages = new ArrayList<>();
		private CountDownLatch latch;

		@StreamListener(Sink.INPUT)
		public void receive(Message message) {
			receivedMessages.add(message);

			Object payload = message.getPayload();
			if (payload.equals(EXCEPTION_REQUEST)) {
				throw new RuntimeException(REQUESTED_EXCEPTION);
			}

			handledMessages.add(message);
			if (latch != null) {
				latch.countDown();
			}
		}

		public void setLatch(CountDownLatch latch) {
			this.latch = latch;
		}

		public List<Message> getHandledMessages() {
			return handledMessages;
		}

		public List<Message> getReceivedMessages() {
			return receivedMessages;
		}
	}
}
