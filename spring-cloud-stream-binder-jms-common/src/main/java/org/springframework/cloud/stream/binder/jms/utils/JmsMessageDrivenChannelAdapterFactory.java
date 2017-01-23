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

package org.springframework.cloud.stream.binder.jms.utils;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Session;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.dsl.jms.JmsMessageDrivenChannelAdapter;
import org.springframework.integration.jms.ChannelPublishingJmsMessageListener;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Component responsible of building up endpoint required to bind consumers.
 *
 * @author José Carlos Valero
 * @author Gary Russell
 * @since 1.1
 */
public class JmsMessageDrivenChannelAdapterFactory implements ApplicationContextAware, BeanFactoryAware {

	private final ListenerContainerFactory listenerContainerFactory;

	private final MessageRecoverer messageRecoverer;

	private BeanFactory beanFactory;

	private ApplicationContext applicationContext;


	public JmsMessageDrivenChannelAdapterFactory(ListenerContainerFactory listenerContainerFactory,
			MessageRecoverer messageRecoverer) {
		this.listenerContainerFactory = listenerContainerFactory;
		this.messageRecoverer = messageRecoverer;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public JmsMessageDrivenChannelAdapter build(Queue destination,
												final ConsumerProperties properties) {
		RetryingChannelPublishingJmsMessageListener listener = new RetryingChannelPublishingJmsMessageListener(
				properties, messageRecoverer);
		listener.setBeanFactory(this.beanFactory);
		JmsMessageDrivenChannelAdapter adapter = new JmsMessageDrivenChannelAdapter(
				listenerContainerFactory.build(destination), listener);
		adapter.setApplicationContext(this.applicationContext);
		adapter.setBeanFactory(this.beanFactory);
		return adapter;
	}

	private static class RetryingChannelPublishingJmsMessageListener extends ChannelPublishingJmsMessageListener {

		private final String RETRY_CONTEXT_MESSAGE_ATTRIBUTE = "message";

		private final ConsumerProperties properties;

		private final MessageRecoverer messageRecoverer;

		RetryingChannelPublishingJmsMessageListener(ConsumerProperties properties,
				MessageRecoverer messageRecoverer) {
			this.properties = properties;
			this.messageRecoverer = messageRecoverer;
		}

		@Override
		public void onMessage(final Message jmsMessage, final Session session) throws JMSException {
			getRetryTemplate(properties).execute(
					new RetryCallback<Object, JMSException>() {

						@Override
						public Object doWithRetry(RetryContext retryContext) throws JMSException {
							try {
								retryContext.setAttribute(RETRY_CONTEXT_MESSAGE_ATTRIBUTE, jmsMessage);
								RetryingChannelPublishingJmsMessageListener.super.onMessage(jmsMessage, session);
							}
							catch (JMSException e) {
								logger.error("Failed to send message",
										e);
								resetMessageIfRequired(jmsMessage);
								throw new RuntimeException(e);
							}
							catch (Exception e) {
								resetMessageIfRequired(jmsMessage);
								throw e;
							}
							return null;
						}

					},
					new RecoveryCallback<Object>() {

						@Override
						public Object recover(RetryContext retryContext) throws Exception {
							if (messageRecoverer != null) {
								Message message = (Message) retryContext.getAttribute(RETRY_CONTEXT_MESSAGE_ATTRIBUTE);
								messageRecoverer.recover(message, MessageRecoverer.ACTIVE_MQ_DLQ, retryContext.getLastThrowable());
							}
							else {
								logger.warn("No message recoverer was configured. Messages will be discarded.");
							}
							return null;
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

}
