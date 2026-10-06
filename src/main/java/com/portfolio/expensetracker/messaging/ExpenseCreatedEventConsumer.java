package com.portfolio.expensetracker.messaging;

import com.portfolio.expensetracker.config.RabbitMQConfig;
import com.portfolio.expensetracker.event.ExpenseCreatedEvent;
import com.portfolio.expensetracker.service.ExpenseCreatedEventHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ExpenseCreatedEventConsumer {

    private final ExpenseCreatedEventHandler handler;

    public ExpenseCreatedEventConsumer(ExpenseCreatedEventHandler handler) {
        this.handler = handler;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void consume(ExpenseCreatedEvent event) {
        handler.handle(event);
    }

}
