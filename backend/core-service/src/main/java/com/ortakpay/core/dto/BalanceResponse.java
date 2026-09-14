package com.ortakpay.core.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceResponse(UUID userId, String email, String displayName, BigDecimal netAmount) {}
