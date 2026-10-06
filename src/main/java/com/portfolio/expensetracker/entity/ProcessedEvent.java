package com.portfolio.expensetracker.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    private Long expenseId;

    public ProcessedEvent() {
    }

    public ProcessedEvent(Long expenseId) {
        this.expenseId = expenseId;
    }

    public Long getExpenseId() {
        return expenseId;
    }
}
