package org.springframework.cloud.stream.binder.jms.test;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.store.kahadb.KahaDBPersistenceAdapter;

import java.io.File;

/**
 * Provides ActiveMQ related utilities.
 *
 * @author José Carlos Valero
 * @since 1.1
 */
public abstract class ActiveMQTestUtils {

    public static ActiveMQConnectionFactory startEmbeddedActiveMQServer() throws Exception {
        BrokerService broker = new BrokerService();

        File testDataDir = new File("target/activemq-data/QueuePurgeTest");
        broker.setDataDirectoryFile(testDataDir);
        broker.setUseJmx(true);
        broker.setDeleteAllMessagesOnStartup(true);
        KahaDBPersistenceAdapter persistenceAdapter = new KahaDBPersistenceAdapter();
        persistenceAdapter.setDirectory(new File(testDataDir, "kahadb"));
        broker.setPersistenceAdapter(persistenceAdapter);
        broker.addConnector("tcp://localhost:0");
        broker.start();
        return new ActiveMQConnectionFactory(broker.getTransportConnectors().get(0).getConnectUri().toString());
    }

}
