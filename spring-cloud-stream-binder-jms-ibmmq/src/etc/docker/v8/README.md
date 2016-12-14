# Starting a local IBM&reg; MQ&reg; 8 instance

Follow these steps to run a local MQ&reg; instance using Docker.

## Using Docker Machine

Install [Docker Toolbox](https://www.docker.com/products/docker-toolbox)
and verify that you have started a Docker VM using the VirtualBox driver:

```console
$ docker-machine ls
NAME          ACTIVE   DRIVER       STATE     URL                         SWARM   DOCKER    ERRORS
default       *        virtualbox   Running   tcp://192.168.99.100:2376           v1.12.1
```

Next, in the `src/etc/docker/v8` directory, run `docker-compose up`.

You should see output ending in something similar to this:

```console
...
local-mq8 | 5 MQSC commands read.
local-mq8 | No commands have a syntax error.
local-mq8 | All valid MQSC commands were processed.
local-mq8 | ----------------------------------------
```

Next, point your docker client to the active Docker VM with:

```console
$ eval $(docker-machine env default)
```

Use the following connection details with your JMS/MQ management tool of choice
or the following configuration properties in a Spring Cloud Stream app:

| Name          | Value                         | Configuration Property Example
| ------------- | ----------------------------- | ----------------------------------------- |
| Host          | `docker-machine ip default`   | ibmmq.host=`docker-machine ip default`    |
| Port          | 1414                          | ibmmq.port=1414                           |
| Queue Manager | LOCAL.DOCKER.QMGR             | ibmmq.queueManager=LOCAL.DOCKER.QMGR      |
| Channel       | PASSWORD.SVRCONN              | ibmmq.channel=PASSWORD.SVRCONN            |
| Username      | mq                            | ibmmq.username=mq                         |
| Password      | mq                            | ibmmq.password=mq                         |
