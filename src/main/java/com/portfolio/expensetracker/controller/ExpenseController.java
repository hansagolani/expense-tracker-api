package com.portfolio.expensetracker.controller;

import com.portfolio.expensetracker.dto.ExpenseCreateRequest;
import com.portfolio.expensetracker.dto.ExpenseDTO;
import com.portfolio.expensetracker.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    @Autowired
    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public List<ExpenseDTO> getAllExpenses() {
        return expenseService.getAllExpenses()
                .stream()
                .map(ExpenseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public ExpenseDTO getExpenseById(@PathVariable Long id) {
        return ExpenseDTO.fromEntity(
                expenseService.getExpenseById(id)
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseDTO createExpense(
            @Valid @RequestBody ExpenseCreateRequest request,
            @RequestParam Long categoryId) {

        return ExpenseDTO.fromEntity(
                expenseService.createExpense(request, categoryId)
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
    }
}
