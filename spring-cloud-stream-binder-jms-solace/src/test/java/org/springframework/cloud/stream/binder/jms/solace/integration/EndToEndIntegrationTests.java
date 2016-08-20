package org.springframework.cloud.stream.binder.jms.solace.integration;

import org.springframework.cloud.stream.binder.jms.solace.SolaceQueueProvisioner;
import org.springframework.cloud.stream.binder.jms.solace.SolaceTestUtils;

/**
 * @author José Carlos Valero
 * @since 1.1
 */
public class EndToEndIntegrationTests extends org.springframework.cloud.stream.binder.test.integration.EndToEndIntegrationTests {
    public EndToEndIntegrationTests() throws Exception {
        super(
                new SolaceQueueProvisioner(SolaceTestUtils.getSolaceProperties()),
                SolaceTestUtils.createConnectionFactory()
        );
    }

    @Override
    protected void deprovisionDLQ() throws Exception {
        SolaceTestUtils.deprovisionDLQ();
    }
}
