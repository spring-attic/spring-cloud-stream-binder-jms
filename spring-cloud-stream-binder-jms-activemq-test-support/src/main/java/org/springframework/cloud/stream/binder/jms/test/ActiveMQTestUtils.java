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

package org.springframework.cloud.stream.binder.jms.test;

import java.io.File;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.store.PersistenceAdapter;
import org.apache.activemq.store.kahadb.KahaDBPersistenceAdapter;

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
