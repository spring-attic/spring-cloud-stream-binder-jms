/*
 *  Copyright 2017 the original author or authors.
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

package org.springframework.cloud.stream.binder.jms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Donovan Muller
 * @author Gary Russell
 */
@ConfigurationProperties("spring.cloud.stream.binder.jms")
public class JmsBinderConfigurationProperties {

	/** the name of the dead letter queue **/
	private String deadLetterQueueName = "Spring.Cloud.Stream.dlq";

	public String getDeadLetterQueueName() {
		return deadLetterQueueName;
	}

	public void setDeadLetterQueueName(String deadLetterQueueName) {
		this.deadLetterQueueName = deadLetterQueueName;
	}

}
