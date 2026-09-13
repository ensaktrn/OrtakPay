package com.ortakpay.core.service;

import com.ortakpay.core.domain.Group;
import com.ortakpay.core.domain.GroupMember;
import com.ortakpay.core.domain.User;
import com.ortakpay.core.dto.GroupResponse;
import com.ortakpay.core.exception.DuplicateGroupMemberException;
import com.ortakpay.core.exception.UserNotFoundException;
import com.ortakpay.core.repository.GroupMemberRepository;
import com.ortakpay.core.repository.GroupRepository;
import com.ortakpay.core.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final GroupAccessGuard groupAccessGuard;

    @Transactional
    public GroupResponse createGroup(String name, UUID creatorId) {
        User creator = userRepository.getReferenceById(creatorId);
        Group group = groupRepository.save(
                Group.builder().name(name).createdBy(creator).build());
        groupMemberRepository.save(
                GroupMember.builder().group(group).user(creator).build());
        return toGroupResponse(group);
    }

    @Transactional
    public void addMember(UUID groupId, UUID requesterId, String email) {
        Group group = groupAccessGuard.requireMembership(groupId, requesterId);

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("No user with email: " + email));

        if (groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, user.getId())) {
            throw new DuplicateGroupMemberException("User " + email + " is already a member of this group");
        }

        groupMemberRepository.save(GroupMember.builder().group(group).user(user).build());
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroupDetails(UUID groupId, UUID requesterId) {
        Group group = groupAccessGuard.requireMembership(groupId, requesterId);
        return toGroupResponse(group);
    }

    private GroupResponse toGroupResponse(Group group) {
        List<GroupResponse.MemberResponse> members = groupMemberRepository.findByGroup_Id(group.getId()).stream()
                .map(member -> new GroupResponse.MemberResponse(
                        member.getUser().getId(), member.getUser().getEmail(), member.getUser().getDisplayName()))
                .toList();
        return new GroupResponse(group.getId(), group.getName(), group.getCreatedBy().getId(), members);
    }
}
