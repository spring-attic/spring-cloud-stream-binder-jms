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

import org.hamcrest.core.Is;
import org.junit.Test;
import org.springframework.cloud.stream.binder.jms.config.JmsBinderConfigurationProperties;
import org.springframework.jms.listener.AbstractMessageListenerContainer;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Queue;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.util.ReflectionTestUtils.getField;

public class ListenerContainerFactoryTest {

    @Test
    public void listenerContainerFactory_createsAndConfiguresMessageListenerContainer() throws Exception {
        ConnectionFactory factory = mock(ConnectionFactory.class);
        ListenerContainerFactory listenerContainerFactory = new ListenerContainerFactory(
                new JmsBinderConfigurationProperties(),
                factory);

        Queue queue = mock(Queue.class);
        AbstractMessageListenerContainer messageListenerContainer = listenerContainerFactory.build(queue);

        assertThat(messageListenerContainer.getDestination(), Is.<Destination>is(queue));
        assertThat(messageListenerContainer.getConnectionFactory(), Is.is(factory));
        assertThat(getField(messageListenerContainer, "pubSubDomain"), Is.<Object>is(false));
        assertThat("Transacted is not true. Transacted is required for guaranteed deliveries. " +
                        "In particular, some implementation will require it so they can eventually route the message to the DLQ",
                messageListenerContainer.isSessionTransacted(), is(true));

    }

}
