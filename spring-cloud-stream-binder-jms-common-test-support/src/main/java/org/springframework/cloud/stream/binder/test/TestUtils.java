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
