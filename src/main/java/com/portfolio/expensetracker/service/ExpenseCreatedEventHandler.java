package com.portfolio.expensetracker.service;

import com.portfolio.expensetracker.entity.ProcessedEvent;
import com.portfolio.expensetracker.entity.UserTotal;
import com.portfolio.expensetracker.event.ExpenseCreatedEvent;
import com.portfolio.expensetracker.repository.ProcessedEventRepository;
import com.portfolio.expensetracker.repository.UserTotalRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseCreatedEventHandler {

    private final ProcessedEventRepository processedEventRepository;
    private final UserTotalRepository userTotalRepository;

    public ExpenseCreatedEventHandler(ProcessedEventRepository processedEventRepository,
            UserTotalRepository userTotalRepository) {
        this.processedEventRepository = processedEventRepository;
        this.userTotalRepository = userTotalRepository;
    }

    @Transactional
    public void handle(ExpenseCreatedEvent event) {
        if (processedEventRepository.existsById(event.expenseId())) {
            return;
        }

        processedEventRepository.saveAndFlush(new ProcessedEvent(event.expenseId()));

        int rowsUpdated = userTotalRepository.addToTotal(
                event.ownerId(),
                event.amount()
        );

        if (rowsUpdated == 0) {
            userTotalRepository.save(
                    new UserTotal(event.ownerId(), event.amount())
            );
        }
    }

}
