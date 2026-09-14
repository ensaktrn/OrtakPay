package com.ortakpay.core.event;

import java.util.UUID;

public record GroupMemberAddedInternalEvent(
        UUID groupId,
        String groupName,
        UUID newMemberId,
        String newMemberEmail,
        String newMemberDisplayName,
        String addedByDisplayName) {}
