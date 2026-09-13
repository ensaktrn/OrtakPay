package com.ortakpay.core.repository;

import com.ortakpay.core.domain.GroupMember;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

    List<GroupMember> findByGroup_IdAndUser_IdIn(UUID groupId, Collection<UUID> userIds);

    boolean existsByGroup_IdAndUser_Id(UUID groupId, UUID userId);

    List<GroupMember> findByGroup_Id(UUID groupId);
}
