package com.portfolio.expensetracker.service;

import com.portfolio.expensetracker.dto.ExpenseCreateRequest;
import com.portfolio.expensetracker.entity.Category;
import com.portfolio.expensetracker.entity.Expense;
import com.portfolio.expensetracker.entity.User;
import com.portfolio.expensetracker.exception.ResourceNotFoundException;
import com.portfolio.expensetracker.repository.CategoryRepository;
import com.portfolio.expensetracker.repository.ExpenseRepository;
import com.portfolio.expensetracker.repository.UserRepository;
import com.portfolio.expensetracker.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.portfolio.expensetracker.event.ExpenseCreatedEvent;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;



import java.util.List;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;


    @Autowired
    public ExpenseService(ExpenseRepository expenseRepository,
                          CategoryRepository categoryRepository,
                          UserRepository userRepository,
                          ApplicationEventPublisher eventPublisher) {
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Returns expenses belonging to the current user - or,
     * if the current user is an ADMIN, ALL expenses across all users.
     */
    public List<Expense> getAllExpenses() {
        if (SecurityUtils.isCurrentUserAdmin()) {
            return expenseRepository.findAll();
        }

        User currentUser = getCurrentUser();

        return expenseRepository.findByOwnerId(currentUser.getId());
    }

    /**
     * Fetches one expense by id - if it belongs to the current user, or the current user is an ADMIN.
     */
    public Expense getExpenseById(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Expense not found"));

        if (SecurityUtils.isCurrentUserAdmin()) {
            return expense;
        }

        User currentUser = getCurrentUser();

        if (!expense.getOwner().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Expense not found");
        }

        return expense;
    }

    /**
     * Creates an expense owned by the current user.
     * The incoming Expense can reference to a self-owned Category only.
     */
    @Transactional
    public Expense createExpense(ExpenseCreateRequest request, Long categoryId) {
        User currentUser = getCurrentUser();

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found"));

        // Applies to ADMIN too.
        if (!category.getOwner().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Category not found");
        }

        Expense expense = new Expense(
                request.description(),
                request.amount(),
                request.date(),
                category,
                currentUser
        );
        Expense savedExpense = expenseRepository.save(expense);

        //Publish event
        eventPublisher.publishEvent(
                new ExpenseCreatedEvent(
                        savedExpense.getId(),
                        savedExpense.getDescription(),
                        savedExpense.getAmount(),
                        savedExpense.getOwner().getId(),
                        Instant.now()
                )
        );

        return savedExpense;

    }

    /**
     * Deletes an expense - if it belongs to the current user, or the current user is an ADMIN.
     */
    public void deleteExpense(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Expense not found"));

        if (!SecurityUtils.isCurrentUserAdmin()) {
            User currentUser = getCurrentUser();

            if (!expense.getOwner().getId().equals(currentUser.getId())) {
                throw new ResourceNotFoundException("Expense not found");
            }
        }

        expenseRepository.delete(expense);
    }

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();

        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Current user not found"));
    }
}
