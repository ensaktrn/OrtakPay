package com.ortakpay.core.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ortakpay.core.domain.Group;
import com.ortakpay.core.domain.GroupMember;
import com.ortakpay.core.domain.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

class GroupMemberRepositoryTest extends AbstractRepositoryIT {

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndReadsGroupMemberById() {
        User owner = persistUser("owner@example.com");
        Group group = persistGroup("Trip", owner);

        GroupMember member = GroupMember.builder().group(group).user(owner).build();

        GroupMember saved = groupMemberRepository.saveAndFlush(member);
        entityManager.clear();

        Optional<GroupMember> found = groupMemberRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getGroup().getId()).isEqualTo(group.getId());
        assertThat(found.get().getUser().getId()).isEqualTo(owner.getId());
        assertThat(found.get().getJoinedAt()).isNotNull();
    }

    @Test
    void rejectsDuplicateGroupAndUserCombination() {
        User owner = persistUser("owner2@example.com");
        Group group = persistGroup("Trip 2", owner);

        groupMemberRepository.saveAndFlush(
                GroupMember.builder().group(group).user(owner).build());

        GroupMember duplicate =
                GroupMember.builder().group(group).user(owner).build();

        assertThatThrownBy(() -> groupMemberRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User persistUser(String email) {
        return entityManager.persistAndFlush(User.builder()
                .email(email)
                .passwordHash("hashed-pw")
                .displayName("Member")
                .build());
    }

    private Group persistGroup(String name, User owner) {
        return entityManager.persistAndFlush(
                Group.builder().name(name).createdBy(owner).build());
    }
}
