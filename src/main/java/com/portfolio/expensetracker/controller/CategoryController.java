package com.portfolio.expensetracker.controller;

import com.portfolio.expensetracker.dto.CategoryCreateRequest;
import com.portfolio.expensetracker.dto.CategoryDTO;
import com.portfolio.expensetracker.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryDTO> getAllCategories() {
        return categoryService.getAllCategories()
                .stream()
                .map(CategoryDTO::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public CategoryDTO getCategoryById(@PathVariable Long id) {
        return CategoryDTO.fromEntity(
                categoryService.getCategoryById(id)
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDTO createCategory(
            @Valid @RequestBody CategoryCreateRequest request) {

        return CategoryDTO.fromEntity(
                categoryService.createCategory(request)
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
    }
}
