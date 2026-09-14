package com.ortakpay.core.controller;

import com.ortakpay.core.domain.User;
import com.ortakpay.core.dto.AddMemberRequest;
import com.ortakpay.core.dto.CreateGroupRequest;
import com.ortakpay.core.dto.GroupResponse;
import com.ortakpay.core.service.GroupService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse createGroup(
            @AuthenticationPrincipal User currentUser, @Valid @RequestBody CreateGroupRequest request) {
        return groupService.createGroup(request.name(), currentUser.getId());
    }

    @PostMapping("/{groupId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public void addMember(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody AddMemberRequest request) {
        groupService.addMember(groupId, currentUser.getId(), request.email());
    }

    @GetMapping("/{groupId}")
    public GroupResponse getGroup(@PathVariable UUID groupId, @AuthenticationPrincipal User currentUser) {
        return groupService.getGroupDetails(groupId, currentUser.getId());
    }
}
