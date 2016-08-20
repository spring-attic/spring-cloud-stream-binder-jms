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

import javax.jms.*;

import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.integration.dsl.jms.JmsMessageDrivenChannelAdapter;
import org.springframework.integration.jms.ChannelPublishingJmsMessageListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Component responsible of building up endpoint required to bind consumers.
 *
 * @author José Carlos Valero
 * @since 1.1
 */
public class JmsMessageDrivenChannelAdapterFactory {
    final String RETRY_CONTEXT_MESSAGE_ATTRIBUTE = "message";
    private final ListenerContainerFactory listenerContainerFactory;
    private final MessageRecoverer messageRecoverer;
    private final DestinationNameResolver destinationNameResolver;


    public JmsMessageDrivenChannelAdapterFactory(ListenerContainerFactory listenerContainerFactory,
                                                 MessageRecoverer messageRecoverer,
                                                 DestinationNameResolver destinationNameResolver) {
        this.listenerContainerFactory = listenerContainerFactory;
        this.messageRecoverer = messageRecoverer;
        this.destinationNameResolver = destinationNameResolver;
    }

    public JmsMessageDrivenChannelAdapter build(Queue group,
                                                final ConsumerProperties properties) {
        return new JmsMessageDrivenChannelAdapter(
                    listenerContainerFactory.build(group),
                    // the listener is the channel adapter. it connects the JMS endpoint to the input
                    // channel by converting the messages that the listener container passes to it
                    new ChannelPublishingJmsMessageListener() {
                        @Override
                        public void onMessage(Message jmsMessage,
                                              Session session) throws JMSException {
                            getRetryTemplate(properties).execute(
                                    continueRetryContext -> {
                                        try {
                                            continueRetryContext.setAttribute(
                                                    RETRY_CONTEXT_MESSAGE_ATTRIBUTE,
                                                    jmsMessage);
                                            super.onMessage(jmsMessage, session);
                                        } catch (JMSException e) {
                                            logger.error("Failed to send message",
                                                    e);
                                            resetMessageIfRequired(jmsMessage);
                                            throw new RuntimeException(e);
                                        } catch (Exception e){
                                            resetMessageIfRequired(jmsMessage);
                                            throw e;
                                        }
                                        return null;
                                    },
                                    recoverRetryContext -> {
                                        if (messageRecoverer != null) {
                                            Message message = (Message) recoverRetryContext.getAttribute(
                                                    RETRY_CONTEXT_MESSAGE_ATTRIBUTE);
                                            messageRecoverer.recover(message,
                                                    recoverRetryContext.getLastThrowable());
                                        } else {
                                            logger.warn(
                                                    "No message recoverer was configured. Messages will be discarded.");
                                        }
                                        return null;
                                    }
                            );
                        }
                    }
            );
    }

    private void resetMessageIfRequired(Message jmsMessage) throws JMSException {
        if (jmsMessage instanceof BytesMessage) {
            BytesMessage message = (BytesMessage) jmsMessage;
            message.reset();
        }
    }

    private RetryTemplate getRetryTemplate(ConsumerProperties properties) {
        RetryTemplate template = new RetryTemplate();
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(properties.getMaxAttempts());
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(properties.getBackOffInitialInterval());
        backOffPolicy.setMultiplier(properties.getBackOffMultiplier());
        backOffPolicy.setMaxInterval(properties.getBackOffMaxInterval());
        template.setRetryPolicy(retryPolicy);
        template.setBackOffPolicy(backOffPolicy);
        return template;
    }
}
