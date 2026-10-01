package com.portfolio.expensetracker.exception;

public class InvalidOperationException extends RuntimeException {
    public InvalidOperationException(String message) {
        super(message);
    }
}
