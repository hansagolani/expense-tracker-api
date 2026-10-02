package com.portfolio.expensetracker.repository;

import com.portfolio.expensetracker.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByOwnerId(Long ownerId);

    boolean existsByOwnerIdAndName(Long ownerId, String name);

}
