package com.portfolio.expensetracker.dto;

import com.portfolio.expensetracker.entity.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;

    public record ExpenseDTO(
            Long id,
            String description,
            BigDecimal amount,
            LocalDate date,
            Long categoryId,
            String categoryName,
            Long ownerId
    ) {
        public static ExpenseDTO fromEntity(Expense expense) {
            return new ExpenseDTO(
                    expense.getId(),
                    expense.getDescription(),
                    expense.getAmount(),
                    expense.getDate(),
                    expense.getCategory().getId(),
                    expense.getCategory().getName(),
                    expense.getOwner().getId()
            );
        }
    }
