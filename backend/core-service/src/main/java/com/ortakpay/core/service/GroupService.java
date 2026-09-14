package com.ortakpay.core.service;

import com.ortakpay.core.domain.Group;
import com.ortakpay.core.domain.GroupMember;
import com.ortakpay.core.domain.User;
import com.ortakpay.core.dto.GroupResponse;
import com.ortakpay.core.dto.GroupSummaryResponse;
import com.ortakpay.core.event.GroupMemberAddedInternalEvent;
import com.ortakpay.core.exception.DuplicateGroupMemberException;
import com.ortakpay.core.exception.UserNotFoundException;
import com.ortakpay.core.repository.GroupMemberRepository;
import com.ortakpay.core.repository.GroupRepository;
import com.ortakpay.core.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final GroupAccessGuard groupAccessGuard;
    private final ApplicationEventPublisher applicationEventPublisher;

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

        User requester = userRepository.getReferenceById(requesterId);
        applicationEventPublisher.publishEvent(new GroupMemberAddedInternalEvent(
                groupId,
                group.getName(),
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                requester.getDisplayName()));
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroupDetails(UUID groupId, UUID requesterId) {
        Group group = groupAccessGuard.requireMembership(groupId, requesterId);
        return toGroupResponse(group);
    }

    // No GroupAccessGuard check needed here, unlike getGroupDetails: this
    // queries by the caller's own userId, so it can never surface a group
    // the caller isn't already a member of.
    @Transactional(readOnly = true)
    public List<GroupSummaryResponse> getMyGroups(UUID userId) {
        return groupMemberRepository.findByUser_Id(userId).stream()
                .map(GroupMember::getGroup)
                .map(group -> new GroupSummaryResponse(
                        group.getId(), group.getName(), (int) groupMemberRepository.countByGroup_Id(group.getId())))
                .toList();
    }

    private GroupResponse toGroupResponse(Group group) {
        List<GroupResponse.MemberResponse> members = groupMemberRepository.findByGroup_Id(group.getId()).stream()
                .map(member -> new GroupResponse.MemberResponse(
                        member.getUser().getId(), member.getUser().getEmail(), member.getUser().getDisplayName()))
                .toList();
        return new GroupResponse(group.getId(), group.getName(), group.getCreatedBy().getId(), members);
    }
}
