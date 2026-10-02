package com.portfolio.expensetracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseCreateRequest(
        @NotBlank String description,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal amount,

        @NotNull
        LocalDate date
) {
}
