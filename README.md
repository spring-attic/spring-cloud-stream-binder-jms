Spring Cloud Stream JMS Binder
------------------------------

The Spring Cloud Stream JMS Binder, provides a SCS binder based on JMS 1.1

There is a Pivotal Tracker® project [publicly available](https://www.pivotaltracker.com/projects/1658999).

JMS supports both point-to-point messaging using its [`Queue`](https://docs.oracle.com/javaee/6/api/javax/jms/Queue.html) abstraction, and 
publish-subscribe messaging using [`Topic`](https://docs.oracle.com/javaee/6/api/javax/jms/Topic.html). However, neither of these patterns
maps fully onto the SCS model of [persistent publish-subscribe with consumer groups](http://docs.spring.io/spring-cloud-stream/docs/current/reference/htmlsingle/#_persistent_publish_subscribe_support)

JMS also does not support provisioning of queues and topics, whereas in SCS queues are created if required whenever they declared.

Some brokers which implement JMS nonetheless provide proprietary extensions which do support these required pieces of functionality.
The `Binder` therefore delegates provisioning this to a broker-specific `QueueProvisioner`.

For more details, see the documentation for the individual broker support submodules.
