package com.ortakpay.core.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ortakpay.core.domain.Group;
import com.ortakpay.core.domain.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

class GroupRepositoryTest extends AbstractRepositoryIT {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndReadsGroupById() {
        User owner = entityManager.persistAndFlush(User.builder()
                .email("owner@example.com")
                .passwordHash("hashed-pw")
                .displayName("Owner")
                .build());

        Group group = Group.builder().name("Trip to Kapadokya").createdBy(owner).build();

        Group saved = groupRepository.saveAndFlush(group);
        entityManager.clear();

        Optional<Group> found = groupRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Trip to Kapadokya");
        assertThat(found.get().getCreatedBy().getId()).isEqualTo(owner.getId());
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getUpdatedAt()).isNotNull();
    }
}
