package com.portfolio.expensetracker.dto;

public record AuthResponse(
        String token,
        String username
) {
}
