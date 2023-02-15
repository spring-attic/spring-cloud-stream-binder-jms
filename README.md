# spring-cloud-stream-binder-jms is no longer actively maintained by VMware, Inc.

Spring Cloud Stream JMS Binder
------------------------------

The Spring Cloud Stream JMS Binder, provides a SCS binder based on JMS 1.1

There is a Pivotal Tracker® project [publicly available](https://www.pivotaltracker.com/projects/1658999).

### Limitations

This binder is currently in alpha, bugs are expected.

JMS supports both point-to-point messaging using its [`Queue`](https://docs.oracle.com/javaee/6/api/javax/jms/Queue.html) abstraction, and
publish-subscribe messaging using [`Topic`](https://docs.oracle.com/javaee/6/api/javax/jms/Topic.html). However, neither of these patterns
maps fully onto the SCS model of [persistent publish-subscribe with consumer groups](https://docs.spring.io/spring-cloud-stream/docs/current/reference/htmlsingle/#_persistent_publish_subscribe_support)

JMS also does not support provisioning of queues and topics, whereas in SCS queues are created if required whenever they declared.

Some brokers which implement JMS nonetheless provide proprietary extensions which do support these required pieces of functionality.
The `Binder` therefore delegates provisioning this to a broker-specific `QueueProvisioner`.

For more details, see the documentation for the individual broker support sub-modules.

- [**Solace**](https://github.com/spring-cloud/spring-cloud-stream-binder-solace)
- [**ActiveMQ**](spring-cloud-stream-binder-jms-activemq)
- [**IBM&reg; MQ&reg;**](https://github.com/spring-cloud/spring-cloud-stream-binder-ibm-mq)

### Provided implementations

Together with the root SPI the Spring Cloud Stream JMS module provides an implementation
for:

1. [Solace](https://www.solacesystems.com/products/jms-messaging) based on the Java proprietary Solace API.
2. [ActiveMQ](https://activemq.apache.org/) based on Virtual Destinations, a JMS compatible feature following certain naming conventions.
3. [IBM&reg; MQ&reg;](https://www-03.ibm.com/software/products/en/ibm-mq) based on the Java proprietary libraries ([PCF](https://www.ibm.com/support/knowledgecenter/SSFKSJ_8.0.0/com.ibm.mq.dev.doc/q030980_.htm)) provided with an IBM&reg; MQ&reg; installation.

### Implementing new JMS providers

Before starting with a new JMS implementation, it is important to clarify that a Java compatible API
should be provided by the vendor, enabling the provisioning of infrastructure at runtime. Additionally,
SCS expects mixed topologies of topic-queues. i.e. Ideally, your vendor allows queues being subscribed
to topics, so you can get groups of competing consumers (Queues) on top of topics.
There might be other ways of achieving the same effect, e.g. through [virtual topics as in ActiveMQ](https://activemq.apache.org/virtual-destinations.html).

In order to create a new JMS provider, there are a some fundamental steps to be taken into account.

1. Implementing the SPI located at `org.springframework.cloud.stream.binder.jms.spi.QueueProvisioner`
1. Creating the configuration for your binder.
Every JMS implementation is expected to provide at least a bean of type `QueueProvider` in addition
to a reference to the root JMS configuration `JMSAutoConfiguration` an example of a working configuration
class can be found in the Solace implementation in `org.springframework.cloud.stream.binder.jms.solace.config.SolaceJmsConfiguration`.
1. A `spring.binders` file under `META-INF` defining the configuration entry point for your binder SPI, probably
the class created in the previous point.

It might be a good starting point checking out the existing implementations
e.g. [Solace](https://github.com/spring-cloud/spring-cloud-stream-binder-solace) or [ActiveMQ](spring-cloud-stream-binder-jms-activemq).

#### Testing your new JMS provider

Apart from your unit tests, or integration tests, the JMS binder provides a template that will check most core features
of the SCS model, verifying that your implementation is SCS compliant, in order to enable these tests you just need
to implement `org.springframework.cloud.stream.binder.test.integration.EndToEndIntegrationTests` from the
`spring-cloud-stream-binder-jms-test-support` maven submodule. e.g.:

```java
public class MyEffortlessEndToEndIntegrationTests extends org.springframework.cloud.stream.binder.test.integration.EndToEndIntegrationTests {
    public EndToEndIntegrationTests() throws Exception {
        super(
                new MyBrandNewQueueProvisioner(),
                myJmsConnectionFactory
        );
    }
}
```

You might encounter an exception preventing the WebApplicationContext from being created, make sure to include
a yaml (or alternatively properties) file in your test resources including, at least:

```yml
spring:
  main:
    web-environment: false

  # Necessary for org.springframework.cloud.stream.binder.jms.integration testing multiple ApplicationContexts.
  jmx:
    enabled: false
```

Depending on your technology it might be easier or more difficult to set up the infrastructure,
if possible embedded, self-contained servers are preferred as in ActiveMQ, but this is not always possible
as in Solace. Due to technical or legal reasons.
