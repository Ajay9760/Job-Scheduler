package com.example.chronos;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

@TestConfiguration
@Profile("test")
public class TestBeans {

    @Bean
    public RabbitTemplate rabbitTemplate() {
        // Mock so tests don't need real RabbitMQ
        return mock(RabbitTemplate.class);
    }
}

