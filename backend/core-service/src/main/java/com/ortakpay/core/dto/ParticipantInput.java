package com.ortakpay.core.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * {@code value} means something different per split type: ignored for EQUAL,
 * the exact owed amount for EXACT, a percentage (0-100) for PERCENTAGE - so it
 * is deliberately left unconstrained here rather than picking a validation rule
 * that would only be correct for one of the three.
 */
public record ParticipantInput(@NotNull UUID userId, BigDecimal value) {}
