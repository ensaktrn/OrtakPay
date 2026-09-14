package com.ortakpay.core.dto;

import java.time.Instant;

public record AuthResponse(String token, Instant expiresAt) {}
