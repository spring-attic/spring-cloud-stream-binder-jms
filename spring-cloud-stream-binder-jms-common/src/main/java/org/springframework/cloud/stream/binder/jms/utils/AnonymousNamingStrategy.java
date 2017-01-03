package org.springframework.cloud.stream.binder.jms.utils;

/**
 * @author Donovan Muller
 */
public interface AnonymousNamingStrategy {

    String generateName();

    String generateName(String prefix);
}
