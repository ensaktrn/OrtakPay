package com.ortakpay.core.repository;

import com.ortakpay.core.domain.Expense;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    Page<Expense> findByGroup_Id(UUID groupId, Pageable pageable);
}
