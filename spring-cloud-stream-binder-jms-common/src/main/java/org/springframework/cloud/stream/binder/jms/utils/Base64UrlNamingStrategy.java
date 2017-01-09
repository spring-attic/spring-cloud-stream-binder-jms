package org.springframework.cloud.stream.binder.jms.utils;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;

/**
 * Taken from spring-cloud/spring-cloud-stream-binder-rabbit. See
 * https://github.com/spring-cloud/spring-cloud-stream-binder-rabbit/blob/master/spring-cloud-stream-binder-rabbit/src/main/java/org/springframework/cloud/stream/binder/rabbit/RabbitMessageChannelBinder.java#L90
 *
 * @author Donovan Muller
 */
public class Base64UrlNamingStrategy implements AnonymousNamingStrategy {

    private String prefix = "spring.gen-";

    public Base64UrlNamingStrategy() {
    }

    public Base64UrlNamingStrategy(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String generateName() {
        return generateName(this.prefix);
    }

    @Override
    public String generateName(String prefix) {
        Assert.hasText(prefix, "Prefix cannot be zero-length");

        UUID uuid = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits());
        // Convert to base64 and remove trailing =
        return prefix + Base64Utils.encodeToUrlSafeString(bb.array()).replaceAll("=", "")
                .replaceAll("-", "$");
    }
}
