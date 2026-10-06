package com.portfolio.expensetracker.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "expense.exchange";
    public static final String QUEUE = "expense.created";
    public static final String ROUTING_KEY = "expense.created";

    @Bean
    public DirectExchange expenseExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Queue expenseCreatedQueue() {
        return new Queue(QUEUE);
    }

    @Bean
    public Binding expenseCreatedBinding(Queue expenseCreatedQueue, DirectExchange expenseExchange) {
        return BindingBuilder
                .bind(expenseCreatedQueue)
                .to(expenseExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
