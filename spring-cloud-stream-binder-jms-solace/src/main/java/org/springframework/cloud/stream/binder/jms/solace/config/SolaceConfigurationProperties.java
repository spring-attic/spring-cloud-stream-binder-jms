package org.springframework.cloud.stream.binder.jms.solace.config;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.solace")
public class SolaceConfigurationProperties {

    /**
     * Host of solace server e.g. 192.168.99.100
     */
    private String host;

    /**
     * Username, e.g. admin
     */
    private String username;

    /**
     * Password, e.g. admin
     */
    private String password;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
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
}
