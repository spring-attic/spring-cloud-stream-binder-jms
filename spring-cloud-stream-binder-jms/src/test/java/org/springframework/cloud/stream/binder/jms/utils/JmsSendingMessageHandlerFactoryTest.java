/*
 *  Copyright 2002-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.stream.binder.jms.utils;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.integration.jms.JmsSendingMessageHandler;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * @author José Carlos Valero
 * @since 1.1
 */
public class JmsSendingMessageHandlerFactoryTest {

    public static final String DESTINATION_NAME = "a-destination";
    JmsTemplate jmsTemplate = mock(JmsTemplate.class);
    BeanFactory beanFactory = mock(BeanFactory.class);
    private JmsSendingMessageHandlerFactory target = new JmsSendingMessageHandlerFactory(jmsTemplate,beanFactory);

    //Not too sure about these tests, but can't find a better way of actually testing a factory without interacting with the subproduct.

    @Test
    public void build_createsAHandlerWithTheProvidedParameters() throws Exception {
        ProducerProperties properties = new ProducerProperties();

        JmsSendingMessageHandler handler = target.build(DESTINATION_NAME, properties);

        assertThat(ReflectionTestUtils.getField(handler, "jmsTemplate"), is(jmsTemplate));
        assertThat(ReflectionTestUtils.getField(handler, "producerProperties"), is(properties));
    }

    @Test
    public void build_configuresTheHandlerWithDestinationAndBeanFactory() throws Exception {
        ProducerProperties properties = new ProducerProperties();

        JmsSendingMessageHandler handler = target.build(DESTINATION_NAME, properties);

        assertThat(ReflectionTestUtils.getField(handler, "destinationName"), is(DESTINATION_NAME));
        assertThat(ReflectionTestUtils.getField(handler, "beanFactory"), is(beanFactory));
    }
}