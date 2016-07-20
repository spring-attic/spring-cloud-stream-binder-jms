Spring Cloud Stream JMS Binder – Solace Support
-----------------------------------------------

This module provides provisioning functionality as required by [spring-cloud-stream-binder-jms](../).

To be able to compile the module Solace Java and JMS API jars need to be 
available. Maven is configured to look for them in the lib folder. 

The required sol-jms, sol-common and sol-jcsmp JARs are bundled together in the 
download available at http://dev.solacesystems.com/downloads/download_jms-api/

The current version was built against solace API v7.1.2

### Running tests

In order to run tests a Solace instance is required. There are virtual images available for download 
as well as images for various cloud providers at http://dev.solacesystems.com/downloads/. Once this has been
set up you may need to update `src/main/test/resources/application.yml` with the correct hostname and credentials.
We found the VirtualBox image fairly easy to set up, it seems to work best with 'Host Only Networking' which is 
fine for local development/testing.