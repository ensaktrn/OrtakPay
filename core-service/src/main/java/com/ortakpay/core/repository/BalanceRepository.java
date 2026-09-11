package com.ortakpay.core.repository;

import com.ortakpay.core.domain.Balance;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceRepository extends JpaRepository<Balance, UUID> {}
