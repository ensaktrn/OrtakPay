package com.ortakpay.core.repository;

import com.ortakpay.core.domain.ExpenseShare;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseShareRepository extends JpaRepository<ExpenseShare, UUID> {

    List<ExpenseShare> findByExpense_IdIn(Collection<UUID> expenseIds);
}
