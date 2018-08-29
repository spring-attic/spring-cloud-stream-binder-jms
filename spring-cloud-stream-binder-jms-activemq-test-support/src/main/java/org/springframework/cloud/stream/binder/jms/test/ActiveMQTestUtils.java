/*
 *  Copyright 2002-2017 the original author or authors.
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

package org.springframework.cloud.stream.binder.jms.test;

import java.io.File;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.store.PersistenceAdapter;
import org.apache.activemq.store.kahadb.KahaDBPersistenceAdapter;

import javax.jms.ConnectionFactory;

/**
 * Provides ActiveMQ related utilities.
 *
 * @author José Carlos Valero
 * @author Tim Ysewyn
 * @since 1.1
 */
public class ActiveMQTestUtils {

	private BrokerService broker;

	public ActiveMQTestUtils() {
		startEmbeddedActiveMQServer();
	}

	public ConnectionFactory getConnectionFactory() throws Exception {
		startEmbeddedActiveMQServer();
		return new ActiveMQConnectionFactory(broker.getTransportConnectors().get(0).getConnectUri());
	}

	private void startEmbeddedActiveMQServer() {
		synchronized (this) {
			if (broker == null) {
				try {
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
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	public void stopEmbeddedActiveMQServer() throws Exception {
		broker.stop();
	}

}
