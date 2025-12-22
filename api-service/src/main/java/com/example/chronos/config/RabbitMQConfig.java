package com.example.chronos.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "chronos.exchange";
    public static final String QUEUE_NAME = "chronos.job.queue";
    public static final String ROUTING_KEY = "job.execute";

    @Bean
    public TopicExchange jobExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue jobQueue() {
        return new Queue(QUEUE_NAME, true); // durable = true
    }

    @Bean
    public Binding jobBinding(Queue jobQueue, TopicExchange jobExchange) {
        return BindingBuilder.bind(jobQueue)
                .to(jobExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
