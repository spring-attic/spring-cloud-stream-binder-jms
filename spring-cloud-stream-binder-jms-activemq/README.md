Spring Cloud Stream JMS Binder – ActiveMQ Support
-------------------------------------------------

This module provides provisioning functionality as required by [spring-cloud-stream-binder-jms](../../../).

### How it works

ActiveMQ allows creating the SCS model by using [virtual destinations](https://activemq.apache.org/virtual-destinations.html).
The effect is virtually the same as subscribing queues to topics.

### Compiling the module

The module provides out of the box everything required to be compiled.

### Running tests

Tests should run out of the box, and an embedded instance of ActiveMQ should
start in order to perform integration tests.