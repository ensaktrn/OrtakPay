package com.ortakpay.core.repository;

import com.ortakpay.core.domain.Settlement;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {}
