package com.ortakpay.core.repository;

import com.ortakpay.core.domain.Balance;
import com.ortakpay.core.domain.Group;
import com.ortakpay.core.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceRepository extends JpaRepository<Balance, UUID> {

    Optional<Balance> findByGroupAndUser(Group group, User user);

    List<Balance> findByGroup_Id(UUID groupId);
}
