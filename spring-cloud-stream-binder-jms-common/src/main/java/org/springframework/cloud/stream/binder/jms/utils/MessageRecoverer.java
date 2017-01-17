/*
 *  Copyright 2002-2016 the original author or authors.
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

import javax.jms.Message;

/**
 * Interface used to define the recovery strategy once the maximum number
 * of delivery attempts has been reached.
 *
 * @author Jack Galilee
 * @author Jonathan Sharpe
 * @author Joseph Taylor
 * @author José Carlos Valero
 * @since 1.1
 */
public interface MessageRecoverer {

	/**
	 * Recover from the failure to deliver a message.
	 *
	 * @param undeliveredMessage the message that has not been delivered.
	 * @param cause the reason for the failure to deliver.
	 */
	void recover(Message undeliveredMessage, Throwable cause);
}
