package com.ortakpay.core.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ortakpay.core.domain.Balance;
import com.ortakpay.core.domain.Group;
import com.ortakpay.core.domain.User;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Simulates two independent, concurrent transactions racing on the same Balance
 * row using {@code PROPAGATION_REQUIRES_NEW} via {@code TransactionTemplate}
 * (rather than real threads) - deterministic, and exactly the scenario the task
 * describes: read the same row (same version) in two separate transactions,
 * commit one change, then attempt the second with a now-stale version.
 *
 * <p>All setup and reads also go through the same REQUIRES_NEW template, not the
 * ambient {@code @DataJpaTest} test transaction - REQUIRES_NEW always suspends
 * whatever transaction is ambient and opens a genuinely separate one, so if setup
 * were left uncommitted in the ambient (rolled-back-at-end) test transaction, the
 * separate transactions here would never see it under READ_COMMITTED isolation.
 */
class BalanceOptimisticLockingIntegrationTest extends AbstractRepositoryIT {

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate requiresNew;

    @BeforeEach
    void setUp() {
        requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    @Test
    void secondConcurrentSave_onSameBalanceRow_failsWithOptimisticLock() {
        UUID balanceId = requiresNew.execute(status -> {
            User user = userRepository.save(User.builder()
                    .email("racer-" + UUID.randomUUID() + "@example.com")
                    .passwordHash("hash")
                    .displayName("Racer")
                    .build());
            Group group =
                    groupRepository.save(Group.builder().name("Race Group").createdBy(user).build());
            Balance balance = balanceRepository.save(
                    Balance.builder().group(group).user(user).build());
            return balance.getId();
        });

        Balance readByFirstTransaction =
                requiresNew.execute(status -> balanceRepository.findById(balanceId).orElseThrow());
        Balance readBySecondTransaction =
                requiresNew.execute(status -> balanceRepository.findById(balanceId).orElseThrow());

        requiresNew.execute(status -> {
            readByFirstTransaction.applyDelta(new BigDecimal("10.00"));
            return balanceRepository.save(readByFirstTransaction);
        });

        assertThatThrownBy(() -> requiresNew.execute(status -> {
                    readBySecondTransaction.applyDelta(new BigDecimal("5.00"));
                    return balanceRepository.save(readBySecondTransaction);
                }))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        Balance finalState = balanceRepository.findById(balanceId).orElseThrow();
        assertThat(finalState.getNetAmount()).isEqualByComparingTo("10.00");
    }
}
