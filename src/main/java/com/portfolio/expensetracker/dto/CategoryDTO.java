package com.portfolio.expensetracker.dto;

import com.portfolio.expensetracker.entity.Category;

public record CategoryDTO(
        Long id,
        String name,
        Long ownerId
) {
    public static CategoryDTO fromEntity(Category category) {
        return new CategoryDTO(category.getId(), category.getName(), category.getOwner().getId());
    }
}
