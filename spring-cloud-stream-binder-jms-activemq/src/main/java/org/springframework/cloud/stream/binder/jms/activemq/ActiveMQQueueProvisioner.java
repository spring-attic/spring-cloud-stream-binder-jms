package org.springframework.cloud.stream.binder.jms.activemq;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.cloud.stream.binder.jms.spi.QueueProvisioner;
import org.springframework.jms.support.JmsUtils;

/**
 * {@link QueueProvisioner} for ActiveMQ.
 *
 * @author José Carlos Valero
 * @since 1.1
 */
public class ActiveMQQueueProvisioner implements QueueProvisioner{

    public static final String ACTIVE_MQ_DLQ = "ActiveMQ.DLQ";
    private final ActiveMQConnectionFactory connectionFactory;

    public ActiveMQQueueProvisioner(ActiveMQConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Destinations provisionTopicAndConsumerGroup(String topicName, String... consumerGroupName) {
        Connection activeMQConnection;
        Session session;
        Topic topic = null;
        Queue[] groups = null;
        try {
            activeMQConnection = connectionFactory.createConnection();
            session = activeMQConnection.createSession(true, Session.CLIENT_ACKNOWLEDGE);
            topic = session.createTopic(String.format("VirtualTopic.%s", topicName));
            if (ArrayUtils.isNotEmpty(consumerGroupName)) {
                groups = new Queue[consumerGroupName.length];
                for (int i = 0; i < consumerGroupName.length; i++) {
                    /*
                     * By default, ActiveMQ consumer queues are named 'Consumer.*.VirtualTopic.',
                     * therefore we must remove '.' from the consumer group name if present.
                     * For example, anonymous consumer groups are named 'anonymous.*' by default.
                     */
                    groups[i] = createQueue(topicName, session,	consumerGroupName[i].replaceAll("\\.", "_"));
                }
            }

            JmsUtils.commitIfNecessary(session);
            JmsUtils.closeSession(session);
            JmsUtils.closeConnection(activeMQConnection);
        } catch (JMSException e) {
            e.printStackTrace();
        }

        return new Destinations(topic, groups);
    }

    private Queue createQueue(String topicName, Session session, String consumerName) throws JMSException {
        Queue queue = session.createQueue(String.format("Consumer.%s.VirtualTopic.%s", consumerName, topicName));
        //TODO: Understand why a producer is required to actually create the queue, it's not mentioned in ActiveMQ docs
        session.createProducer(queue).close();
        return queue;
    }

    @Override
    public String provisionDeadLetterQueue() {
        Session session = null;
        Connection connection = null;
        try {
            connection = connectionFactory.createConnection();
            session = connection.createSession(true, 1);
            session.createQueue(ACTIVE_MQ_DLQ);
        } catch (JMSException e) {
            e.printStackTrace();
        }finally {
            try {
                JmsUtils.commitIfNecessary(session);
                JmsUtils.closeSession(session);
                JmsUtils.closeConnection(connection);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
        return ACTIVE_MQ_DLQ;
    }

}
