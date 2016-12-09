package org.springframework.cloud.stream.binder.jms.ibmmq;

import java.io.IOException;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.binder.jms.config.JmsBinderConfigurationProperties;
import org.springframework.cloud.stream.binder.jms.ibmmq.config.IBMMQConfigurationProperties;
import org.springframework.jms.support.JmsUtils;

import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFMessageAgent;

public class IBMMQRequests {

	public static final int MQ_OBJECT_ALREADY_EXISTS = 4001;

	public static final int MQ_SUBSCRIPTION_ALREADY_EXISTS = 3311;

	private static final Logger logger = LoggerFactory.getLogger(IBMMQRequests.class);

	private final ConnectionFactory connectionFactory;

	/**
	 * Thread safe (mostly). See
	 * http://www.ibm.com/support/knowledgecenter/SSFKSJ_8.0.0/com.ibm.mq.javadoc.doc/WMQJavaClasses/com/ibm/mq/MQQueueManager.html
	 */
	private final MQQueueManager queueManager;

	private final JmsBinderConfigurationProperties binderConfigurationProperties;

	private final IBMMQConfigurationProperties configurationProperties;

	public IBMMQRequests(ConnectionFactory connectionFactory,
			JmsBinderConfigurationProperties binderConfigurationProperties,
			IBMMQConfigurationProperties configurationProperties) {
		this.connectionFactory = connectionFactory;
		this.binderConfigurationProperties = binderConfigurationProperties;
		this.configurationProperties = configurationProperties;

		try {
			this.queueManager = new MQQueueManager(
					configurationProperties.getQueueManager());
		}
		catch (MQException e) {
			throw new RuntimeException(
					String.format("Could not create MQ Queue Manager object for '%s'",
							configurationProperties.getQueueManager()));
		}
	}

	public Topic createTopic(String topicName) {
		try {
			PCFMessageAgent pcfMessageAgent = new PCFMessageAgent(queueManager);
			final PCFMessage request = new PCFMessage(MQConstants.MQCMD_CREATE_TOPIC);
			request.addParameter(MQConstants.MQCA_TOPIC_NAME, topicName);
			request.addParameter(MQConstants.MQCA_TOPIC_STRING, topicName);
			pcfMessageAgent.send(request);
			pcfMessageAgent.disconnect();
		}
		catch (MQException e) {
			// see
			// http://www.ibm.com/support/knowledgecenter/SSFKSJ_8.0.0/com.ibm.mq.tro.doc/q048200_.htm
			if (e.getReason() != MQ_OBJECT_ALREADY_EXISTS) {
				throw new RuntimeException(
						String.format("Could not provision topic '%s'", topicName), e);
			}
			else {
				logger.warn("Topic '{}' already exists, using existing object",
						topicName);
			}
		}
		catch (IOException e) {
			String errorMessage = String.format("Could not create topic: '%s'",
					topicName);
			logger.error(String.format("Could not create topic: '%s'", topicName, e));
			throw new RuntimeException(errorMessage, e);
		}

		Connection connection = null;
		Session session = null;
		try {
			connection = connectionFactory.createConnection();
			session = connection.createSession(
					binderConfigurationProperties.isTransacted(),
					Session.AUTO_ACKNOWLEDGE);
			return session.createTopic(topicName);
		}
		catch (JMSException e) {
			String errorMessage = String.format("Could not create topic: '%s'",
					topicName);
			logger.error(String.format("Could not create topic: '%s'", topicName, e));
			throw new RuntimeException(errorMessage, e);
		}
		finally {
			JmsUtils.closeSession(session);
			JmsUtils.closeConnection(connection);
		}
	}

	public Queue createQueue(String queueName) {
		try {
			PCFMessageAgent pcfMessageAgent = new PCFMessageAgent(queueManager);

			PCFMessage request = new PCFMessage(MQConstants.MQCMD_CREATE_Q);
			request.addParameter(MQConstants.MQCA_Q_NAME, queueName);
			request.addParameter(MQConstants.MQIA_Q_TYPE, MQConstants.MQQT_LOCAL);
			request.addParameter(MQConstants.MQIA_MAX_Q_DEPTH,
					configurationProperties.getQueueDepth());
			pcfMessageAgent.send(request);
			pcfMessageAgent.disconnect();
		}
		catch (MQException e) {
			// see
			// http://www.ibm.com/support/knowledgecenter/SSFKSJ_8.0.0/com.ibm.mq.tro.doc/q048200_.htm
			if (e.getReason() != MQ_OBJECT_ALREADY_EXISTS) {
				throw new RuntimeException(
						String.format("Could not create queue '%s'", queueName), e);
			}
			else {
				logger.warn("Queue '{}' already exists, using existing object",
						queueName);
			}
		}
		catch (IOException e) {
			logger.error(String.format("Could not create queue '%s'", queueName), e);
		}

		Connection connection = null;
		Session session = null;
		try {
			connection = connectionFactory.createConnection();
			session = connection.createSession(
					binderConfigurationProperties.isTransacted(),
					Session.AUTO_ACKNOWLEDGE);
			return session.createQueue(queueName);
		}
		catch (JMSException e) {
			String errorMessage = String.format("Could not create queue: '%s'",
					queueName);
			logger.error(String.format("Could not create queue: '%s'", queueName, e));
			throw new RuntimeException(errorMessage, e);
		}
		finally {
			JmsUtils.closeSession(session);
			JmsUtils.closeConnection(connection);
		}
	}

	public void subcribeQueueToTopic(String topicName, String queueName) {
		if (queueName != null) {
			try {
				createQueue(queueName);

				PCFMessageAgent pcfMessageAgent = new PCFMessageAgent(queueManager);
				PCFMessage request = new PCFMessage(
						MQConstants.MQCMD_CREATE_SUBSCRIPTION);
				request.addParameter(MQConstants.MQCACF_SUB_NAME, queueName);
				request.addParameter(MQConstants.MQCA_TOPIC_STRING, topicName);
				request.addParameter(MQConstants.MQCACF_DESTINATION, queueName);
				request.addParameter(MQConstants.MQCACF_DESTINATION_Q_MGR,
						configurationProperties.getQueueManager());
				pcfMessageAgent.send(request);
				pcfMessageAgent.disconnect();
			}
			catch (MQException e) {
				// see
				// http://www.ibm.com/support/knowledgecenter/SSFKSJ_8.0.0/com.ibm.mq.tro.doc/q048200_.htm
				if (e.getReason() != MQ_SUBSCRIPTION_ALREADY_EXISTS
						&& e.getReason() != MQ_OBJECT_ALREADY_EXISTS) {
					throw new RuntimeException(
							String.format("Could not subscribe queue '%s' to topic '%s'",
									queueName, topicName),
							e);
				}
				else {
					logger.warn(
							"Subscription or queue '{}' already exists for topic '{}', using existing object",
							queueName, topicName);
				}
			}
			catch (IOException e) {
				logger.error(String.format("Could not subscribe queue '%s' to topic '%s'",
						topicName, queueName), e);
			}
		}
	}
}
