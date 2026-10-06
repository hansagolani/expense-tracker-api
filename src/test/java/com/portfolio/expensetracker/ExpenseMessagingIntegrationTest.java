package com.portfolio.expensetracker;

import com.portfolio.expensetracker.entity.ProcessedEvent;
import com.portfolio.expensetracker.entity.UserTotal;
import com.portfolio.expensetracker.event.ExpenseCreatedEvent;
import com.portfolio.expensetracker.repository.ProcessedEventRepository;
import com.portfolio.expensetracker.repository.UserTotalRepository;
import com.portfolio.expensetracker.service.ExpenseCreatedEventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ExpenseMessagingIntegrationTest {

    @Autowired
    private ExpenseCreatedEventHandler handler;

    @Autowired
    private UserTotalRepository userTotalRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void cleanDatabase() {
        processedEventRepository.deleteAll();
        userTotalRepository.deleteAll();
    }

    @Test
    void firstExpenseCreatesUserTotal() {

        ExpenseCreatedEvent event = new ExpenseCreatedEvent(
                100L,
                "Groceries",
                new BigDecimal("25.50"),
                1L,
                Instant.now()
        );

        handler.handle(event);

        UserTotal total = userTotalRepository.findAll()
                .stream()
                .filter(t -> t.getUserId().equals(1L))
                .findFirst()
                .orElseThrow();

        assertThat(total.getTotal())
                .isEqualByComparingTo("25.50");

        assertThat(processedEventRepository.existsById(100L))
                .isTrue();
    }

    @Test
    void secondExpenseUpdatesExistingUserTotal() {

        ExpenseCreatedEvent first = new ExpenseCreatedEvent(
                101L,
                "Groceries",
                new BigDecimal("25.50"),
                1L,
                Instant.now()
        );

        ExpenseCreatedEvent second = new ExpenseCreatedEvent(
                102L,
                "Gas",
                new BigDecimal("40.00"),
                1L,
                Instant.now()
        );

        handler.handle(first);
        handler.handle(second);

        UserTotal total = userTotalRepository.findAll()
                .stream()
                .filter(t -> t.getUserId().equals(1L))
                .findFirst()
                .orElseThrow();

        assertThat(total.getTotal())
                .isEqualByComparingTo("65.50");
    }

    @Test
    void duplicateEventDoesNotIncreaseTotal() {

        ExpenseCreatedEvent event = new ExpenseCreatedEvent(
                103L,
                "Restaurant",
                new BigDecimal("30.00"),
                1L,
                Instant.now()
        );

        handler.handle(event);
        handler.handle(event);

        UserTotal total = userTotalRepository.findAll()
                .stream()
                .filter(t -> t.getUserId().equals(1L))
                .findFirst()
                .orElseThrow();

        assertThat(total.getTotal())
                .isEqualByComparingTo("30.00");

        assertThat(processedEventRepository.count())
                .isEqualTo(1);
    }

    @Test
    void differentUsersHaveSeparateTotals() {

        ExpenseCreatedEvent userOneExpense = new ExpenseCreatedEvent(
                104L,
                "Groceries",
                new BigDecimal("20.00"),
                1L,
                Instant.now()
        );

        ExpenseCreatedEvent userTwoExpense = new ExpenseCreatedEvent(
                105L,
                "Gas",
                new BigDecimal("35.00"),
                2L,
                Instant.now()
        );

        handler.handle(userOneExpense);
        handler.handle(userTwoExpense);

        UserTotal userOneTotal = userTotalRepository.findAll()
                .stream()
                .filter(t -> t.getUserId().equals(1L))
                .findFirst()
                .orElseThrow();

        UserTotal userTwoTotal = userTotalRepository.findAll()
                .stream()
                .filter(t -> t.getUserId().equals(2L))
                .findFirst()
                .orElseThrow();

        assertThat(userOneTotal.getTotal())
                .isEqualByComparingTo("20.00");

        assertThat(userTwoTotal.getTotal())
                .isEqualByComparingTo("35.00");
    }
}
