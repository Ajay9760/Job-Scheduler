package com.example.chronos.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topology shared by scheduler (producer) and worker (consumer). Declared by both so start order
 * does not matter. Failed/poison messages go to the dead-letter queue, never loop forever.
 * Active only when spring-amqp is on the classpath (api-service excludes it).
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
public class RabbitMessagingConfig {

    public static final String EXCHANGE = "chronos.jobs";
    public static final String QUEUE = "chronos.jobs.execute";
    public static final String ROUTING_KEY = "job.execute";
    public static final String DLX = "chronos.jobs.dlx";
    public static final String DLQ = "chronos.jobs.dead";

    @Bean
    DirectExchange jobsExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    Queue jobsQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ)
                .withArgument("x-max-priority", 10)
                .build();
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    Binding jobsBinding() {
        return BindingBuilder.bind(jobsQueue()).to(jobsExchange()).with(ROUTING_KEY);
    }

    @Bean
    Binding deadBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DLQ);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
