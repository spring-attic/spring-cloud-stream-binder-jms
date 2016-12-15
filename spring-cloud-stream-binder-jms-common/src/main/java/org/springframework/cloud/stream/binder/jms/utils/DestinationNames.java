package org.springframework.cloud.stream.binder.jms.utils;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author José Carlos Valero
 * @since 20/08/16
 */
public class DestinationNames {

    private final String topicName;
    private final String[] groupNames;
    private final Integer partitionIndex;

    public DestinationNames(String topicName, String[] groupNames, int partitionIndex) {
        this.topicName = topicName;
        this.groupNames = groupNames;
        this.partitionIndex = partitionIndex;
    }

    public DestinationNames(String topicName, String[] groupNames) {
        this.topicName = topicName;
        this.groupNames = groupNames;
        partitionIndex = null;
    }

    public String getTopicName() {
        return topicName;
    }

    public String[] getGroupNames() {
        return groupNames;
    }

    public Integer getPartitionIndex() {
        return partitionIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DestinationNames that = (DestinationNames) o;
        return Objects.equals(topicName, that.topicName) &&
                Arrays.equals(groupNames, that.groupNames) &&
                Objects.equals(partitionIndex, that.partitionIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicName, groupNames, partitionIndex);
    }

    @Override
    public String toString() {
        return "DestinationNames{" +
                "topicName='" + topicName + '\'' +
                ", groupNames=" + Arrays.toString(groupNames) +
                ", partitionIndex=" + partitionIndex +
                '}';
    }
}
