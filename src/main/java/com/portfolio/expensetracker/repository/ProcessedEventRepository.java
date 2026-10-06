package com.portfolio.expensetracker.repository;

import com.portfolio.expensetracker.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

}
