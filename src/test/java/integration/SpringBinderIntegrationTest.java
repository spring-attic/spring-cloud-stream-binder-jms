package integration;

public abstract class SpringBinderIntegrationTest {

    public void waitFor(Runnable assertion) throws InterruptedException {
        waitFor(1000, assertion);
    }

    public void waitFor(int millis, Runnable assertion) throws InterruptedException {
        while (true) {
            long endTime = System.currentTimeMillis() + millis;
            try {
                assertion.run();
                return;
            } catch (AssertionError e) {
                if (System.currentTimeMillis() > endTime) {
                    throw e;
                }
            }
            Thread.sleep(millis / 10);
        }
    }

}
