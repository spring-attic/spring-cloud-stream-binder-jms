package org.springframework.cloud.stream.binder.jms.utils;

import org.apache.commons.lang.math.NumberUtils;

import javax.jms.Topic;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author José Carlos Valero
 * @since 20/08/16
 */
public class TopicPartitionRegistrar {
    private static final Integer DEFAULT_TOPIC = -1;
    private final ConcurrentHashMap<Integer, Topic> destinations;

    public TopicPartitionRegistrar() {
        destinations = new ConcurrentHashMap<>(10);
    }

    public void addDestination(Integer partition, Topic topic){
        if(partition != null){
            this.destinations.put(partition, topic);
        }else{
            this.destinations.put(DEFAULT_TOPIC, topic);
        }
    }

    public Topic getDestination(Object partition){
        if (partition == null){
            return getNonPartitionedDestination();
        }
        if (partition instanceof Integer) {
            return this.destinations.get(partition);
        }
        if (partition instanceof String) {
            return this.destinations.get(Integer.parseInt((String) partition));
        }
        if (NumberUtils.isDigits(partition.toString())) {
            return this.destinations.get(Integer.parseInt(partition.toString()));
        }
        throw new IllegalArgumentException(String.format("The provided partition '%s' is not a valid format", partition));
    }

    private Topic getNonPartitionedDestination(){
        return this.destinations.get(DEFAULT_TOPIC);
    }

}
