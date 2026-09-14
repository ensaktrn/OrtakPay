package com.ortakpay.core.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SettlementResponse(UUID id, UUID fromUserId, UUID toUserId, BigDecimal amount) {}
