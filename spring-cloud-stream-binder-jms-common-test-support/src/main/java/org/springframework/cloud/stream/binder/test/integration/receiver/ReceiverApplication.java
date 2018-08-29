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

package org.springframework.cloud.stream.binder.test.integration.receiver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

@SpringBootApplication(exclude = {EmbeddedWebServerFactoryCustomizerAutoConfiguration.class, WebMvcAutoConfiguration.class, JmxAutoConfiguration.class})
@EnableBinding(Sink.class)
public class ReceiverApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReceiverApplication.class, args);
	}

	@Component
	public static class Receiver {

		public static final String EXCEPTION_REQUEST = "Please throw an exception";
		public static final String REQUESTED_EXCEPTION = "Here you go";

		private final List<Message<Object>> handledMessages = new ArrayList<>();

		private final List<Message<Object>> receivedMessages = new ArrayList<>();
		private CountDownLatch latch;

		@StreamListener(Sink.INPUT)
		public void receive(Message<Object> message) {
			receivedMessages.add(message);

			Object payload = extractPayload(message);
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

		public List<Message<Object>> getHandledMessages() {
			return handledMessages;
		}

		public List<Message<Object>> getReceivedMessages() {
			return receivedMessages;
		}

		private Object extractPayload(Message<Object> message) {
			Object o = message.getPayload();
			MimeType contentType = (MimeType) message.getHeaders().get("contentType");
			if (MimeTypeUtils.APPLICATION_JSON.equals(contentType)
				|| MimeTypeUtils.TEXT_PLAIN.equals(contentType)) {
				return new String((byte[]) o);
			} else {
				return o;
			}
		}
	}
}
