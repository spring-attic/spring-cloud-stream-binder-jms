package org.springframework.cloud.stream.binder.jms.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class Base64UrlNamingStrategyTest {

	@Test
	public void base64NamingStrategy_withSimplePrefix() {
		String generateName = new Base64UrlNamingStrategy().generateName("foo-");
		assertThat(generateName).startsWith("foo-");
	}
}
