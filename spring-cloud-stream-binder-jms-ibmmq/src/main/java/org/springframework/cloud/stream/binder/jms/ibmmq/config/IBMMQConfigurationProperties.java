package org.springframework.cloud.stream.binder.jms.ibmmq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.ibm.msg.client.wmq.WMQConstants;

/**
 * Configuration properties for IBM MQ specific features.
 *
 * @author Donovan Muller
 */
@ConfigurationProperties(prefix = "ibmmq")
public class IBMMQConfigurationProperties {

	private String host;

	private Integer port = 1414;

	/**
	 * The name of the Queue Manager
	 */
	private String queueManager;

	private String channel;

	/**
	 * Only WMQ_CLIENT_NONJMS_MQ (1) transport type supported
	 */
	private Integer transportType = WMQConstants.WMQ_CLIENT_NONJMS_MQ;

	/**
	 * Optional username for queue manager authentication
	 */
	private String username;

	/**
	 * Optional password for queue manager authentication
	 */
	private String password;

	/**
	 * Queue depth for provisioned queues. Defaults to 5000.
	 */
	private int queueDepth = 5000;

	/**
	 * Queue depth for the provisioned DLQ. Defaults to 5000.
	 */
	private int deadLetterQueueDepth = 5000;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public String getQueueManager() {
		return queueManager;
	}

	public void setQueueManager(String queueManager) {
		this.queueManager = queueManager;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public Integer getTransportType() {
		return transportType;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getDeadLetterQueueDepth() {
		return deadLetterQueueDepth;
	}

	public void setDeadLetterQueueDepth(int deadLetterQueueDepth) {
		this.deadLetterQueueDepth = deadLetterQueueDepth;
	}

	public int getQueueDepth() {
		return queueDepth;
	}

	public void setQueueDepth(int queueDepth) {
		this.queueDepth = queueDepth;
	}
}
