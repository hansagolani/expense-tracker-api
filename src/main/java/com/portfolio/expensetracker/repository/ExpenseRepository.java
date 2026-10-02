package com.portfolio.expensetracker.repository;

import com.portfolio.expensetracker.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByOwnerId(Long ownerId);
    boolean existsByCategoryId(Long categoryId);
}
