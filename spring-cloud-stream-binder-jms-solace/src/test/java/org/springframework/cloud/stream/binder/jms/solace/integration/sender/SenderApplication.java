/*
 *  Copyright 2002-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.stream.binder.jms.solace.integration.sender;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@SpringBootApplication(exclude = {EmbeddedServletContainerAutoConfiguration.class, WebMvcAutoConfiguration.class})
@EnableBinding(Source.class)
public class SenderApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(SenderApplication.class, args);
        Sender generator = context.getBean(Sender.class);
        generator.send("Joseph");
        generator.send("Jack");
    }

    @Component
    public static class Sender {

        private Source source;

        @Autowired
        public Sender(Source source) {
            this.source = source;
        }

        public void send(Object something) {
            send(something, new HashMap<>());
        }

        public void send(Object something, Map<String, Object> headers) {
            MessageBuilder<Object> builder = MessageBuilder.withPayload(something);
            headers.entrySet().forEach(s -> builder.setHeader(s.getKey(), s.getValue()));
            Message message = builder.build();
            source.output().send(message);
        }
    }
}
