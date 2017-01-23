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

package org.springframework.cloud.stream.binder.test;
/**
 * @author José Carlos Valero
 * @since 20/08/16
 */
public abstract class TestUtils {

	public static void waitFor(Runnable assertion) {
		waitFor(1000, assertion);
	}

	public static void waitFor(int millis, Runnable assertion) {
		long endTime = System.currentTimeMillis() + millis;

		while (true) {
			try {
				assertion.run();
				return;
			} catch (AssertionError e) {
				if (System.currentTimeMillis() > endTime) {
					throw e;
				}
			}
			try {
				Thread.sleep(millis / 10);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static void waitForItToWork(int millis, Runnable assertion) {
		long endTime = System.currentTimeMillis() + millis;

		while (true) {
			try {
				assertion.run();
				return;
			} catch (Exception e) {
				if (System.currentTimeMillis() > endTime) {
					throw e;
				}
			}
			try {
				Thread.sleep(millis / 10);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
