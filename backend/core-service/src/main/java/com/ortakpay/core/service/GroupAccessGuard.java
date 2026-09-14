package com.ortakpay.core.service;

import com.ortakpay.core.domain.Group;
import com.ortakpay.core.exception.GroupAccessDeniedException;
import com.ortakpay.core.exception.GroupNotFoundException;
import com.ortakpay.core.repository.GroupMemberRepository;
import com.ortakpay.core.repository.GroupRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Explicit, call-it-yourself resource-based authorization (see
 * docs/adr/0009-explicit-service-layer-authorization.md for why this isn't
 * {@code @PreAuthorize} + a PermissionEvaluator instead). Every service method
 * that touches a specific group's data is expected to call
 * {@link #requireMembership(UUID, UUID)} as its first step.
 */
@Component
@RequiredArgsConstructor
public class GroupAccessGuard {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    public Group requireMembership(UUID groupId, UUID userId) {
        Group group = groupRepository
                .findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group not found: " + groupId));

        if (!groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, userId)) {
            throw new GroupAccessDeniedException("User is not a member of this group");
        }

        return group;
    }
}
