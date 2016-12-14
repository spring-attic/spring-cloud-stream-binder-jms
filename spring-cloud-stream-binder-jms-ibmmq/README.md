# Spring Cloud Stream JMS Binder – IBM&reg; MQ&reg; Support

This module provides provisioning functionality for IBM&reg; MQ&reg; 8.x+ as required by [spring-cloud-stream-binder-jms](../../../).

*IBM&reg; and MQ&reg; are registered trademarks of International Business Machines Corporation.*

## How it works

Using [PCF](https://www.ibm.com/support/knowledgecenter/SSFKSJ_7.0.1/com.ibm.mq.amqzag.doc/fa11570_.htm)
commands, a [Topic](http://www.ibm.com/support/knowledgecenter/SSFKSJ_8.0.0/com.ibm.mq.pro.doc/q004990_.htm)
is created for publishing messages. Receiving messages via consumer groups is implemented as [Subscriptions](http://www.ibm.com/support/knowledgecenter/SSFKSJ_7.5.0/com.ibm.mq.pla.doc/q004950_.htm)
created and subscribed to the Topic, which are then configured to deliver messages to created [Queues](http://www.ibm.com/support/knowledgecenter/SSFKSJ_8.0.0/com.ibm.mq.explorer.doc/e_queues.htm)
(named according to consumer group name) from which the messages are processed.

### Security constraints

Note that object creation is dependant on the security constraints enforced by the [queue manager](https://www.ibm.com/support/knowledgecenter/SSFKSJ_7.1.0/com.ibm.mq.doc/fa10450_.htm).
The current implementation assumes that the user (if `username` is provided) used for authentication
has the correct access rights to create Topic, Subscription and Queue objects.

*Reusing existing objects is currently not tested but is possible and will be
catered for in upcoming updates.*

## Versions tested

Currently only IBM&reg; MQ&reg; version 8.x has been tested.

## Local test environment

One option to test the binder locally is to use the
IBM&reg; MQ&reg; for Developers Docker image available on [Docker Hub](https://hub.docker.com/r/ibmcom/mq/).

Included in this project is a `docker-compose.yml` file to build, configure and run a local
Queue Manager instance. See the [`README.md`](src/etc/docker/v8/README.md) for details.

## Compiling the module

As the MQ Java libraries are proprietry licensed software, you must install the
libraries into your configured Maven repository (local and/or remote).

From your IBM&reg; MQ&reg; installation directory under `java/lib`, install/add the following two libraries:

* `com.ibm.mq.allclient.jar`
* `com.ibm.mq.pcf.jar`

Below is an example of [installing](https://maven.apache.org/guides/mini/guide-3rd-party-jars-local.html)
the two libraries into your local Maven repository:

```console
$ mvn install:install-file
  -Dfile=<path-to-file>/com.ibm.mq.allclient.jar \
  -DgroupId=com.ibm \
  -DartifactId=com.ibm.mq.allclient \
  -Dversion=<version> \
  -Dpackaging=jar

$ mvn install:install-file
  -Dfile=<path-to-file>/com.ibm.mq.pcf.jar \
  -DgroupId=com.ibm \
  -DartifactId=com.ibm.mq.pcf \
  -Dversion=<version> \
  -Dpackaging=jar  
```

Now compile the IBM&reg; MQ&reg; binder with:

```console
$ mvn -P ibmmq clean compile
```

## Running integration tests

Make sure you have an IBM&reg; MQ&reg; Queue Manager instance running.
If you are running the MQ Docker container via the [included `docker-compose.yml`](src/etc/docker/v8/README.md),
you can run all the integration tests as is, with:

```console
$ mvn -P ibmmq clean test -Dtest=EndToEndIntegrationTests -DfailIfNoTests=false
```

otherwise adjust the connection details according to your queue manager.

## Known issues

If no consumer group (`spring.cloud.stream.bindings.input.group` / `spring.cloud.stream.bindings.output.producer.required-groups`)
is provided, an anonymous group is generated. The corresponding
topic, subscription and queue provisioned will not be removed at any point.
If you run many apps without a specific consumer group set, you will
incur many orphaned anonymous objects.

**These objects must be cleaned up manually.**
