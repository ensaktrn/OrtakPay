package com.ortakpay.core.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Deliberately has no fromUserId field: the paying side of a settlement is
 * always the authenticated caller (see SettlementService.recordSettlement),
 * never something a client can specify in the request body.
 */
public record SettlementRequest(@NotNull UUID toUserId, @NotNull @Positive BigDecimal amount) {}
