package com.ortakpay.core.repository;

import com.ortakpay.core.domain.ExpenseShare;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseShareRepository extends JpaRepository<ExpenseShare, UUID> {}
