package org.springframework.cloud.stream.binder.jms.utils;

public interface AnonymousNamingStrategy {

    String generateName();

    String generateName(String prefix);
}
