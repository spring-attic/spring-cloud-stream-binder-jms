Spring Cloud Stream JMS Binder SPI
----------------------------------

The root `spring-cloud-stream-binder-jms` provides a minimum SPI required 
to create new JMS Spring Cloud Stream providers in a simple way. The required
SPI purpose is to provide those features that are not covered by the JMS 
specification. In particular, provisioning in the way SCS expects it to work.
