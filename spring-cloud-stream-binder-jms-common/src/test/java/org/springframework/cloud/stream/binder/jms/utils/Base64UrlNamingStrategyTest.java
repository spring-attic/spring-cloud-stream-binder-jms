package org.springframework.cloud.stream.binder.jms.utils;

import org.junit.Test;

public class Base64UrlNamingStrategyTest {

	@Test(expected = IllegalArgumentException.class)
	public void base64NamingStrategy_withEmptyPrefixOnConstructor_isNotAllowed() {
		new Base64UrlNamingStrategy("").generateName();
	}

	@Test(expected = IllegalArgumentException.class)
	public void base64NamingStrategy_withEmptyPrefix_isNotAllowed() {
		new Base64UrlNamingStrategy().generateName("");
	}
}
