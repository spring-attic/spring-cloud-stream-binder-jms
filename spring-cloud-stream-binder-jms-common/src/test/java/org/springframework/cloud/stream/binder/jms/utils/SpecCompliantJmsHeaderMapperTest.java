package org.springframework.cloud.stream.binder.jms.utils;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.jms.JmsHeaderMapper;
import org.springframework.messaging.MessageHeaders;

import javax.jms.JMSException;
import javax.jms.Message;

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
