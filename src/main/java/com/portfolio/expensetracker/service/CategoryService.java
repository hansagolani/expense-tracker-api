package com.portfolio.expensetracker.service;

import com.portfolio.expensetracker.dto.CategoryCreateRequest;
import com.portfolio.expensetracker.entity.Category;
import com.portfolio.expensetracker.entity.User;
import com.portfolio.expensetracker.exception.DuplicateResourceException;
import com.portfolio.expensetracker.exception.InvalidOperationException;
import com.portfolio.expensetracker.exception.ResourceNotFoundException;
import com.portfolio.expensetracker.repository.CategoryRepository;
import com.portfolio.expensetracker.repository.ExpenseRepository;
import com.portfolio.expensetracker.repository.UserRepository;
import com.portfolio.expensetracker.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;

    @Autowired
    public CategoryService(CategoryRepository categoryRepository, UserRepository userRepository, ExpenseRepository expenseRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
    }

    /**
     * Returns categories belonging to the current user, or
     * if the current user is an ADMIN, ALL categories across all users.
     */
    public List<Category> getAllCategories() {
        if (SecurityUtils.isCurrentUserAdmin()) {
            return categoryRepository.findAll();
        }
        User currentUser = getCurrentUser();
        return categoryRepository.findByOwnerId(currentUser.getId());
    }

    /**
     * Fetches one category by id - if it belongs to the current user, or the current user is an ADMIN.
     */
    public Category getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found"));

        if (SecurityUtils.isCurrentUserAdmin()) {
            return category;
        }

        User currentUser = getCurrentUser();

        if (!category.getOwner().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Category not found");
        }

        return category;
    }


    /**
     * Creates a category owned by the current user.
     */
    public Category createCategory(CategoryCreateRequest request) {
        User currentUser = getCurrentUser();

        if (categoryRepository.existsByOwnerIdAndName(
                currentUser.getId(), request.name())) {

            throw new DuplicateResourceException(
                    "Category with this name already exists");
        }

        Category category = new Category(
                request.name(),
                currentUser
        );

        return categoryRepository.save(category);
    }

    /**
     * Deletes a category - if it belongs to the current user, or the current user is an ADMIN.
     * Categories with existing expenses cannot be deleted.
     */
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found"));

        if (!SecurityUtils.isCurrentUserAdmin()) {
            User currentUser = getCurrentUser();

            if (!category.getOwner().getId().equals(currentUser.getId())) {
                throw new ResourceNotFoundException("Category not found");
            }
        }

        if (expenseRepository.existsByCategoryId(id)) {
            throw new InvalidOperationException(
                    "Category has active expenses");
        }

        categoryRepository.delete(category);
    }

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();

        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Current user not found"));
    }
}
