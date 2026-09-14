package com.ortakpay.core.dto;

import java.util.UUID;

/**
 * Not called for explicitly by the auth spec, but both POST /api/auth/register
 * (201 body) and GET /api/users/me need to hand back "who is this user" - reusing
 * one small record for both avoids two near-identical DTOs.
 */
public record UserResponse(UUID id, String email, String displayName) {}
