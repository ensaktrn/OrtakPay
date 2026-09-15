package com.ortakpay.notification.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SettlementReminderMessage(
        UUID groupId, String groupName, UUID userId, String email, String displayName, BigDecimal owedAmount) {}
