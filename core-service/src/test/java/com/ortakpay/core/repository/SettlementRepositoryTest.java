package com.ortakpay.core.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ortakpay.core.domain.Group;
import com.ortakpay.core.domain.Settlement;
import com.ortakpay.core.domain.User;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

class SettlementRepositoryTest extends AbstractRepositoryIT {

    @Autowired
    private SettlementRepository settlementRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndReadsSettlementById() {
        User fromUser = entityManager.persistAndFlush(User.builder()
                .email("from@example.com")
                .passwordHash("hashed-pw")
                .displayName("From")
                .build());
        User toUser = entityManager.persistAndFlush(User.builder()
                .email("to@example.com")
                .passwordHash("hashed-pw")
                .displayName("To")
                .build());
        Group group = entityManager.persistAndFlush(
                Group.builder().name("Trip").createdBy(fromUser).build());

        Settlement settlement = Settlement.builder()
                .group(group)
                .fromUser(fromUser)
                .toUser(toUser)
                .amount(new BigDecimal("75.00"))
                .build();

        Settlement saved = settlementRepository.saveAndFlush(settlement);
        entityManager.clear();

        Optional<Settlement> found = settlementRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualByComparingTo("75.00");
        assertThat(found.get().getFromUser().getId()).isEqualTo(fromUser.getId());
        assertThat(found.get().getToUser().getId()).isEqualTo(toUser.getId());
        assertThat(found.get().getGroup().getId()).isEqualTo(group.getId());
        assertThat(found.get().getSettledAt()).isNotNull();
    }
}
