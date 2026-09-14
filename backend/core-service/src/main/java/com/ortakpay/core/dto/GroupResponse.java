package com.ortakpay.core.dto;

import java.util.List;
import java.util.UUID;

public record GroupResponse(UUID id, String name, UUID createdBy, List<MemberResponse> members) {

    public record MemberResponse(UUID userId, String email, String displayName) {}
}
