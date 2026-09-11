package com.ortakpay.core.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ortakpay.core.domain.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

class UserRepositoryTest extends AbstractRepositoryIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndReadsUserById() {
        User user = User.builder()
                .email("alice@example.com")
                .passwordHash("hashed-pw")
                .displayName("Alice")
                .build();

        User saved = userRepository.saveAndFlush(user);
        entityManager.clear();

        Optional<User> found = userRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("alice@example.com");
        assertThat(found.get().getPasswordHash()).isEqualTo("hashed-pw");
        assertThat(found.get().getDisplayName()).isEqualTo("Alice");
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getUpdatedAt()).isNotNull();
    }
}
