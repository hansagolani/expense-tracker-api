package com.portfolio.expensetracker.event;

import java.math.BigDecimal;
import java.time.Instant;

public record ExpenseCreatedEvent(
        Long expenseId,
        String description,
        BigDecimal amount,
        Long ownerId,
        Instant timestamp
) {
}
