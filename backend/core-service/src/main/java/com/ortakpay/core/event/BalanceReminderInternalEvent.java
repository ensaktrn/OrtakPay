package com.ortakpay.core.event;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceReminderInternalEvent(
        UUID groupId, String groupName, UUID userId, String email, String displayName, BigDecimal owedAmount) {}
