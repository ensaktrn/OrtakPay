package com.ortakpay.core.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ortakpay.core.domain.Balance;
import com.ortakpay.core.domain.Group;
import com.ortakpay.core.domain.User;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

class BalanceRepositoryTest extends AbstractRepositoryIT {

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndReadsBalanceById() {
        User user = persistUser("balance-user@example.com");
        Group group = persistGroup("Trip", user);

        Balance balance =
                Balance.builder().group(group).user(user).build();

        Balance saved = balanceRepository.saveAndFlush(balance);
        entityManager.clear();

        Optional<Balance> found = balanceRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getNetAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(found.get().getVersion()).isEqualTo(0L);
        assertThat(found.get().getGroup().getId()).isEqualTo(group.getId());
        assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void rejectsDuplicateGroupAndUserCombination() {
        User user = persistUser("balance-user-2@example.com");
        Group group = persistGroup("Trip 2", user);

        balanceRepository.saveAndFlush(
                Balance.builder().group(group).user(user).build());

        Balance duplicate = Balance.builder().group(group).user(user).build();

        assertThatThrownBy(() -> balanceRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User persistUser(String email) {
        return entityManager.persistAndFlush(User.builder()
                .email(email)
                .passwordHash("hashed-pw")
                .displayName("User")
                .build());
    }

    private Group persistGroup(String name, User owner) {
        return entityManager.persistAndFlush(
                Group.builder().name(name).createdBy(owner).build());
    }
}
