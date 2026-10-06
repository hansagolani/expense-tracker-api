package com.portfolio.expensetracker.repository;

import com.portfolio.expensetracker.entity.UserTotal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface UserTotalRepository extends JpaRepository<UserTotal, Long> {

    @Modifying
    @Query("UPDATE UserTotal u SET u.total = u.total + :amount WHERE u.userId = :userId")
    int addToTotal(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
