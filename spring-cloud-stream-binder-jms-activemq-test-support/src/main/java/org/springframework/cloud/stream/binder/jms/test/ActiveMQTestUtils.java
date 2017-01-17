package org.springframework.cloud.stream.binder.jms.test;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.store.PersistenceAdapter;
import org.apache.activemq.store.kahadb.KahaDBPersistenceAdapter;

import java.io.File;

/**
 * Provides ActiveMQ related utilities.
 *
 * @author José Carlos Valero
 * @since 1.1
 */
public abstract class ActiveMQTestUtils {

	private static BrokerService broker;

	public static ActiveMQConnectionFactory startEmbeddedActiveMQServer() throws Exception {
		createBroker();
		return new ActiveMQConnectionFactory(broker.getTransportConnectors().get(0).getConnectUri().toString());
	}

	public static void stopEmbeddedActiveMQServer() throws Exception {
		broker.stop();
	}

	private static void createBroker() throws Exception {
		synchronized (ActiveMQTestUtils.class){
			if(broker == null){
				broker = new BrokerService();
				File testDataDir = new File("target/activemq-data/tests");
				broker.setDataDirectoryFile(testDataDir);
				broker.setUseJmx(true);
				broker.setDeleteAllMessagesOnStartup(true);
				PersistenceAdapter persistenceAdapter = new KahaDBPersistenceAdapter();
				persistenceAdapter.setDirectory(new File(testDataDir, "kahadb"));
				broker.setPersistenceAdapter(persistenceAdapter);
				broker.addConnector("tcp://localhost:44029");
				broker.start();
				broker.waitUntilStarted();
			}else {
				return;
			}
		}

	}

}
