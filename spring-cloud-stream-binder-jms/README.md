Spring Cloud Stream JMS Binder SPI
----------------------------------

The root `spring-cloud-stream-binder-jms` provides a minimum SPI required 
to create new JMS Spring Cloud Stream providers in a simple way. The required
SPI purpose is to provide those features that are not covered by the JMS 
specification. In particular, provisioning in the way SCS expects it to work.

### Provided implementations

Together with the root SPI the Spring Cloud Stream JMS module provides an implementation
for:
 
1. [Solace](http://www.solacesystems.com/products/jms-messaging) based on the Java proprietary Solace API.

### Implementing new JMS providers

Before starting with a new JMS implementation, it is important to clarify that a Java compatible API
should be provided by the vendor, enabling the provisioning of infrastructure at runtime. Additionally,
SCS expects mixed topologies of topic-queues. i.e. Ideally, your vendor allows queues being subscribed
to topics, so you can get groups of competing consumers (Queues) on top of topics.
There might be other ways of achieving the same feature, e.g. through [virtual topics as in ActiveMQ](http://activemq.apache.org/virtual-destinations.html).

In order to create a new JMS providers, there are a some fundamental steps to be taken into account.

1. Implementing the SPI located at `org.springframework.cloud.stream.binder.jms.spi.QueueProvisioner`
1. Creating the configuration for your binder.
Every JMS implementation is expected to provide at least a bean of type `QueueProvider` in addition
to a reference to the root JMS configuration `JMSAutoConfiguration` an example of a working configuration
class can be found in the Solace implementation in `org.springframework.cloud.stream.binder.jms.solace.config.SolaceJmsConfiguration`.
1. A `spring.binders` file under `META-INF` defining the configuration entry point for your binder SPI, probably
the class created in the previous point.