/*
 *  Copyright 2002-2017 the original author or authors.
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
package org.springframework.cloud.stream.binder.jms.utils;

import javax.jms.JMSException;
import javax.jms.Message;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.jms.JmsHeaderMapper;
import org.springframework.messaging.MessageHeaders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Donovan Muller
 */
public class SpecCompliantJmsHeaderMapperTest {

	private JmsHeaderMapper jmsHeaderMapper;

	@Before
	public void setup() {
		jmsHeaderMapper = new SpecCompliantJmsHeaderMapper();
	}

	@Test
	public void mappingHeaders_whenIllegalCharacterInNameUsed_rewriteToBeCompliant() throws JMSException {
		Message message = mock(Message.class);

		jmsHeaderMapper.fromHeaders(new MessageHeaders(ImmutableMap.of(
				"x-invalid-header-name", (Object) "test",
				"x_valid_header_name",  "test"
		)), message);

		verify(message).setObjectProperty("x_invalid_header_name", "test");
		verify(message).setObjectProperty("x_valid_header_name", "test");
	}
}
