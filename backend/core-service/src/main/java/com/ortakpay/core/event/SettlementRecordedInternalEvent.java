package com.ortakpay.core.event;

import java.math.BigDecimal;
import java.util.UUID;

public record SettlementRecordedInternalEvent(
        UUID groupId,
        String groupName,
        UUID fromUserId,
        String fromDisplayName,
        UUID toUserId,
        String toEmail,
        String toDisplayName,
        BigDecimal amount) {}
